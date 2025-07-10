/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent

import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEventPriority.Companion.Default
import androidx.navigationevent.NavigationEventPriority.Companion.Overlay

/**
 * Creates a [NavigationEventDispatcher] instance.
 *
 * This dispatcher acts as a localized entry point for registering [NavigationEventCallback]
 * instances and triggering navigation events within a specific part of your application (e.g., a
 * composable or a fragment). It delegates the actual event processing and callback management to a
 * [NavigationEventProcessor] that is shared across all parent-child dispatchers.
 *
 * @param fallbackOnBackPressed An optional lambda to be invoked if a navigation event completes and
 *   no registered [NavigationEventCallback] handles it. This provides a default "back" action.
 * @param onHasEnabledCallbacksChanged An optional lambda that will be called whenever the global
 *   state of whether there are any enabled callback changes.
 */
public class NavigationEventDispatcher(
    private val fallbackOnBackPressed: (() -> Unit)? = null,
    private val onHasEnabledCallbacksChanged: ((Boolean) -> Unit)? = null,
) {

    /**
     * The internal, shared processor responsible for managing all registered
     * [NavigationEventCallback]s and orchestrating the actual event dispatching across all
     * [NavigationEventDispatcher] instances. This ensures consistent ordering and state for all
     * navigation events.
     */
    internal val sharedProcessor: NavigationEventProcessor = NavigationEventProcessor()

    init {
        // If a lambda for changes in enabled callbacks is provided, register it with the
        // shared processor. This allows this specific dispatcher instance (or its consumers)
        // to be notified of global changes in the callback enablement state.
        if (onHasEnabledCallbacksChanged != null) {
            sharedProcessor.addOnHasEnabledCallbacksChangedCallback(onHasEnabledCallbacksChanged)
        }
    }

    /**
     * Adds a callback that will be notified when the overall enabled state of registered callbacks
     * changes.
     *
     * @param callback The callback to invoke when the enabled state changes.
     */
    internal fun addOnHasEnabledCallbacksChangedCallback(callback: (Boolean) -> Unit) {
        sharedProcessor.addOnHasEnabledCallbacksChangedCallback(callback)
    }

    /**
     * Returns `true` if there is at least one [NavigationEventCallback.isEnabled] callback
     * registered with this dispatcher.
     *
     * @return True if there is at least one enabled callback.
     */
    public fun hasEnabledCallbacks(): Boolean = sharedProcessor.hasEnabledCallbacks()

    /**
     * Recomputes and updates the current [hasEnabledCallbacks] state based on the enabled status of
     * all registered callbacks. This method should be called whenever a callback's enabled state or
     * its registration status (added/removed) changes.
     */
    internal fun updateEnabledCallbacks() {
        sharedProcessor.updateEnabledCallbacks()
    }

    /**
     * Adds a new [NavigationEventCallback] to receive navigation events.
     *
     * **Callbacks are invoked based on [priority], and then by recency.** All [Overlay] callbacks
     * are called before any [Default] callbacks. Within each priority group, callbacks are invoked
     * in a Last-In, First-Out (LIFO) order—the most recently added callback is called first.
     *
     * All callbacks are invoked on the main thread. To stop receiving events, a callback must be
     * removed via [NavigationEventCallback.remove].
     *
     * @param callback The callback instance to be added.
     * @param priority The priority of the callback, determining its invocation order relative to
     *   others. See [NavigationEventPriority].
     * @throws IllegalArgumentException if the given callback is already registered with a different
     *   dispatcher.
     */
    @Suppress("PairedRegistration") // Callback is removed via `NavigationEventCallback.remove()`
    @MainThread
    public fun addCallback(
        callback: NavigationEventCallback,
        priority: NavigationEventPriority = Default,
    ) {
        sharedProcessor.addCallback(dispatcher = this, callback, priority)
    }

    internal fun removeCallback(callback: NavigationEventCallback) {
        sharedProcessor.removeCallback(callback)
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventStarted] event with the given event. This call is
     * delegated to the shared [NavigationEventProcessor].
     *
     * @param event [NavigationEvent] to dispatch to the callbacks.
     */
    @MainThread
    public fun dispatchOnStarted(event: NavigationEvent) {
        sharedProcessor.dispatchOnStarted(event)
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventProgressed] event with the given event. This call
     * is delegated to the shared [NavigationEventProcessor].
     *
     * @param event [NavigationEvent] to dispatch to the callbacks.
     */
    @MainThread
    public fun dispatchOnProgressed(event: NavigationEvent) {
        sharedProcessor.dispatchOnProgressed(event)
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventCompleted] event. This call is delegated to the
     * shared [NavigationEventProcessor], passing the fallback action.
     */
    @MainThread
    public fun dispatchOnCompleted() {
        sharedProcessor.dispatchOnCompleted(fallbackOnBackPressed)
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventCancelled] event. This call is delegated to the
     * shared [NavigationEventProcessor].
     */
    @MainThread
    public fun dispatchOnCancelled() {
        sharedProcessor.dispatchOnCancelled()
    }
}
