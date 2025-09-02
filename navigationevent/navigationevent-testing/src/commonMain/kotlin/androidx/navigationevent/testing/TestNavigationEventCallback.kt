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

package androidx.navigationevent.testing

import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventCallback
import androidx.navigationevent.NavigationEventInfo

/**
 * Creates an instance of [TestNavigationEventCallback] without requiring an explicit generic type.
 *
 * This function is a convenience wrapper around the [TestNavigationEventCallback] constructor that
 * defaults its info type to `*`. Use this in tests where the specific type of [NavigationEventInfo]
 * is not relevant.
 */
public fun TestNavigationEventCallback(
    isBackEnabled: Boolean = true,
    onBackStarted: TestNavigationEventCallback<*>.(event: NavigationEvent) -> Unit = {},
    onBackProgressed: TestNavigationEventCallback<*>.(event: NavigationEvent) -> Unit = {},
    onBackCancelled: TestNavigationEventCallback<*>.() -> Unit = {},
    onBackCompleted: TestNavigationEventCallback<*>.() -> Unit = {},
): TestNavigationEventCallback<*> {
    return TestNavigationEventCallback(
        currentInfo = NavigationEventInfo.NotProvided,
        previousInfo = null,
        isBackEnabled = isBackEnabled,
        onBackStarted = onBackStarted,
        onBackProgressed = onBackProgressed,
        onBackCancelled = onBackCancelled,
        onBackCompleted = onBackCompleted,
    )
}

/**
 * A test implementation of [NavigationEventCallback] that records received events and invocation
 * counts.
 *
 * This class is primarily used in tests to verify that specific navigation event callbacks are
 * triggered as expected. It captures the [NavigationEvent] objects and counts how many times each
 * callback is fired.
 *
 * @param T The type of [NavigationEventInfo] this callback handles.
 * @param currentInfo The initial **current** navigation information for the callback.
 * @param previousInfo The initial **previous** navigation information. Defaults to `null`.
 * @param isBackEnabled Determines if the callback should process events. Defaults to `true`.
 * @param onBackStarted An optional lambda to execute when `onEventStarted` is called.
 * @param onBackProgressed An optional lambda to execute when `onEventProgressed` is called.
 * @param onBackCancelled An optional lambda to execute when `onEventCancelled` is called.
 * @param onBackCompleted An optional lambda to execute when `onEventCompleted` is called.
 */
public class TestNavigationEventCallback<T : NavigationEventInfo>(
    currentInfo: T,
    previousInfo: T? = null,
    isBackEnabled: Boolean = true,
    private val onBackStarted: TestNavigationEventCallback<T>.(event: NavigationEvent) -> Unit = {},
    private val onBackProgressed: TestNavigationEventCallback<T>.(event: NavigationEvent) -> Unit =
        {},
    private val onBackCancelled: TestNavigationEventCallback<T>.() -> Unit = {},
    private val onBackCompleted: TestNavigationEventCallback<T>.() -> Unit = {},
) : NavigationEventCallback<T>(isBackEnabled) {

    init {
        setInfo(currentInfo = currentInfo, previousInfo = previousInfo)
    }

    private val _onBackStartedEvents = mutableListOf<NavigationEvent>()

    /** A [List] of all events received by the [onBackStarted] callback. */
    public val onBackStartedEvents: List<NavigationEvent>
        get() = _onBackStartedEvents.toList()

    /** The number of times [onBackStarted] has been invoked. */
    public val onBackStartedInvocations: Int
        get() = _onBackStartedEvents.size

    private val _onBackProgressedEvents = mutableListOf<NavigationEvent>()

    /** A [List] of all events received by the [onBackProgressed] callback. */
    public val onBackProgressedEvents: List<NavigationEvent>
        get() = _onBackProgressedEvents.toList()

    /** The number of times [onBackProgressed] has been invoked. */
    public val onBackProgressedInvocations: Int
        get() = _onBackProgressedEvents.size

    /** The number of times [onBackCompleted] has been invoked. */
    public var onBackCompletedInvocations: Int = 0
        private set

    /** The number of times [onBackCancelled] has been invoked. */
    public var onBackCancelledInvocations: Int = 0
        private set

    override fun onBackStarted(event: NavigationEvent) {
        _onBackStartedEvents += event
        onBackStarted.invoke(this, event)
    }

    override fun onBackProgressed(event: NavigationEvent) {
        _onBackProgressedEvents += event
        onBackProgressed.invoke(this, event)
    }

    override fun onBackCompleted() {
        onBackCompletedInvocations++
        onBackCompleted(this)
    }

    override fun onBackCancelled() {
        onBackCancelledInvocations++
        onBackCancelled(this)
    }
}
