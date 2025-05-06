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

/**
 * Call for handling [NavigationEventDispatcher] callbacks.
 *
 * This class maintains its own [isEnabled] state and will only receive callbacks when enabled.
 *
 * @param isEnabled The default enabled state for this callback.
 * @param priority The priority this callback should be registered with.
 * @see NavigationEventDispatcher
 */
public abstract class NavigationEventCallback(
    /**
     * The enabled state of the callback. Only when this callback is enabled will it receive
     * callbacks to [onEventCompleted].
     */
    isEnabled: Boolean,
    /** The priority of this callback. */
    public val priority: NavigationEventPriority = NavigationEventPriority.Default
) {

    public var isEnabled: Boolean = isEnabled
        set(value) {
            field = value
            enabledChangedCallback?.invoke()
        }

    internal var enabledChangedCallback: (() -> Unit)? = null

    /**
     * Whether this callback should consume the callback from the [NavigationEventDispatcher] or
     * allow it to continue.
     */
    public var isPassThrough: Boolean = false

    /**
     * A set of active [Subscription]s associated with this [NavigationEventCallback].
     *
     * Each subscription represents a registration with a [NavigationEventDispatcher]. These are
     * automatically unsubscribed when [remove] is called to avoid unintended event dispatches.
     */
    private val subscriptions = mutableSetOf<Subscription>()

    /**
     * Unsubscribes this [NavigationEventCallback] from all registered [NavigationEventDispatcher]s.
     *
     * This method invokes [Subscription.unsubscribe] on all tracked subscriptions and clears them,
     * ensuring the callback no longer receives navigation events from any dispatcher it was
     * previously registered with.
     */
    public fun remove() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
        subscriptions.clear()
    }

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnStarted] callback. */
    public open fun onEventStarted(event: NavigationEvent) {}

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnProgressed] callback. */
    public open fun onEventProgressed(event: NavigationEvent) {}

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnCompleted] callback. */
    public open fun onEventCompleted() {}

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnCancelled] callback. */
    public open fun onEventCancelled() {}

    /**
     * Tracks a [Subscription] associated with this [NavigationEventCallback].
     *
     * Tracked subscriptions will be automatically unsubscribed when [remove] is called, preventing
     * unintended callback invocations.
     *
     * @param subscription The [Subscription] to be tracked for later unsubscription.
     */
    internal fun addSubscription(subscription: Subscription) {
        subscriptions += subscription
    }

    /**
     * Represents a registration link between a [NavigationEventDispatcher] and a
     * [NavigationEventCallback].
     *
     * This allows a [NavigationEventCallback] to unsubscribe from all associated dispatchers by
     * calling [NavigationEventCallback.remove], effectively stopping event delivery to the
     * callback.
     */
    internal fun interface Subscription {
        /**
         * Unsubscribes a [NavigationEventCallback] from an associated [NavigationEventDispatcher].
         */
        fun unsubscribe()
    }
}
