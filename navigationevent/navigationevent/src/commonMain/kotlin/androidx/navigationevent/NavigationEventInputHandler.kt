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
import androidx.navigationevent.NavigationEventDirection.Companion.Backward

/**
 * An abstract input handler that can send events to a [NavigationEventDispatcher].
 *
 * @param dispatcher The [NavigationEventDispatcher] to send events to.
 */
public abstract class NavigationEventInputHandler(
    private val dispatcher: NavigationEventDispatcher
) {
    @Suppress("PairedRegistration")
    @MainThread
    protected fun addOnHasEnabledCallbacksChangedCallback(callback: (Boolean) -> Unit) {
        dispatcher.addOnHasEnabledCallbacksChangedCallback(inputHandler = this, callback)
    }

    @MainThread
    protected fun dispatchOnStarted(event: NavigationEvent) {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher.dispatchOnStarted(inputHandler = this, direction = Backward, event)
    }

    @MainThread
    protected fun dispatchOnProgressed(event: NavigationEvent) {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher.dispatchOnProgressed(inputHandler = this, direction = Backward, event)
    }

    @MainThread
    protected fun dispatchOnCancelled() {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher.dispatchOnCancelled(inputHandler = this, direction = Backward)
    }

    @MainThread
    protected fun dispatchOnCompleted() {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher.dispatchOnCompleted(inputHandler = this, direction = Backward)
    }
}
