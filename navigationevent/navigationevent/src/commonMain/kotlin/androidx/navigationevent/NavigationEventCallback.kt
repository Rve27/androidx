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
 * @see NavigationEventDispatcher
 */
public abstract class NavigationEventCallback(
    /**
     * The enabled state of the callback. Only when this callback is enabled will it receive
     * callbacks to [onEventCompleted].
     */
    isEnabled: Boolean
) {

    public var isEnabled: Boolean = isEnabled
        set(value) {
            field = value
            dispatcher?.updateEnabledCallbacks()
        }

    /**
     * Whether this callback should consume the callback from the [NavigationEventDispatcher] or
     * allow it to continue.
     */
    public var isPassThrough: Boolean = false

    internal var dispatcher: NavigationEventDispatcher? = null

    public fun remove() {
        dispatcher?.removeCallback(this)
    }

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnStarted] callback. */
    public open fun onEventStarted(event: NavigationEvent) {}

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnProgressed] callback. */
    public open fun onEventProgressed(event: NavigationEvent) {}

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnCompleted] callback. */
    public open fun onEventCompleted() {}

    /** Callback for handling the [NavigationEventDispatcher.dispatchOnCancelled] callback. */
    public open fun onEventCancelled() {}
}
