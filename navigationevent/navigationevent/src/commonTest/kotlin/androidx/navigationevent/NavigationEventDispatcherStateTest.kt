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

import androidx.kruth.assertThat
import androidx.navigationevent.NavigationEventInfo.NotProvided
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import androidx.navigationevent.testing.TestNavigationEvent
import androidx.navigationevent.testing.TestNavigationEventCallback
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class NavigationEventDispatcherStateTest {

    private val dispatcherOwner = TestNavigationEventDispatcherOwner()
    private val dispatcher = dispatcherOwner.navigationEventDispatcher
    private val inputHandler = NavigationEventInputHandler(dispatcher)

    @Test
    fun state_whenMultipleCallbacksAreAdded_thenReflectsInfoFromLastAddedCallback() = runTest {
        val homeCallback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("home"))
        val detailsCallback =
            TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))

        assertThat(dispatcher.state.value).isEqualTo(Idle(NotProvided))

        dispatcher.addCallback(homeCallback)
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("home")))

        // Callbacks are prioritized like a stack (LIFO), so adding a new one makes it active.
        dispatcher.addCallback(detailsCallback)
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))
    }

    @Test
    fun state_whenSetInfoIsCalledOnActiveCallback_thenStateIsUpdated() = runTest {
        val callback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("initial"))
        dispatcher.addCallback(callback)

        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("initial")))

        // Calling setInfo on the active callback should immediately update the dispatcher's state.
        callback.setInfo(currentInfo = HomeScreenInfo("updated"), previousInfo = null)

        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("updated")))
    }

    @Test
    fun state_whenSetInfoIsCalledOnInactiveCallback_thenStateIsUnchanged() = runTest {
        val homeCallback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("home"))
        val detailsCallback =
            TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))
        dispatcher.addCallback(homeCallback)
        dispatcher.addCallback(detailsCallback)

        // The state should reflect the last-added (active) callback.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))

        // Calling setInfo on an inactive callback should NOT affect the global state.
        // This confirms our logic in `onCallbackInfoChanged` is working correctly.
        homeCallback.setInfo(currentInfo = HomeScreenInfo("home-updated"), previousInfo = null)

        // The state should remain unchanged because the update came from a non-active callback.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("details")))
    }

    @Test
    fun state_whenFullGestureLifecycleIsDispatched_thenTransitionsToInProgressAndBackToIdle() {
        val callbackInfo = HomeScreenInfo("home")
        val callback = TestNavigationEventCallback(currentInfo = callbackInfo)
        dispatcher.addCallback(callback)

        val startEvent = TestNavigationEvent(touchX = 0.1F)
        val progressEvent = TestNavigationEvent(touchX = 0.3f)

        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))

        // Starting a gesture should move the state to InProgress with the start event.
        inputHandler.handleOnStarted(startEvent)
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(callbackInfo)
        assertThat(state.previousInfo).isNull()
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Progressing the gesture should keep it InProgress but update to the latest event.
        inputHandler.handleOnProgressed(progressEvent)
        state = dispatcher.state.value as InProgress
        assertThat(state.latestEvent).isEqualTo(progressEvent)

        // Completing the gesture should return the state to Idle.
        inputHandler.handleOnCompleted()
        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))
    }

    @Test
    fun state_whenGestureIsCancelled_thenReturnsToIdleState() {
        val callbackInfo = HomeScreenInfo("home")
        val callback = TestNavigationEventCallback(currentInfo = callbackInfo)
        dispatcher.addCallback(callback)

        val startEvent = TestNavigationEvent()

        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))

        // Starting a gesture moves the state to InProgress.
        inputHandler.handleOnStarted(startEvent)
        assertThat(dispatcher.state.value).isEqualTo(InProgress(callbackInfo, null, startEvent))

        // Cancelling the gesture should also return the state to Idle.
        inputHandler.handleOnCancelled()
        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))
    }

    @Test
    fun inProgressState_whenInfoIsUpdatedDuringGesture_thenReflectsCorrectStateProperties() {
        val firstInfo = HomeScreenInfo("initial")
        val callback = TestNavigationEventCallback(currentInfo = firstInfo)
        dispatcher.addCallback(callback)

        val startEvent = TestNavigationEvent(touchX = 0.1F)

        // Start the gesture.
        inputHandler.handleOnStarted(startEvent)

        // At the start, previousInfo is null.
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(firstInfo)
        assertThat(state.previousInfo).isNull()
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Update the info mid-gesture. This triggers our updated `onCallbackInfoChanged` logic.
        val secondInfo = HomeScreenInfo("updated")
        callback.setInfo(currentInfo = secondInfo, previousInfo = firstInfo)

        // The state should now reflect the updated info. The `previousInfo` is now captured.
        state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(secondInfo)
        assertThat(state.previousInfo).isEqualTo(firstInfo)
        assertThat(state.latestEvent).isEqualTo(startEvent) // Event hasn't changed yet.

        // Complete the gesture.
        inputHandler.handleOnCompleted()
        assertThat(dispatcher.state.value).isEqualTo(Idle(secondInfo))
    }

    @Test
    fun inProgressState_whenNewGestureStartsAfterAnotherCompletes_thenPreviousInfoIsNotStale() {
        val initialInfo = HomeScreenInfo("initial")
        val callback = TestNavigationEventCallback(currentInfo = initialInfo)
        dispatcher.addCallback(callback)

        // FIRST GESTURE: Create a complex state.
        inputHandler.handleOnStarted(TestNavigationEvent(touchX = 0.1f))
        callback.setInfo(currentInfo = HomeScreenInfo("updated"), previousInfo = null)
        inputHandler.handleOnCompleted()

        // After the first gesture, the final state is Idle with the updated info.
        val finalInfo = HomeScreenInfo("updated")
        assertThat(dispatcher.state.value).isEqualTo(Idle(finalInfo))

        // SECOND GESTURE: Verify that previousInfo was cleared by `clearPreviousInfo()`.
        val event2 = TestNavigationEvent(touchX = 0.3f)
        inputHandler.handleOnStarted(event2)

        // When a new gesture starts, `previousInfo` should be null, not stale data.
        val state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(finalInfo)
        assertThat(state.previousInfo).isNull()
        assertThat(state.latestEvent).isEqualTo(event2)
    }

    @Test
    fun state_whenActiveDispatcherIsDisabled_fallsBackToSiblingDispatcherCallback() {
        // Create two sibling dispatchers sharing the same owner and processor.
        val childDispatcher = NavigationEventDispatcher(parentDispatcher = dispatcher)

        val callbackA = TestNavigationEventCallback(currentInfo = HomeScreenInfo("A"))
        dispatcher.addCallback(callbackA)
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("A")))

        val callbackB = TestNavigationEventCallback(currentInfo = DetailsScreenInfo("B"))
        childDispatcher.addCallback(callbackB)
        // Assert that state reflects callbackB, which was added last and is now active.
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("B")))

        // Disable the dispatcher that hosts the currently active callback.
        childDispatcher.isEnabled = false

        // The processor should now ignore callbackB and the state should
        // fall back to the next-highest priority callback, which is callbackA.
        assertThat(dispatcher.state.value).isEqualTo(Idle(HomeScreenInfo("A")))

        // Re-enable the dispatcher.
        childDispatcher.isEnabled = true

        // The state should once again reflect callbackB, as it's now enabled
        // and has higher priority (due to being added last).
        assertThat(dispatcher.state.value).isEqualTo(Idle(DetailsScreenInfo("B")))
    }
}

/** A sealed interface for type-safe navigation information. */
sealed interface TestInfo : NavigationEventInfo

data class HomeScreenInfo(val id: String) : TestInfo

data class DetailsScreenInfo(val id: String) : TestInfo
