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
@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.navigationevent

import androidx.annotation.MainThread
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.navigationevent.NavigationEventInfo.NotProvided
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import androidx.navigationevent.testing.TestNavigationEventCallback
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class NavigationEventDispatcherTest {

    private val dispatcherOwner = TestNavigationEventDispatcherOwner()
    private val dispatcher = dispatcherOwner.navigationEventDispatcher
    private val input = TestNavigationEventInput().also { dispatcher.addInput(it) }

    // region Core API

    @Test
    fun dispatch_onStarted_thenOnStartedIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(NavigationEvent())

        assertThat(callback.startedInvocations).isEqualTo(1)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onProgressed_thenOnProgressedIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnProgressed(NavigationEvent())

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(1)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onCompleted_theOnCompletedIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_onCancelled_theOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCancelled()

        assertThat(callback.startedInvocations).isEqualTo(0)
        assertThat(callback.progressedInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(0)
        assertThat(callback.cancelledInvocations).isEqualTo(1)
    }

    @Test
    fun removeCallback_whenNavigationIsInProgress_thenOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()

        // We need to capture the state when onEventCancelled is called to verify the order.
        var startedInvocationsAtCancelTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventCancelled = {
                    // Capture the count of 'started' invocations when 'cancelled' is called.
                    startedInvocationsAtCancelTime = this.startedInvocations
                }
            )
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(NavigationEvent())
        // Sanity check that navigation has started.
        assertThat(callback.startedInvocations).isEqualTo(1)

        callback.remove()

        // Assert that onEventCancelled was called once, and it happened after onEventStarted.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        assertThat(startedInvocationsAtCancelTime).isEqualTo(1)
    }

    @Test
    fun dispatch_whenCallbackDisablesItself_thenOnCancelledIsNotSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(onEventStarted = { isEnabled = false })
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(NavigationEvent())
        input.handleOnCompleted()

        // The callback was disabled, but cancellation should not be triggered.
        // The 'completed' event should still be received because the navigation was in progress.
        assertThat(callback.startedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun setEnabled_whenNavigationIsInProgress_thenOnCancelledIsNotSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(NavigationEvent())
        assertThat(callback.startedInvocations).isEqualTo(1)

        callback.isEnabled = false

        // Assert that disabling the callback does not trigger a cancellation.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenCallbackRemovesItselfOnStarted_thenOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()
        var cancelledInvocationsAtStartTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventStarted = {
                    // Capture the 'cancelled' count before removing to ensure it was 0.
                    cancelledInvocationsAtStartTime = this.cancelledInvocations
                    remove()
                }
            )
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(NavigationEvent())

        // Assert that 'onEventStarted' was called.
        assertThat(callback.startedInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' was called from within 'onEventStarted'.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' had not been called before 'remove()'.
        assertThat(cancelledInvocationsAtStartTime).isEqualTo(0)
    }

    @Test
    fun dispatch_whenAnotherNavigationIsInProgress_thenPreviousIsCancelled() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)

        // Start the first navigation.
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(NavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Start the second navigation, which should cancel the first.
        input.handleOnStarted(NavigationEvent())

        // Assert callback1 was cancelled and callback2 was started.
        assertThat(callback1.cancelledInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(1)

        // Complete the second navigation.
        input.handleOnCompleted()
        assertThat(callback2.completedInvocations).isEqualTo(1)

        // Ensure callback1 was not affected by the completion of the second navigation.
        assertThat(callback1.completedInvocations).isEqualTo(0)
    }

    @Test
    fun addCallback_whenNavigationIsInProgress_thenNewCallbackIsIgnoredForCurrentNavigation() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(NavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        // Add a second callback while the first navigation is in progress.
        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Complete the first navigation.
        input.handleOnCompleted()

        // Assert that only the first callback was affected.
        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(0)
        assertThat(callback2.completedInvocations).isEqualTo(0)

        // Start and complete a second navigation.
        input.handleOnStarted(NavigationEvent())
        input.handleOnCompleted()

        // Assert that the second navigation was handled by the new top callback (callback2).
        assertThat(callback1.startedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback1.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback2.startedInvocations).isEqualTo(1)
        assertThat(callback2.completedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_whenNoEnabledCallbacksExist_thenFallbackIsInvoked() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()

        // After disabling the only callback, the fallback should be called.
        callback.isEnabled = false
        input.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun dispatch_whenOverlayCallbackExists_thenOverlaySupersedesDefault() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()

        // The overlay callback should handle the event, and the normal one should not.
        assertThat(overlayCallback.completedInvocations).isEqualTo(1)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenDisabledOverlayCallbackExists_thenDefaultCallbackIsInvoked() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        // The highest priority callback is disabled.
        overlayCallback.isEnabled = false

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()

        // The event should skip the disabled overlay and be handled by the default.
        assertThat(overlayCallback.completedInvocations).isEqualTo(0)
        assertThat(normalCallback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenCallbackIsRegisteredWithAnotherDispatcher_thenThrowsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher1 = NavigationEventDispatcher()
        dispatcher1.addCallback(callback)

        val dispatcher2 = NavigationEventDispatcher()
        assertThrows<IllegalArgumentException> { dispatcher2.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addCallback_whenCallbackIsAlreadyRegistered_thenThrowsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher = NavigationEventDispatcher()
        dispatcher.addCallback(callback)

        assertThrows<IllegalArgumentException> { dispatcher.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addCallback_whenMultipleOverlayCallbacksExist_thenLastAddedIsInvoked() {
        val dispatcher = NavigationEventDispatcher()
        val firstOverlayCallback = TestNavigationEventCallback()
        val secondOverlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)
        dispatcher.addCallback(firstOverlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(secondOverlayCallback, NavigationEventPriority.Overlay)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()

        // Only the last-added overlay callback should handle the event.
        assertThat(secondOverlayCallback.completedInvocations).isEqualTo(1)
        assertThat(firstOverlayCallback.completedInvocations).isEqualTo(0)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenNoCallbacksExist_thenFallbackIsInvoked() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })

        // With no callbacks registered at all, the fallback should still work.
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()

        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun setEnabled_whenDisabledCallbackIsReEnabled_thenItReceivesEvents() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // Disable the callback and confirm it doesn't receive an event.
        callback.isEnabled = false
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(0)

        // Re-enable the callback.
        callback.isEnabled = true
        input.handleOnCompleted()

        // It should now receive the event.
        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_whenNoNavigationIsInProgress_thenDispatchesToTopCallback() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // Dispatching progress or completed without a start should still notify the top callback.
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnProgressed(NavigationEvent())
        assertThat(callback.progressedInvocations).isEqualTo(1)

        input.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Ensure no cancellation was ever triggered.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun addCallback_whenCallbackIsRemovedAndReAdded_thenBehavesAsNew() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Remove the callback.
        callback.remove()
        input.handleOnCompleted()
        // Invocations should not increase.
        assertThat(callback.completedInvocations).isEqualTo(1)

        // Re-add the same callback instance. It should be treated as a new callback.
        dispatcher.addCallback(callback)
        input.handleOnCompleted()
        // Invocations should increase again.
        assertThat(callback.completedInvocations).isEqualTo(2)
    }

    @Test
    fun addInput_whenAdded_onAttachIsCalled() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()

        dispatcher.addInput(input)

        assertThat(input.addedInvocations).isEqualTo(1)
        assertThat(input.currentDispatcher).isEqualTo(dispatcher)
    }

    @Test
    fun addInput_whenAddedTwice_onAttachIsCalledOnce() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()

        dispatcher.addInput(input)
        dispatcher.addInput(input)

        assertThat(input.addedInvocations).isEqualTo(1)
    }

    @Test
    fun removeInputHandler_whenRemoved_onDetachIsCalled() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        assertThat(input.addedInvocations).isEqualTo(1)

        dispatcher.removeInput(input)

        assertThat(input.removedInvocations).isEqualTo(1)
        assertThat(input.currentDispatcher).isNull()
    }

    @Test
    fun removeInputHandler_whenNotRegistered_onDetachIsNotCalled() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()

        // Try to remove a input that was never added.
        dispatcher.removeInput(input)

        assertThat(input.removedInvocations).isEqualTo(0)
    }

    @Test
    fun dispose_detachesInputHandlers() {
        val dispatcher = NavigationEventDispatcher()
        val handler1 = TestNavigationEventInput()
        val handler2 = TestNavigationEventInput()
        dispatcher.addInput(handler1)
        dispatcher.addInput(handler2)

        dispatcher.dispose()

        assertThat(handler1.removedInvocations).isEqualTo(1)
        assertThat(handler2.removedInvocations).isEqualTo(1)
        assertThat(handler1.currentDispatcher).isNull()
        assertThat(handler2.currentDispatcher).isNull()
    }

    @Test
    fun addInput_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        val input = TestNavigationEventInput()
        assertThrows<IllegalStateException> { dispatcher.addInput(input) }
            .hasMessageThat()
            .contains("This NavigationEventDispatcher has already been disposed")
    }

    @Test
    fun removeInputHandler_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        val input = TestNavigationEventInput()
        assertThrows<IllegalStateException> { dispatcher.removeInput(input) }
            .hasMessageThat()
            .contains("This NavigationEventDispatcher has already been disposed")
    }

    @Test
    fun onHasEnabledCallbacksChanged_whenHandlerIsRegistered_callbackIsFired() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)

        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(0)

        // Add a callback to trigger the onHasEnabledCallbacksChanged listener
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(1)

        // Disable the callback to trigger it again
        callback.isEnabled = false
        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(2)
    }

    @Test
    fun onHasEnabledCallbacksChanged_whenHandlerIsRemoved_callbackIsNotFired() {
        val dispatcher = NavigationEventDispatcher()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)
        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(1)

        dispatcher.removeInput(input)

        // Add another callback; the removed input should not be notified
        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)
        assertThat(input.onHasEnabledCallbacksChangedInvocations).isEqualTo(1) // Unchanged
    }

    @Test
    fun dispose_whenParentIsDisposed_detachesChildInputHandlers() {
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)

        val parentHandler = TestNavigationEventInput()
        val childHandler = TestNavigationEventInput()
        parentDispatcher.addInput(parentHandler)
        childDispatcher.addInput(childHandler)

        // Disposing the parent should cascade to the child
        parentDispatcher.dispose()

        assertThat(parentHandler.removedInvocations).isEqualTo(1)
        assertThat(childHandler.removedInvocations).isEqualTo(1)
        assertThat(parentHandler.currentDispatcher).isNull()
        assertThat(childHandler.currentDispatcher).isNull()
    }

    // endregion Core API

    // region Passive Listeners API

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

        val startEvent = NavigationEvent(touchX = 0.1F)
        val progressEvent = NavigationEvent(touchX = 0.3f)

        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))

        // Starting a gesture should move the state to InProgress with the start event.
        input.handleOnStarted(startEvent)
        var state = dispatcher.state.value as InProgress
        assertThat(state.currentInfo).isEqualTo(callbackInfo)
        assertThat(state.previousInfo).isNull()
        assertThat(state.latestEvent).isEqualTo(startEvent)

        // Progressing the gesture should keep it InProgress but update to the latest event.
        input.handleOnProgressed(progressEvent)
        state = dispatcher.state.value as InProgress
        assertThat(state.latestEvent).isEqualTo(progressEvent)

        // Completing the gesture should return the state to Idle.
        input.handleOnCompleted()
        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))
    }

    @Test
    fun state_whenGestureIsCancelled_thenReturnsToIdleState() {
        val callbackInfo = HomeScreenInfo("home")
        val callback = TestNavigationEventCallback(currentInfo = callbackInfo)
        dispatcher.addCallback(callback)

        val startEvent = NavigationEvent()

        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))

        // Starting a gesture moves the state to InProgress.
        input.handleOnStarted(startEvent)
        assertThat(dispatcher.state.value).isEqualTo(InProgress(callbackInfo, null, startEvent))

        // Cancelling the gesture should also return the state to Idle.
        input.handleOnCancelled()
        assertThat(dispatcher.state.value).isEqualTo(Idle(callbackInfo))
    }

    @Test
    fun inProgressState_whenInfoIsUpdatedDuringGesture_thenReflectsCorrectStateProperties() {
        val firstInfo = HomeScreenInfo("initial")
        val callback = TestNavigationEventCallback(currentInfo = firstInfo)
        dispatcher.addCallback(callback)

        val startEvent = NavigationEvent(touchX = 0.1F)

        // Start the gesture.
        input.handleOnStarted(startEvent)

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
        input.handleOnCompleted()
        assertThat(dispatcher.state.value).isEqualTo(Idle(secondInfo))
    }

    @Test
    fun inProgressState_whenNewGestureStartsAfterAnotherCompletes_thenPreviousInfoIsNotStale() {
        val initialInfo = HomeScreenInfo("initial")
        val callback = TestNavigationEventCallback(currentInfo = initialInfo)
        dispatcher.addCallback(callback)

        // FIRST GESTURE: Create a complex state.
        input.handleOnStarted(NavigationEvent(touchX = 0.1f))
        callback.setInfo(currentInfo = HomeScreenInfo("updated"), previousInfo = null)
        input.handleOnCompleted()

        // After the first gesture, the final state is Idle with the updated info.
        val finalInfo = HomeScreenInfo("updated")
        assertThat(dispatcher.state.value).isEqualTo(Idle(finalInfo))

        // SECOND GESTURE: Verify that previousInfo was cleared by `clearPreviousInfo()`.
        val event2 = NavigationEvent(touchX = 0.3f)
        input.handleOnStarted(event2)

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

    @Test
    fun getState_whenFilteredForSpecificType_onlyEmitsMatchingStates() =
        runTest(UnconfinedTestDispatcher()) {
            val initialHomeInfo = HomeScreenInfo("initial")
            val homeCallback = TestNavigationEventCallback(currentInfo = HomeScreenInfo("home"))
            val detailsCallback =
                TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))
            val collectedStates = mutableListOf<NavigationEventState<HomeScreenInfo>>()

            dispatcher
                .getState(backgroundScope, initialHomeInfo)
                .onEach { collectedStates.add(it) }
                .launchIn(backgroundScope)
            advanceUntilIdle()

            // The flow must start with the initial value provided.
            assertThat(collectedStates).hasSize(1)
            assertThat(collectedStates.last()).isEqualTo(Idle(initialHomeInfo))

            // A new state with a matching type should be collected.
            dispatcher.addCallback(homeCallback)
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)
            assertThat(collectedStates.last()).isEqualTo(Idle(HomeScreenInfo("home")))

            // A state with a non-matching type should be filtered out and not collected.
            dispatcher.addCallback(detailsCallback)
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)

            // When the active callback is removed, since a non-matching type should be filtered out
            // and not collected.
            detailsCallback.remove()
            advanceUntilIdle()
            assertThat(collectedStates).hasSize(2)
            assertThat(collectedStates.last()).isEqualTo(Idle(HomeScreenInfo("home")))
        }

    @Test
    fun getState_whenTypeDoesNotMatch_emitsOnlyInitialInfo() =
        runTest(UnconfinedTestDispatcher()) {
            val initialHomeInfo = HomeScreenInfo("initial")
            val detailsCallback =
                TestNavigationEventCallback(currentInfo = DetailsScreenInfo("details"))
            val collectedStates = mutableListOf<NavigationEventState<HomeScreenInfo>>()

            dispatcher
                .getState(backgroundScope, initialHomeInfo)
                .onEach { collectedStates.add(it) }
                .launchIn(backgroundScope)
            advanceUntilIdle()

            // The flow must start with its initial value.
            assertThat(collectedStates).hasSize(1)
            assertThat(collectedStates.first()).isEqualTo(Idle(initialHomeInfo))

            // Add a callback with a non-matching type.
            dispatcher.addCallback(detailsCallback)
            advanceUntilIdle()

            // The collector should not have emitted a new value.
            assertThat(collectedStates).hasSize(1)

            // Update the non-matching callback's info.
            detailsCallback.setInfo(
                currentInfo = DetailsScreenInfo("details-updated"),
                previousInfo = null,
            )
            advanceUntilIdle()

            // The collector should still not have emitted a new value.
            assertThat(collectedStates).hasSize(1)
        }

    @Test
    fun progress_whenIdleOrInProgress_returnsCorrectValue() {
        val callbackInfo = HomeScreenInfo("home")
        val callback = TestNavigationEventCallback(currentInfo = callbackInfo)
        dispatcher.addCallback(callback)

        // Before any gesture, the state is Idle and progress should be 0.
        assertThat(dispatcher.state.value.progress).isEqualTo(0f)

        // Start a gesture.
        input.handleOnStarted(NavigationEvent(progress = 0.1f))
        assertThat(dispatcher.state.value.progress).isEqualTo(0.1f)

        // InProgress state should reflect the event's progress.
        input.handleOnProgressed(NavigationEvent(progress = 0.5f))
        assertThat(dispatcher.state.value.progress).isEqualTo(0.5f)

        // Complete the gesture.
        input.handleOnCompleted()

        // After the gesture, the state is Idle again and progress should be 0.
        assertThat(dispatcher.state.value.progress).isEqualTo(0f)
    }

    // endregion
}

/** A sealed interface for type-safe navigation information. */
sealed interface TestInfo : NavigationEventInfo

data class HomeScreenInfo(val id: String) : TestInfo

data class DetailsScreenInfo(val id: String) : TestInfo

/**
 * A test implementation of [NavigationEventInput] that records lifecycle events and invocation
 * counts.
 *
 * Use this class in tests to verify that `onAdded`, `onRemoved`, and `onHasEnabledCallbacksChanged`
 * are called correctly. It counts how many times each lifecycle method is invoked and stores a
 * reference to the most recently added dispatcher. It also provides helper methods to simulate
 * dispatching navigation events.
 *
 * @param onAdded An optional lambda to execute when [onAdded] is called.
 * @param onRemoved An optional lambda to execute when [onRemoved] is called.
 * @param onHasEnabledCallbacksChanged An optional lambda to execute when
 *   [onHasEnabledCallbacksChanged] is called.
 */
private class TestNavigationEventInput(
    private val onAdded: (dispatcher: NavigationEventDispatcher) -> Unit = {},
    private val onRemoved: () -> Unit = {},
    private val onHasEnabledCallbacksChanged: (hasEnabledCallbacks: Boolean) -> Unit = {},
) : NavigationEventInput() {

    /** The number of times [onAdded] has been invoked. */
    var addedInvocations: Int = 0
        private set

    /** The number of times [onRemoved] has been invoked. */
    var removedInvocations: Int = 0
        private set

    /** The number of times [onHasEnabledCallbacksChanged] has been invoked. */
    var onHasEnabledCallbacksChangedInvocations: Int = 0
        private set

    /**
     * The most recently added [NavigationEventDispatcher].
     *
     * This is set by [onAdded] and cleared to `null` by [onRemoved].
     */
    var currentDispatcher: NavigationEventDispatcher? = null
        private set

    /**
     * Test helper to simulate the start of a navigation event.
     *
     * This directly calls `dispatchOnStarted`, notifying any registered callbacks. Use this to
     * trigger the beginning of a navigation flow in your tests.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun handleOnStarted(event: NavigationEvent = NavigationEvent()) {
        dispatchOnStarted(event)
    }

    /**
     * Test helper to simulate the progress of a navigation event.
     *
     * This directly calls `dispatchOnProgressed`, notifying any registered callbacks.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    fun handleOnProgressed(event: NavigationEvent = NavigationEvent()) {
        dispatchOnProgressed(event)
    }

    /**
     * Test helper to simulate the completion of a navigation event.
     *
     * This directly calls `dispatchOnCompleted`, notifying any registered callbacks.
     */
    @MainThread
    fun handleOnCompleted() {
        dispatchOnCompleted()
    }

    /**
     * Test helper to simulate the cancellation of a navigation event.
     *
     * This directly calls `dispatchOnCancelled`, notifying any registered callbacks.
     */
    @MainThread
    fun handleOnCancelled() {
        dispatchOnCancelled()
    }

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        addedInvocations++
        currentDispatcher = dispatcher
        onAdded.invoke(dispatcher)
    }

    override fun onRemoved() {
        currentDispatcher = null
        removedInvocations++
        onRemoved.invoke()
    }

    override fun onHasEnabledCallbacksChanged(hasEnabledCallbacks: Boolean) {
        onHasEnabledCallbacksChangedInvocations++
        onHasEnabledCallbacksChanged.invoke(hasEnabledCallbacks)
    }
}
