/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent

import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEventDispatcher.Companion.PRIORITY_DEFAULT
import androidx.navigationevent.NavigationEventDispatcher.Companion.PRIORITY_OVERLAY
import androidx.navigationevent.NavigationEventDispatcher.Priority
import androidx.navigationevent.NavigationEventInfo.NotProvided
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the lifecycle and dispatching of [NavigationEventHandler] instances across all
 * NavigationEventDispatcher instances. This class ensures consistent ordering, state management,
 * and prioritized dispatch for navigation events.
 */
internal class NavigationEventProcessor {

    /** The private, mutable source of truth for the navigation state. */
    internal val _state = MutableStateFlow<NavigationEventState<*>>(Idle(currentInfo = NotProvided))

    /**
     * The [StateFlow] from the highest-priority, enabled navigation handler.
     *
     * This represents the navigation state of the currently active component.
     */
    val state: StateFlow<NavigationEventState<*>> = _state.asStateFlow()

    /**
     * Stores high-priority handlers that should be evaluated before default handlers.
     *
     * [ArrayDeque] is used for efficient `addFirst()` and `remove()` operations, which is ideal for
     * maintaining a Last-In, First-Out (LIFO) dispatch order. This means the most recently added
     * overlay handler is the first to be checked.
     *
     * @see [defaultHandlers]
     * @see [inProgressHandler]
     */
    private val overlayHandlers = ArrayDeque<NavigationEventHandler<*>>()

    /**
     * Stores standard-priority handlers.
     *
     * Like [overlayHandlers`, this uses [ArrayDeque] to efficiently manage a LIFO queue, ensuring
     * the most recently added default handler is checked first within its priority level.
     *
     * @see [overlayHandlers]
     * @see [inProgressHandler]
     */
    private val defaultHandlers = ArrayDeque<NavigationEventHandler<*>>()

    /**
     * The handler for a navigation event that is currently in progress.
     *
     * This handler has the highest dispatch priority, ensuring that terminal events (like
     * [dispatchOnCompleted] or [dispatchOnCancelled]) are delivered only to the participant of the
     * active navigation. This is cleared after the event is terminated.
     *
     * Notably, if this handler is removed while an event is in progress, it is implicitly treated
     * as a terminal event and receives a cancellation call before being removed.
     *
     * @see [overlayHandlers]
     * @see [defaultHandlers]
     */
    private var inProgressHandler: NavigationEventHandler<*>? = null

    /**
     * The direction of the navigation event currently in progress.
     *
     * This is non-null only when [inProgressHandler] is also non-null. Its lifecycle is tied
     * directly to the active navigation event.
     */
    private var inProgressDirection: NavigationEventDirection? = null

    /**
     * Holds inputs that were registered without a specific priority.
     *
     * These are typically treated as the lowest priority level, processed only after
     * [defaultInputs] and [overlayInputs].
     */
    val unspecifiedInputs = mutableSetOf<NavigationEventInput>()

    /**
     * Holds inputs registered with the [PRIORITY_DEFAULT] priority.
     *
     * This level is for primary UI content and is processed before [unspecifiedInputs] but after
     * [overlayInputs].
     */
    val defaultInputs = mutableSetOf<NavigationEventInput>()

    /**
     * Holds inputs registered with the [PRIORITY_OVERLAY] priority.
     *
     * This is the highest priority level, intended for UI elements like dialogs or bottom sheets
     * that appear on top of other content.
     */
    val overlayInputs = mutableSetOf<NavigationEventInput>()

    /** Whether at least one callback with `Default` priority is enabled. */
    var hasEnabledDefaultHandlers: Boolean = false

    /** Whether at least one callback with `Overlay` priority is enabled. */
    var hasEnabledOverlayHandlers: Boolean = false

    /**
     * Recalculates the enabled status for all callback priorities, notifies listeners of any
     * changes, and synchronizes the global navigation state.
     *
     * This is the central update method and should be called whenever a callback is added, removed,
     * or its own enabled status changes.
     */
    fun refreshEnabledHandlers() {
        // 1) Snapshot new truth from current callbacks.
        // Use `any` instead of `filter` to avoid allocating intermediate lists.
        // (`any` also short-circuits on the first match, making it strictly cheaper.)
        val newDefaultEnabled = defaultHandlers.any { it.isBackEnabled || it.isForwardEnabled }
        val newOverlayEnabled = overlayHandlers.any { it.isBackEnabled || it.isForwardEnabled }

        val defaultEnabledChanged = hasEnabledDefaultHandlers != newDefaultEnabled
        val overlayEnabledChanged = hasEnabledOverlayHandlers != newOverlayEnabled

        // 2) Notify only when a priority’s state actually changed.
        if (defaultEnabledChanged) {
            for (input in defaultInputs) {
                input.doOnHasEnabledHandlersChanged(hasEnabledHandlers = newDefaultEnabled)
            }
        }

        if (overlayEnabledChanged) {
            for (input in overlayInputs) {
                input.doOnHasEnabledHandlersChanged(hasEnabledHandlers = newOverlayEnabled)
            }
        }

        // Unspecified listeners reflect the aggregate flag; notify only if either priority changed.
        if (defaultEnabledChanged || overlayEnabledChanged) {
            val anyEnabled = newDefaultEnabled || newOverlayEnabled
            for (input in unspecifiedInputs) {
                input.doOnHasEnabledHandlersChanged(hasEnabledHandlers = anyEnabled)
            }
        }

        // 3) Commit new flags *after* notifications so change detection compares against the
        // previous published state. This prevents spurious notifications within the same cycle.
        hasEnabledDefaultHandlers = newDefaultEnabled
        hasEnabledOverlayHandlers = newOverlayEnabled

        // 4) Synchronize the global navigation state to the active (highest-priority) enabled
        // callback. Order: in-progress > back > forward.
        val enabledHandler =
            inProgressHandler
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Back)
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Forward)
        if (enabledHandler != null) {
            updateEnabledHandlerInfo(handler = enabledHandler)
        }
    }

    /**
     * Called by a [NavigationEventHandler] when its info changes via
     * [NavigationEventHandler.setInfo].
     *
     * This method centralizes the state update logic. It checks if the handler that changed is the
     * authoritative one (either the [inProgressHandler] or the highest-priority idle handler)
     * before updating the shared `_state`. This prevents lower-priority handlers from incorrectly
     * overwriting the state.
     */
    internal fun updateEnabledHandlerInfo(handler: NavigationEventHandler<*>) {
        // Pick the single handler that is allowed to control state right now.
        val activeHandler =
            inProgressHandler
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Back)
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Forward)

        if (activeHandler != handler) {
            return
        }

        // Calculate the new state information from the active handler.
        val newCurrentInfo = activeHandler.currentInfo ?: NotProvided
        val newBackInfo = resolveCombinedBackInfo()
        val newForwardInfo = activeHandler.forwardInfo

        // To avoid redundant state updates and notifications, exit if nothing has changed.
        val oldState = _state.value
        if (
            oldState.currentInfo == newCurrentInfo &&
                oldState.backInfo == newBackInfo &&
                oldState.forwardInfo == newForwardInfo
        ) {
            return
        }

        // Atomically commit the new information to the state flow.
        // When a gesture is in progress, we must preserve its event data.
        _state.update { state ->
            when (state) {
                is Idle -> Idle(newCurrentInfo, newBackInfo, newForwardInfo)
                is InProgress ->
                    InProgress(newCurrentInfo, newBackInfo, newForwardInfo, state.latestEvent)
            }
        }

        // Notify inputs directly for immediate, synchronous updates. This avoids
        // delays from the coroutine dispatcher, ensuring that consumers can react
        // to the state change within the same frame.
        for (input in overlayInputs) {
            input.doOnInfoChanged(newCurrentInfo, newBackInfo, newForwardInfo)
        }
        for (input in defaultInputs) {
            input.doOnInfoChanged(newCurrentInfo, newBackInfo, newForwardInfo)
        }
        for (input in unspecifiedInputs) {
            input.doOnInfoChanged(newCurrentInfo, newBackInfo, newForwardInfo)
        }
    }

    /**
     * Returns `true` if there is at least one [NavigationEventHandler] registered globally within
     * this processor is enabled.
     *
     * @return `true` if any handler is enabled, `false` otherwise.
     */
    fun hasEnabledHandler(): Boolean = hasEnabledOverlayHandlers || hasEnabledDefaultHandlers

    /**
     * Checks if there are any registered handlers, either overlay or normal.
     *
     * @return `true` if there is at least one overlay handler or one normal handler registered,
     *   `false` otherwise.
     */
    fun hasHandlers(): Boolean = overlayHandlers.isNotEmpty() || defaultHandlers.isNotEmpty()

    /**
     * Adds a new [NavigationEventHandler] to receive navigation events, associating it with its
     * [NavigationEventDispatcher].
     *
     * Handlers are placed into priority-specific queues
     * ([NavigationEventDispatcher.PRIORITY_OVERLAY] or
     * [NavigationEventDispatcher.PRIORITY_DEFAULT]) and within those queues, they are ordered in
     * Last-In, First-Out (LIFO) manner. This ensures that the most recently added handler of a
     * given priority is considered first.
     *
     * All handlers are invoked on the main thread. To stop receiving events, a handler must be
     * removed via [NavigationEventHandler.remove].
     *
     * @param dispatcher The [NavigationEventDispatcher] instance registering this handler. This
     *   link is stored on the handler itself to enable self-removal and state tracking.
     * @param handler The handler instance to be added.
     * @param priority The priority of the handler, determining its invocation order relative to
     *   others. See [NavigationEventDispatcher.Priority].
     * @throws IllegalArgumentException if [priority] is not one of the supported constants.
     * @throws IllegalArgumentException if the given handler is already registered with a different
     *   dispatcher.
     */
    @Suppress("PairedRegistration") // Handler is removed via `NavigationEventHandler.remove()`
    @MainThread
    fun addHandler(
        dispatcher: NavigationEventDispatcher,
        handler: NavigationEventHandler<*>,
        @Priority priority: Int = PRIORITY_DEFAULT,
    ) {
        // Enforce that a handler is not already registered with another dispatcher.
        require(handler.dispatcher == null) {
            "Handler '$handler' is already registered with a dispatcher"
        }

        // Add to the front of the appropriate queue to achieve LIFO ordering.
        when (priority) {
            PRIORITY_OVERLAY -> overlayHandlers.addFirst(handler)
            PRIORITY_DEFAULT -> defaultHandlers.addFirst(handler)
            else -> {
                // Since this method may be called from other targets (e.g., Swift),
                // IntDef lint checks may not be available. We must validate at runtime.
                throw IllegalArgumentException("Unsupported priority value: $priority")
            }
        }

        // Store the dispatcher reference on the callback for self-management and internal tracking.
        handler.dispatcher = dispatcher
        refreshEnabledHandlers()
    }

    /**
     * Removes a [NavigationEventHandler] from the processor's registry.
     *
     * If the handler is currently part of an active event (i.e., it is the [inProgressHandler]), it
     * will be notified of cancellation before being removed. This method is idempotent and can be
     * called safely even if the handler is not currently registered.
     *
     * @param handler The [NavigationEventHandler] to remove.
     */
    @MainThread
    fun removeHandler(handler: NavigationEventHandler<*>) {
        // If the handler is the one currently being processed, it needs to be notified of
        // cancellation and then cleared from the in-progress state.
        if (handler == inProgressHandler) {
            when (inProgressDirection) {
                NavigationEventDirection.Back -> handler.doOnBackCancelled()
                NavigationEventDirection.Forward -> handler.doOnForwardCancelled()
            }
            inProgressHandler = null
            inProgressDirection = null
        }

        // The `remove()` operation on ArrayDeque is efficient and simply returns `false` if the
        // element is not found. There's no need for a preceding `contains()` check.
        overlayHandlers.remove(handler)
        defaultHandlers.remove(handler)

        // Clear the dispatcher reference to mark the handler as unregistered and available for
        // re-registration.
        handler.dispatcher = null
        refreshEnabledHandlers()
    }

    /**
     * Dispatches an [NavigationEventHandler.onBackStarted] event with the given event to the
     * highest-priority enabled handler.
     *
     * If an event is currently in progress, it will be cancelled first to ensure a clean state for
     * the new event. Only the single, highest-priority enabled handler is notified and becomes the
     * [inProgressHandler].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the handler.
     */
    @MainThread
    fun dispatchOnStarted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        if (inProgressHandler != null) {
            // It's important to ensure that any ongoing operations from previous events are
            // properly cancelled before starting new ones to maintain a consistent state.
            dispatchOnCancelled(input, direction)
        }

        // Find the highest-priority enabled handler to handle this event.
        val handler = resolveEnabledHandler(direction)
        if (handler != null) {
            // Set this handler as the one in progress *before* execution. This ensures
            // `onCancelled` can be correctly handled if the handler removes itself during
            // `onEventStarted`.
            inProgressHandler = handler
            inProgressDirection = direction
            when (direction) {
                NavigationEventDirection.Back -> handler.doOnBackStarted(event)
                NavigationEventDirection.Forward -> handler.doOnForwardStarted(event)
            }
            _state.update { state ->
                InProgress(
                    currentInfo = state.currentInfo,
                    backInfo = state.backInfo,
                    forwardInfo = state.forwardInfo,
                    latestEvent = event,
                )
            }
        }
    }

    /**
     * Dispatches an [NavigationEventHandler.onBackProgressed] event with the given event.
     *
     * If a handler is currently in progress (from a [dispatchOnStarted] call), only that handler
     * will be notified. Otherwise, the highest-priority enabled handler will receive the progress
     * event. This is not a terminal event, so [inProgressHandler] is not cleared.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the handler.
     */
    @MainThread
    fun dispatchOnProgressed(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)
        // Progressed is not a terminal event, so `inProgress` is not cleared.

        if (handler != null) {
            when (direction) {
                NavigationEventDirection.Back -> handler.doOnBackProgressed(event)
                NavigationEventDirection.Forward -> handler.doOnForwardProgressed(event)
            }
            _state.update { state ->
                InProgress(
                    currentInfo = state.currentInfo,
                    backInfo = state.backInfo,
                    forwardInfo = state.forwardInfo,
                    latestEvent = event,
                )
            }
        }
    }

    /**
     * Dispatches a navigation completion event to the appropriate handler.
     *
     * If a handler is currently in progress, only that one will be notified. Otherwise, the
     * highest-priority enabled handler for the given [direction] is chosen. Completion is a
     * terminal event, so [inProgressHandler] is always cleared afterward.
     *
     * If no handler handles the event:
     * - For [NavigationEventDirection.Back], the [onBackCompletedFallback] action is invoked.
     * - For [NavigationEventDirection.Forward], no fallback is triggered.
     *
     * After dispatching, the dispatcher always transitions back to [Idle] state.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event that completed.
     * @param onBackCompletedFallback The action to invoke if no handler handles a back completion
     *   event.
     */
    @MainThread
    fun dispatchOnCompleted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        onBackCompletedFallback: OnBackCompletedFallback?,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)

        // Clear in-progress, as 'completed' is a terminal event.
        inProgressHandler = null
        inProgressDirection = null

        // No handler: only back events have a fallback to invoke.
        if (handler == null && direction == NavigationEventDirection.Back) {
            onBackCompletedFallback?.onBackCompletedFallback()
        }

        // No handler: does nothing.
        when (direction) {
            NavigationEventDirection.Back -> handler?.doOnBackCompleted()
            NavigationEventDirection.Forward -> handler?.doOnForwardCompleted()
        }

        // Completion is terminal regardless of handler outcome; return to Idle.
        _state.update { state ->
            Idle(
                currentInfo = state.currentInfo,
                backInfo = state.backInfo,
                forwardInfo = state.forwardInfo,
            )
        }
    }

    /**
     * Dispatches an [NavigationEventHandler.onBackCancelled] event.
     *
     * If a handler is currently in progress, only it will be notified. Otherwise, the
     * highest-priority enabled handler will be notified. This is a terminal event, clearing the
     * [inProgressHandler].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     */
    @MainThread
    fun dispatchOnCancelled(input: NavigationEventInput, direction: NavigationEventDirection) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)

        // Clear in-progress, as 'cancelled' is a terminal event.
        inProgressHandler = null
        inProgressDirection = null

        if (handler != null) {
            when (direction) {
                NavigationEventDirection.Back -> handler.doOnBackCancelled()
                NavigationEventDirection.Forward -> handler.doOnForwardCancelled()
            }

            _state.update { state ->
                Idle(
                    currentInfo = state.currentInfo,
                    backInfo = state.backInfo,
                    forwardInfo = state.forwardInfo,
                )
            }
        }
    }

    /**
     * Resolves which handler should handle a navigation event based on priority and enabled state.
     *
     * This function is the core of the priority dispatch system. It ensures that only one handler
     * is selected to receive an event by strictly enforcing dispatch order. The resolution process
     * is:
     * 1. It first scans **overlay** handlers, from most-to-least recently added.
     * 2. If no enabled overlay handler is found, it then scans **default** handlers in the same
     *    LIFO order.
     *
     * The very first handler that is found to be `isEnabled` is returned immediately.
     *
     * @return The single highest-priority [NavigationEventHandler] that is currently enabled, or
     *   `null` if no enabled handlers exist.
     */
    private fun resolveEnabledHandler(
        direction: NavigationEventDirection
    ): NavigationEventHandler<*>? {
        // `firstOrNull` is efficient and respects the LIFO order of the ArrayDeque.
        return when (direction) {
            NavigationEventDirection.Back -> {
                overlayHandlers.firstOrNull { it.isBackEnabled }
                    ?: defaultHandlers.firstOrNull { it.isBackEnabled }
            }
            NavigationEventDirection.Forward -> {
                overlayHandlers.firstOrNull { it.isForwardEnabled }
                    ?: defaultHandlers.firstOrNull { it.isForwardEnabled }
            }
            else -> error("Unsupported NavigationEventDirection: '$direction'.")
        }
    }

    /**
     * Resolves and aggregates [NavigationEventHandler.backInfo] from all enabled handlers to
     * provide a comprehensive view of the back navigation history.
     *
     * This method constructs a unified list of [NavigationEventInfo] by traversing the registered
     * handlers in order of priority: it first collects `backInfo` from all enabled **overlay**
     * handlers, followed by all enabled **default** handlers. This ordering ensures that the
     * resulting list reflects the hierarchical navigation state, with higher-priority contexts
     * appearing first.
     *
     * This is crucial for UIs that need to display a preview of the back stack, as it allows them
     * to accurately represent the destinations that the user will navigate through when repeatedly
     * going back.
     *
     * @return A `List<NavigationEventInfo>` containing the combined back navigation history,
     *   ordered by handler priority. The list will be empty if no enabled handlers provide
     *   `backInfo`.
     */
    private fun resolveCombinedBackInfo(): List<NavigationEventInfo> {
        // TODO(b/436248277): Finalize back-info combination policy.
        //  Ambiguity: when a parent (K4) hosts a child with its own back item (L1),
        //  should the combined path be `L1 -> K4 -> parentStack` or `L1 -> parentStack`?
        //  Decide if/when to include the host's current node, and document it.

        // This function intentionally uses loops and a single mutable list. This is a
        // performance optimization to avoid the intermediate list allocations that would
        // be created by using chained collection functions like `filter` or `flatMap`.
        val combinedBackInfo = mutableListOf<NavigationEventInfo>()

        // Process overlay handlers first to respect their higher priority.
        for (handler in overlayHandlers) {
            if (handler.isBackEnabled && handler.backInfo.isNotEmpty()) {
                combinedBackInfo.addAll(handler.backInfo)
            }
        }

        // Process default handlers second.
        for (handler in defaultHandlers) {
            if (handler.isBackEnabled && handler.backInfo.isNotEmpty()) {
                combinedBackInfo.addAll(handler.backInfo)
            }
        }

        return combinedBackInfo
    }
}
