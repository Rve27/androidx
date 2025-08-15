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

    // region Hierarchy APIs

    @Test
    fun init_whenChildIsCreatedWithParent_thenCallbacksAreSharedAndDispatched() {
        // Given a parent dispatcher and a callback for it
        val parentDispatcher = NavigationEventDispatcher()
        val parentCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)

        // When a child dispatcher is created with the parent and a callback is added to the child
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val childCallback = TestNavigationEventCallback()
        childDispatcher.addCallback(childCallback)

        // Then, dispatching an event from the parent should also trigger the child's callback,
        // indicating the shared processing.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.handleOnStarted(event)

        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Assuming LIFO, parent callback is skipped
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun init_whenChildIsCreatedWithNoParent_thenCallbacksAreIndependent() {
        // Given a parent dispatcher and a child dispatcher created without a parent
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher()

        // And a callback for each
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When an event is dispatched through the parent
        val event = NavigationEvent()
        val parentInput = TestNavigationEventInput()
        parentDispatcher.addInput(parentInput)
        parentInput.handleOnStarted(event)

        // Then only the parent's callback should be invoked, showing independent processing.
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        // When an event is dispatched through the child
        val childInput = TestNavigationEventInput()
        childDispatcher.addInput(childInput)
        childInput.handleOnStarted(event)

        // Then only the child's callback should be invoked, showing independent processing.
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenCalledOnChild_thenCallbackIsDispatchedViaParent() {
        // Given a parent and child dispatcher
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val callback = TestNavigationEventCallback()

        // When a new callback is added to the child
        childDispatcher.addCallback(callback)

        // Then dispatching an event from the parent should trigger the child's callback
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.handleOnStarted(event)
        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenAddedToParentThenChild_thenCallbacksAreOrderedLIFO() {
        // Given a parent and child dispatcher
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        // When a callback is added to the parent, then to the child
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // Then when an event is dispatched, the last-added callback (child's) should be invoked
        // first.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.handleOnStarted(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenMultipleDispatchersAndCallbacksAdded_thenLastAddedCallbackIsInvokedFirst() {
        // Given a parent NavigationEventDispatcher and two child dispatchers.
        val parentDispatcher = NavigationEventDispatcher()
        val child1Dispatcher = NavigationEventDispatcher(parentDispatcher)
        val child2Dispatcher = NavigationEventDispatcher(parentDispatcher)

        // And three TestNavigationCallbacks: one for the parent and one for each child.
        val parentCallback = TestNavigationEventCallback()
        val childCallback1 = TestNavigationEventCallback()
        val childCallback2 = TestNavigationEventCallback()

        // When callbacks are added to the parent, then child2, then child1.
        parentDispatcher.addCallback(parentCallback)
        child2Dispatcher.addCallback(childCallback2)
        child1Dispatcher.addCallback(childCallback1)

        // Then, when an event is dispatched through the parent, only the most recently added active
        // callback (callbackC1 from child1) receives the event. This demonstrates that callbacks
        // are processed in a LIFO manner across the dispatcher hierarchy and that subsequent
        // callbacks are not invoked if an earlier one does not pass through.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.handleOnStarted(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback2.startedInvocations).isEqualTo(0)
        assertThat(childCallback1.startedInvocations).isEqualTo(1)
    }

    @Test
    fun dispose_whenCalledOnChild_thenParentCallbackStillReceivesEvents() {
        // Given a parent and child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When child is disposed
        childDispatcher.dispose()

        // Then dispatching an event from the parent should only trigger the parent's callback
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.handleOnStarted(event)
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun dispose_whenCalledOnParent_cascadesAndThrowsExceptionOnUse() {
        // Given a parent and child dispatcher
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)

        // When the parent is disposed
        parentDispatcher.dispose()

        // Then attempting to use either dispatcher throws an exception
        val event = NavigationEvent()
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                parentDispatcher.addInput(input)
                input.handleOnStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                childDispatcher.addInput(input)
                input.handleOnStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun dispose_whenCalledOnGrandparent_cascadesAndThrowsExceptionOnUse() {
        // Given a three-level dispatcher hierarchy
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)

        // When the grandparent is disposed
        grandparentDispatcher.dispose()

        // Then attempting to use any dispatcher in the hierarchy throws an exception
        val event = NavigationEvent()
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                grandparentDispatcher.addInput(input)
                input.handleOnStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                parentDispatcher.addInput(input)
                input.handleOnStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                childDispatcher.addInput(input)
                input.handleOnStarted(event)
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun isEnabled_whenSetToTrue_thenEventsAreDispatched() {
        // Given a dispatcher
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // When the dispatcher is enabled
        dispatcher.isEnabled = true

        // Then dispatching an event should trigger the callback
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(event)
        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_whenSetToFalse_thenNoCallbacksAreDispatched() {
        // Given a dispatcher with an enabled callback
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(isEnabled = true)
        dispatcher.addCallback(callback)

        // When the dispatcher is disabled
        dispatcher.isEnabled = false

        // Then dispatching an event should not trigger the callback
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(event)
        assertThat(callback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_whenParentIsDisabled_thenChildDoesNotDispatchEvents() {
        // Given a parent and child dispatcher, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When the parent is disabled
        parentDispatcher.isEnabled = false

        // Then dispatching an event from the child should not invoke any callbacks,
        // because the parent's disabled state propagates.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.handleOnStarted(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_whenChildIsLocallyDisabled_thenChildDoesNotDispatchEvents() {
        // Given a parent (enabled) and child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When child is locally disabled
        childDispatcher.isEnabled = false

        // Then dispatching an event from the child should not trigger its callback.
        // The parent's callback should still be invokable via the parent directly.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.handleOnStarted(event)

        assertThat(childCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Parent's callback should still fire via parent
    }

    @Test
    fun isEnabled_whenChildIsLocallyDisabled_thenChildCallbacksDoesNotReceiveEvents() {
        // Given a parent (enabled) and child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When child is locally disabled
        childDispatcher.isEnabled = false

        // Then dispatching an event from the child should not trigger its callback.
        // The parent's callback should still be invokable via the parent directly.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.handleOnStarted(event) // Confirm parent is still active

        assertThat(childCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(1) // Parent's callback should still fire via parent
    }

    @Test
    fun isEnabled_whenDisabledParentIsReEnabled_thenChildDispatchesEventsAgain() {
        // Given a disabled parent and a locally-enabled child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        parentDispatcher.isEnabled = false // Initial state: parent (and thus child) disabled
        // Verify pre-condition (no dispatch before re-enabling)
        val initialEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.handleOnStarted(initialEvent)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        // When the parent is re-enabled
        parentDispatcher.isEnabled = true

        // Then the child should now dispatch events
        val reEnabledEvent = NavigationEvent()
        input.handleOnStarted(reEnabledEvent)

        assertThat(childCallback.startedInvocations).isEqualTo(1)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Parent's callback is still LIFO behind child
    }

    @Test
    fun isEnabled_whenDisabledParentIsReEnabled_thenChildCallbacksReceiveEventsAgain() {
        // Given a disabled parent and a locally-enabled child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        parentDispatcher.isEnabled = false // Initial state: parent (and thus child) disabled
        // Verify pre-condition (no dispatch before re-enabling)
        val initialEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        parentDispatcher.addInput(input)
        input.handleOnStarted(initialEvent)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        // When the parent is re-enabled
        parentDispatcher.isEnabled = true

        // Then the child should now dispatch events
        val reEnabledEvent = NavigationEvent()
        input.handleOnStarted(reEnabledEvent)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Parent's callback is still LIFO behind child
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_whenGrandparentIsDisabled_thenGrandchildDoesNotDispatchEvents() {
        // Given a three-level hierarchy, each with a callback
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentCallback = TestNavigationEventCallback()
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        grandparentDispatcher.addCallback(grandparentCallback)
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When the grandparent is disabled
        grandparentDispatcher.isEnabled = false

        // Then dispatching an event from the grandchild should result in no callbacks being
        // invoked, as the disabled state cascades down.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        childDispatcher.addInput(input)
        input.handleOnStarted(event)

        assertThat(grandparentCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_whenGrandparentIsDisabled_thenGrandchildCallbackDoesNotReceiveEvents() {
        // Given a three-level hierarchy, each with a callback
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentCallback = TestNavigationEventCallback()
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        grandparentDispatcher.addCallback(grandparentCallback)
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When the grandparent is disabled
        grandparentDispatcher.isEnabled = false

        // Then dispatching an event from the grandparent should result in no callbacks being
        // invoked, as the disabled state cascades down.
        val event = NavigationEvent()
        val input = TestNavigationEventInput()
        grandparentDispatcher.addInput(input)
        input.handleOnStarted(event)

        assertThat(grandparentCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun callbackIsEnabled_whenItsDispatcherIsDisabled_thenCallbackDoesNotReceiveEvents() {
        // Given a dispatcher and an enabled TestNavigationCallback
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)
        // Ensure callback is initially enabled
        val preDisableEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(preDisableEvent)
        assertThat(callback.startedInvocations).isEqualTo(1)

        // When the dispatcher associated with the callback is disabled
        dispatcher.isEnabled = false

        // Then dispatching an event (even if the callback's local isEnabled is true)
        // should not trigger the callback because its dispatcher is disabled.
        val event = NavigationEvent()
        input.handleOnStarted(event)

        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun callbackIsEnabled_whenItsDispatcherIsReEnabled_thenCallbackReceivesEventsAgain() {
        // Given a dispatcher and an enabled TestNavigationCallback, and the dispatcher is disabled
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)
        dispatcher.isEnabled = false // Disable dispatcher

        // Pre-condition: Callback does not receive events when dispatcher is disabled
        val preEnableEvent = NavigationEvent()
        val input = TestNavigationEventInput()
        dispatcher.addInput(input)
        input.handleOnStarted(preEnableEvent)
        assertThat(callback.startedInvocations).isEqualTo(0)

        // When the dispatcher associated with the callback is re-enabled
        dispatcher.isEnabled = true

        // Then dispatching an event should now trigger the callback
        val reEnabledEvent = NavigationEvent()
        input.handleOnStarted(reEnabledEvent)

        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        // Adding a callback to a disposed dispatcher should fail.
        assertThrows<IllegalStateException> {
                dispatcher.addCallback(TestNavigationEventCallback())
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnStarted_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        // Dispatching on a disposed dispatcher should fail.
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.handleOnStarted(NavigationEvent())
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnProgressed_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        // Dispatching on a disposed dispatcher should fail.
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.handleOnProgressed(NavigationEvent())
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnCompleted_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        // Dispatching on a disposed dispatcher should fail.
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.handleOnCompleted()
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun handleOnCancelled_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose()

        // Dispatching on a disposed dispatcher should fail.
        assertThrows<IllegalStateException> {
                val input = TestNavigationEventInput()
                dispatcher.addInput(input)
                input.handleOnCancelled()
            }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun dispose_onDisposedDispatcher_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose() // First disposal is fine.

        // Disposing an already-disposed dispatcher should fail.
        assertThrows<IllegalStateException> { dispatcher.dispose() }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun dispose_enabled_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose() // First disposal is fine.

        // Enabling an already-disposed dispatcher should fail.
        assertThrows<IllegalStateException> { dispatcher.isEnabled = false }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    @Test
    fun dispose_disabled_throwsException() {
        val dispatcher = NavigationEventDispatcher()
        dispatcher.dispose() // First disposal is fine.

        // disabling an already-disposed dispatcher should fail.
        assertThrows<IllegalStateException> { dispatcher.isEnabled = false }
            .hasMessageThat()
            .contains("has already been disposed")
    }

    // endregion Hierarchy APIs
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
