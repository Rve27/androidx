/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.RevealActionType.Companion.PrimaryAction
import androidx.wear.compose.material3.RevealActionType.Companion.SecondaryAction
import androidx.wear.compose.material3.RevealActionType.Companion.UndoAction
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealDirection.Companion.RightToLeft
import androidx.wear.compose.material3.RevealValue.Companion.Covered
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealed
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealing
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealed
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealing
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class SwipeToRevealTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun swipeToReveal_undoPrimaryActionDisplayed_performsHaptics() {
        val results = mutableMapOf<HapticFeedbackType, Int>()
        val haptics = hapticFeedback(collectResultsFromHapticFeedback(results))
        val revealStateFlow = MutableStateFlow(RightRevealing)

        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                val revealState by revealStateFlow.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    SwipeToReveal(
                        primaryAction = {
                            PrimaryActionButton(
                                onClick = {},
                                { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                                { Text("Clear") }
                            )
                        },
                        onFullSwipe = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        revealState =
                            rememberRevealState(
                                initialValue = revealState,
                            ),
                    ) {
                        Button({}, Modifier.fillMaxWidth()) {
                            Text("This text should be partially visible.")
                        }
                    }
                }
            }
        }

        rule.runOnIdle { assertThat(results).isEmpty() }

        revealStateFlow.value = RightRevealed

        rule.runOnIdle {
            assertThat(results).hasSize(1)
            assertThat(results).containsKey(HapticFeedbackType.GestureThresholdActivate)
            assertThat(results[HapticFeedbackType.GestureThresholdActivate]).isEqualTo(1)
        }
    }

    @Test
    fun supports_testTag() {
        rule.setContent { SwipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onStartWithDefaultState_keepsContentToRight() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                content = { GetBoxContent(modifier = Modifier.testTag(TEST_TAG)) }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun onZeroOffset_doesNotDrawActions() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = { ActionContent(modifier = Modifier.testTag(TEST_TAG)) }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun onRevealing_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = { ActionContent(modifier = Modifier.testTag(TEST_TAG)) },
                state = rememberRevealState(initialValue = RightRevealing)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onSwipe_drawsAction() {
        val s2rTag = "S2RTag"
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(s2rTag),
                primaryAction = { ActionContent(modifier = Modifier.testTag(TEST_TAG)) }
            )
        }

        rule.onNodeWithTag(s2rTag).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = -(centerX / 4), y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun noSwipe_onFullSwipeRight() {
        var onFullSwipeTriggered = false

        verifyGesture(
            revealValue = Covered,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeRight() }
        )

        assertEquals(false, onFullSwipeTriggered)
    }

    @Test
    fun stateToSwiped_onFullSwipeLeft() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = RightRevealed,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeLeft() }
        )

        assertEquals(true, onFullSwipeTriggered)
    }

    @Test
    fun stateToSwiped_onFullSwipeRight() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = LeftRevealed,
            onFullSwipe = { onFullSwipeTriggered = true },
            revealDirection = Bidirectional,
            gesture = { swipeRight() }
        )

        assertEquals(true, onFullSwipeTriggered)
    }

    @Test
    fun stateToRevealing_onAboveVelocityThresholdSmallDistanceSwipe() {
        verifyGesture(
            revealValue = RightRevealing,
            gesture = { swipeLeft(endX = right - 65, durationMillis = 30L) }
        )
    }

    @Test
    fun noSwipe_onBelowVelocityThresholdSmallDistanceSwipe() {
        verifyGesture(
            revealValue = Covered,
            gesture = { swipeLeft(endX = right - 65, durationMillis = 1000L) }
        )
    }

    @Ignore("b/382204839")
    @Test
    fun stateToRevealing_onAboveVelocityThresholdLongDistanceSwipe() {
        verifyGesture(
            revealValue = RightRevealing,
            gesture = { swipeLeft(endX = right - 300, durationMillis = 100L) }
        )
    }

    @Ignore("b/382204839")
    @Test
    fun stateToRevealing_onBelowVelocityThresholdLongDistanceSwipe() {
        verifyGesture(
            revealValue = RightRevealing,
            gesture = { swipeLeft(endX = right - 300, durationMillis = 1000L) }
        )
    }

    @Test
    fun noSwipe_singleDirectionSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = Covered,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = false,
        )

        assertFalse(onFullSwipeTriggered)
    }

    @Test
    fun noSwipe_bothDirectionsSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = Covered,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeRight() },
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        )

        assertFalse(onFullSwipeTriggered)
    }

    @Ignore("b/382204839")
    @Test
    fun stateToSwiped_bothDirectionsSwipeOnTheEdgeDisabled_onPartialSwipeRight() {
        verifyGesture(
            revealValue = LeftRevealing,
            gesture = { swipeRight(startX = width / 2f, endX = width.toFloat()) },
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        )
    }

    @Test
    fun navigationSwipe_singleDirectionSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onSwipeToDismissBoxDismissed = false
        verifyGesture(
            revealValue = Covered,
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = false,
            wrappedInSwipeToDismissBox = true,
        ) {
            onSwipeToDismissBoxDismissed = true
        }

        assertTrue(onSwipeToDismissBoxDismissed)
    }

    @Test
    fun stateToCovered_singleDirectionRevealingSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onSwipeToDismissBoxDismissed = false

        verifyGesture(
            initialValue = RightRevealing,
            revealValue = Covered,
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = false,
            wrappedInSwipeToDismissBox = true,
        ) {
            onSwipeToDismissBoxDismissed = true
        }

        assertFalse(onSwipeToDismissBoxDismissed)
    }

    @Ignore("b/382204839")
    @Test
    fun stateToCovered_singleDirectionRevealingSwipeOnTheEdgeDisabled_onPartialSwipeRight() {
        var onSwipeToDismissBoxDismissed = false

        verifyGesture(
            initialValue = RightRevealing,
            revealValue = Covered,
            gesture = { swipeRight(startX = width / 2f, endX = width.toFloat()) },
            bidirectionalGestureInclusion = false,
            wrappedInSwipeToDismissBox = true,
        ) {
            onSwipeToDismissBoxDismissed = true
        }

        assertFalse(onSwipeToDismissBoxDismissed)
    }

    @Ignore("b/382204839")
    @Test
    fun stateToCovered_singleDirectionRevealing_onFullSwipeRight() {
        var onSwipeToDismissBoxDismissed = false

        verifyGesture(
            initialValue = RightRevealing,
            revealValue = Covered,
            gesture = { swipeRight(durationMillis = 500L) },
            wrappedInSwipeToDismissBox = true,
        ) {
            onSwipeToDismissBoxDismissed = true
        }

        assertFalse(onSwipeToDismissBoxDismissed)
    }

    @Test
    fun stateToIconsVisible_onPartialSwipeLeft() {
        verifyGesture(revealValue = RightRevealing, gesture = { swipeLeftToRevealing() })
    }

    @Test
    fun onMultiSwipe_whenAllowed_resetsLastState() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            Column {
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(testTagOne),
                    state = revealStateOne
                )
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(testTagTwo),
                    state = revealStateTwo
                )
            }
        }

        // swipe the first S2R to Revealing state
        rule.onNodeWithTag(testTagOne).performTouchInput { swipeLeftToRevealing() }

        // swipe the second S2R to Revealing state
        rule.onNodeWithTag(testTagTwo).performTouchInput { swipeLeftToRevealing() }

        rule.runOnIdle {
            assertEquals(Covered, revealStateOne.currentValue)
            assertEquals(RightRevealing, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onMultiSwipe_whenLastStateRevealed_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            Column {
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(testTagOne),
                    state = revealStateOne
                )
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(testTagTwo),
                    state = revealStateTwo
                )
            }
        }

        // swipe the first S2R to Revealed (full screen swipe)
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeLeft(startX = width.toFloat(), endX = 0f)
        }

        // swipe the second S2R to Revealing state
        rule.onNodeWithTag(testTagTwo).performTouchInput { swipeLeftToRevealing() }

        rule.runOnIdle {
            // assert that state does not reset
            assertEquals(RightRevealed, revealStateOne.currentValue)
            assertEquals(RightRevealing, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onSnapForDifferentStates_lastOneGetsReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            SwipeToRevealWithDefaults(state = revealStateOne)
            SwipeToRevealWithDefaults(state = revealStateTwo)

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                // First change
                revealStateOne.snapTo(RightRevealing)
                // Second change, in a different state
                revealStateTwo.snapTo(RightRevealing)
            }
        }

        rule.runOnIdle { assertEquals(Covered, revealStateOne.currentValue) }
    }

    @Test
    fun onMultiSnapOnSameState_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val lastValue = RightRevealed
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            SwipeToRevealWithDefaults(state = revealStateOne)
            SwipeToRevealWithDefaults(state = revealStateTwo)

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                revealStateOne.snapTo(RightRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same state
            }
        }

        rule.runOnIdle { assertEquals(lastValue, revealStateOne.currentValue) }
    }

    @Test
    fun onSecondaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = SecondaryAction,
            initialRevealValue = RightRevealing,
            secondaryActionModifier = Modifier.testTag(TEST_TAG)
        )

    @Test
    fun onPrimaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = PrimaryAction,
            initialRevealValue = RightRevealing,
            primaryActionModifier = Modifier.testTag(TEST_TAG)
        )

    @Test
    fun onUndoActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = UndoAction,
            initialRevealValue = RightRevealed,
            undoActionModifier = Modifier.testTag(TEST_TAG)
        )

    @Test
    fun onRightSwipe_dispatchEventsToParent() {
        var onPreScrollDispatch = 0f
        rule.setContent {
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        onPreScrollDispatch = available.x
                        return available
                    }
                }
            }
            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                SwipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }

        assert(onPreScrollDispatch > 0)
    }

    @Test
    fun onLeftSwipe_dispatchEventsToParent() {
        var onPreScrollDispatch = 0f
        rule.setContent {
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        onPreScrollDispatch = available.x
                        return available
                    }
                }
            }
            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                SwipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }

        assert(onPreScrollDispatch < 0) // Swiping left means the dispatch will be negative
    }

    private fun verifyLastClickAction(
        expectedClickType: RevealActionType,
        initialRevealValue: RevealValue,
        primaryActionModifier: Modifier = Modifier,
        secondaryActionModifier: Modifier = Modifier,
        undoActionModifier: Modifier = Modifier,
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(initialRevealValue)
            val coroutineScope = rememberCoroutineScope()
            SwipeToRevealWithDefaults(
                primaryAction = {
                    ActionContent(
                        modifier =
                            primaryActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.snapTo(Covered)
                                    revealState.lastActionType = PrimaryAction
                                }
                            }
                    )
                },
                state = revealState,
                secondaryAction = {
                    ActionContent(
                        modifier =
                            secondaryActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.snapTo(Covered)
                                    revealState.lastActionType = SecondaryAction
                                }
                            }
                    )
                },
                undoAction = {
                    ActionContent(
                        modifier =
                            undoActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.animateTo(Covered)
                                    revealState.lastActionType = UndoAction
                                }
                            }
                    )
                }
            )
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle { assertEquals(expectedClickType, revealState.lastActionType) }
    }

    private fun verifyGesture(
        initialValue: RevealValue = Covered,
        revealValue: RevealValue,
        gesture: TouchInjectionScope.() -> Unit,
        onFullSwipe: () -> Unit = {},
        revealDirection: RevealDirection = RightToLeft,
        bidirectionalGestureInclusion: Boolean = true,
        wrappedInSwipeToDismissBox: Boolean = false,
        onSwipeToDismissBoxDismissed: () -> Unit = {},
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState =
                rememberRevealState(
                    initialValue = initialValue,
                )
            if (!wrappedInSwipeToDismissBox) {
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    state = revealState,
                    onFullSwipe = onFullSwipe,
                    revealDirection = revealDirection,
                    bidirectionalGestureInclusion = bidirectionalGestureInclusion,
                )
            } else {
                BasicSwipeToDismissBox(
                    onDismissed = onSwipeToDismissBoxDismissed,
                    state = rememberSwipeToDismissBoxState(),
                ) { isBackground ->
                    if (isBackground) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Red),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            SwipeToRevealWithDefaults(
                                modifier = Modifier.testTag(TEST_TAG),
                                state = revealState,
                                onFullSwipe = onFullSwipe,
                                revealDirection = revealDirection,
                                bidirectionalGestureInclusion = bidirectionalGestureInclusion,
                            )
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(gesture)

        rule.runOnIdle { assertEquals(revealValue, revealState.currentValue) }
    }

    @Composable
    private fun SwipeToRevealWithDefaults(
        modifier: Modifier = Modifier,
        primaryAction: @Composable () -> Unit = { GetAction() },
        state: RevealState = rememberRevealState(),
        revealDirection: RevealDirection = RightToLeft,
        secondaryAction: (@Composable () -> Unit)? = null,
        undoAction: (@Composable () -> Unit)? = null,
        onFullSwipe: () -> Unit = {},
        bidirectionalGestureInclusion: Boolean = true,
        content: @Composable () -> Unit = { GetBoxContent() }
    ) {
        @SuppressLint("PrimitiveInCollection")
        val anchors: Set<RevealValue> =
            remember(revealDirection) {
                if (revealDirection == Bidirectional) {
                    setOf(LeftRevealed, LeftRevealing, Covered, RightRevealing, RightRevealed)
                } else {
                    setOf(Covered, RightRevealing, RightRevealed)
                }
            }

        SwipeToRevealImpl(
            primaryAction = primaryAction,
            anchors = anchors,
            modifier = modifier,
            state = state,
            revealDirection = revealDirection,
            secondaryAction = secondaryAction,
            undoAction = undoAction,
            onFullSwipe = onFullSwipe,
            gestureInclusion =
                if (bidirectionalGestureInclusion) {
                    SwipeToRevealDefaults.bidirectionalGestureInclusion
                } else {
                    SwipeToRevealDefaults.gestureInclusion(state)
                },
            content = content
        )
    }

    @Composable
    private fun GetBoxContent(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
        Box(modifier = modifier.size(width = 200.dp, height = 50.dp).clickable { onClick() }) {}
    }

    @Composable
    private fun ActionContent(modifier: Modifier = Modifier) {
        Box(modifier = modifier.size(50.dp)) {}
    }

    @Composable
    private fun GetAction(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        content: @Composable () -> Unit = { ActionContent(modifier) }
    ) {
        Box(modifier = modifier.clickable { onClick() }) { content() }
    }

    private fun TouchInjectionScope.swipeLeftToRevealing() {
        swipeLeft(
            startX = REVEALING_SINGLE_ACTION_SWIPE_DISTANCE_PX,
            endX = 0f,
            durationMillis = 1_000L
        )
    }

    companion object {
        private const val REVEALING_SINGLE_ACTION_SWIPE_DISTANCE_PX = 70f
    }
}
