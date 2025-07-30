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

/**
 * An input handler that can send events to a [NavigationEventDispatcher].
 *
 * @param dispatcher The [NavigationEventDispatcher] to send events to.
 */
public class NavigationEventInputHandler(dispatcher: NavigationEventDispatcher) :
    AbstractNavigationEventInputHandler(dispatcher) {
    @MainThread
    public fun sendOnStarted(event: NavigationEvent) {
        dispatchOnStarted(event)
    }

    @MainThread
    public fun sendOnProgressed(event: NavigationEvent) {
        dispatchOnProgressed(event)
    }

    @MainThread
    public fun sendOnCompleted() {
        dispatchOnCompleted()
    }

    @MainThread
    public fun sendOnCancelled() {
        dispatchOnCancelled()
    }
}
