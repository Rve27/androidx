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

package androidx.xr.glimmer.stack

import android.os.Build
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.createGlimmerRule
import androidx.xr.glimmer.performIndirectMove
import androidx.xr.glimmer.performIndirectPress
import androidx.xr.glimmer.performIndirectRelease
import androidx.xr.glimmer.performIndirectSwipe
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class VerticalStackTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun zeroItems_displaysNothing() {
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.testTag("stack"), state = state) {
                // No items
            }
        }

        assertThat(rule.onNodeWithTag("stack").getBoundsInRoot().size).isEqualTo(DpSize.Zero)
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun singleItem_displaysItem() {
        val state = StackState()
        rule.setContent { VerticalStack(state = state) { item { Text("Single Item") } } }

        rule.onNodeWithText("Single Item").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_displaysFirstThreeItems() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        rule.onNodeWithText("Item 5").assertDoesNotExist()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_withKeys_displaysFirstThreeItems() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item(key = 0) { StackItem("Item 0") }
                item(key = 1) { StackItem("Item 1") }
                item(key = 2) { StackItem("Item 2") }
                item(key = 3) { StackItem("Item 3") }
            }
        }

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_withNullAndIntKey_displaysFirstThreeItems() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item(key = null) { StackItem("Item 0") }
                item(key = 0) { StackItem("Item 1") }
                item(key = 1) { StackItem("Item 2") }
                item(key = 3) { StackItem("Item 3") }
            }
        }

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_customInitialTopItem_displaysRequestedAndNextItems() {
        val state = StackState(initialTopItem = 1)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_lastInitialTopItem_displaysLastItem() {
        val state = StackState(initialTopItem = 4)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 4").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_outOfRangeInitialTopItem_displaysLastItem() {
        val state = StackState(initialTopItem = 10)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        rule.onNodeWithText("Item 4").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun multipleItems_stateChanges_maintainsItemCount() {
        val state1 = StackState()
        val state2 = StackState()
        var state by mutableStateOf(state1)
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }
        rule.runOnIdle { assertThat(state1.itemCount).isEqualTo(5) }

        rule.runOnIdle { state = state2 }

        rule.runOnIdle { assertThat(state2.itemCount).isEqualTo(5) }
    }

    @Test
    fun swipeUp_pointerInput_displaysOnlyNextThreeItems() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        rule.onNodeWithText("Item 0").performTouchInput {
            swipe(
                start = Offset(x = centerX, y = itemHeight.toFloat()),
                end = Offset(x = centerX, y = 0f),
            )
        }

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeForward_displaysOnlyNextThreeItems() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        performIndirectSwipe(itemHeight)

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeForward_almostItemHeight_snapsToNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()

        performIndirectSwipe((itemHeight * 0.9f).toInt())

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun swipeForwardAndBackward_singleGesture_pointerInput_returnsToOriginalPosition() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)

        rule.onRoot().performTouchInput {
            down(Offset(x = centerX, y = itemHeight / 2f))
            moveTo(Offset(x = centerX, y = 0f))
        }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isGreaterThan(0.3f)

        rule.onRoot().performTouchInput { moveTo(Offset(x = centerX, y = itemHeight / 2f)) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)

        rule.onRoot().performTouchInput { up() }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun swipeForwardAndBackward_singleGesture_returnsToOriginalPosition() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)

        val press = performIndirectPress()
        val moveForward =
            performIndirectMove(distancePx = itemHeight / 2f, previousMotionEvent = press)
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isGreaterThan(0.3f)

        val moveBackward =
            performIndirectMove(distancePx = -itemHeight / 2f, previousMotionEvent = moveForward)
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isLessThan(0.1f)

        performIndirectRelease(previousMotionEvent = moveBackward)
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
    }

    @Test
    fun swipeBackward_almostItemHeight_snapsToPreviousItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()

        performIndirectSwipe(-(itemHeight * 0.9f).toInt())

        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun scrollToEndAndBack_displaysItemsInCorrectOrder() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(3) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("Item 0").assertIsDisplayed() // Reached the beginning
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun mixedDsl_displaysItemsInCorrectOrder() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item { StackItem("First") { itemHeight = it } }
                items(3) { StackItem("Middle $it") }
                items(listOf(3, 4, 5)) { StackItem("Middle $it") }
                item { StackItem("Last") }
            }
        }
        rule.onNodeWithText("First").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        rule.onNodeWithText("Middle 0").assertIsDisplayed()
        rule.onNodeWithText("Middle 1").assertIsDisplayed()
        rule.onNodeWithText("Middle 2").assertIsNotDisplayed()

        repeat(6) { index ->
            performIndirectSwipe(itemHeight)

            rule.onNodeWithText("Middle $index").assertIsDisplayed()
            rule.onNodeWithText("Middle ${(index + 1).coerceAtMost(5)}").assertIsDisplayed()
            assertThat(state.topItem).isEqualTo(index + 1)
        }

        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Last").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(7)
    }

    @Test
    fun increaseItemCount_updatesItems() = runTest {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContent {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { itemCount++ }

        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 3").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(3)
    }

    @Test
    fun decreaseItemCount_updatesItems() = runTest {
        var itemHeight = 0
        val state = StackState()
        var itemCount by mutableStateOf(3)
        rule.setContent {
            VerticalStack(state = state) {
                items(itemCount) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 2").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { itemCount-- }

        rule.onNodeWithText("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Item 1").assertIsDisplayed() // Reached the end
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun reorderItems_withKeys_preservesScrollPosition() = runTest {
        var itemHeight = 0
        val state = StackState()
        var items by mutableStateOf(listOf("A", "B", "C", "D"))
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                item(key = "First", content = { StackItem("First") { itemHeight = it } })
                items(items, key = { it }) { StackItem(it) }
                items(1, key = { "Last" }) { StackItem("Last") }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)

        rule.runOnIdle { items = listOf("A", "C", "B", "D") }

        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("D").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        performIndirectSwipe(itemHeight)
        rule.onNodeWithText("Last").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(5)

        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("D").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(4)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("B").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("C").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("A").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)
        performIndirectSwipe(-itemHeight)
        rule.onNodeWithText("First").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun stateRestoration_restoresTopItem() {
        val restorationTester = StateRestorationTester(rule)
        var targetItem by mutableStateOf<Int?>(null)
        lateinit var state: StackState

        restorationTester.setContent {
            state = rememberStackState(initialTopItem = 0)

            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") }
            }

            LaunchedEffect(targetItem) { targetItem?.let { state.scrollToItem(it) } }
        }

        // Verify the initial state
        rule.onNodeWithText("Item 0").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsNotDisplayed()
        assertThat(state.topItem).isEqualTo(0)

        // Scroll to a new item
        rule.runOnIdle { targetItem = 3 }
        rule.waitForIdle()

        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)

        // Simulate recreation
        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        // Verify the restored state
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
    }

    @Test
    fun interactionSource_emitsDragInteractions() {
        var itemHeight = 0
        val state = StackState()
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        val interactions = mutableListOf<Interaction>()
        scope.launch { state.interactionSource.interactions.collect { interactions.add(it) } }
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        performIndirectSwipe(itemHeight)

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions[0]).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
        }
    }

    @Test
    fun alwaysComposesNextTwoItems() = runTest {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }
        rule.onNodeWithText("Item 0").assertExists() // Top item
        rule.onNodeWithText("Item 1").assertExists() // Next item
        rule.onNodeWithText("Item 2").assertExists() // Next next item

        runOnUiThread { state.scrollToItem(2) }

        rule.onNodeWithText("Item 1").assertExists() // Previous item
        rule.onNodeWithText("Item 2").assertExists() // Top item
        rule.onNodeWithText("Item 3").assertExists() // Next item
        rule.onNodeWithText("Item 4").assertExists() // Next next item
    }

    @Test
    fun positioningAndScale_topItem_occupiesViewportExceptForRevealArea() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                items(5) { index -> StackItem("Item $index") }
            }
        }

        val stackBounds = rule.onNodeWithTag("stack").getBoundsInRoot()
        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()

        assertThat(topItemBounds.left).isEqualTo(stackBounds.left)
        assertThat(topItemBounds.top).isEqualTo(stackBounds.top)
        assertThat(topItemBounds.right).isEqualTo(stackBounds.right)
        assertThat(topItemBounds.bottom).isLessThan(stackBounds.bottom)
    }

    @Test
    fun positioningAndScale_nextItem_smallerAndBelowTopItem() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isGreaterThan(topItemBounds.top)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_nextNextItem_smallerAndBelowNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        // Scroll almost fully to the next item to reveal the next-next item.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.dispatchRawDelta(itemHeight * 0.99f) }

        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()
        val nextNextItemBounds = rule.onNodeWithTag("Item 2").getBoundsInRoot()

        assertThat(nextNextItemBounds.left).isGreaterThan(nextItemBounds.left)
        assertThat(nextNextItemBounds.top).isGreaterThan(nextItemBounds.top)
        assertThat(nextNextItemBounds.right).isLessThan(nextItemBounds.right)
        assertThat(nextNextItemBounds.bottom).isGreaterThan(nextItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_scrollBackwardSlightly_threeItemsVisible() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(1) }

        // Scroll backward slightly reveal the previous item.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.dispatchRawDelta(-itemHeight * 0.01f) }

        val stackBounds = rule.onNodeWithTag("stack").getBoundsInRoot()
        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()
        val nextNextItemBounds = rule.onNodeWithTag("Item 2").getBoundsInRoot()

        assertThat(topItemBounds.left).isEqualTo(stackBounds.left)
        assertThat(topItemBounds.top).isEqualTo(stackBounds.top)
        assertThat(topItemBounds.right).isEqualTo(stackBounds.right)
        assertThat(topItemBounds.bottom).isGreaterThan(stackBounds.top)

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isGreaterThan(topItemBounds.top)
        assertThat(nextItemBounds.top).isLessThan(topItemBounds.bottom)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)

        assertThat(nextNextItemBounds.left).isGreaterThan(nextItemBounds.left)
        assertThat(nextNextItemBounds.top).isGreaterThan(nextItemBounds.top)
        assertThat(nextNextItemBounds.top).isLessThan(nextItemBounds.bottom)
        assertThat(nextNextItemBounds.right).isLessThan(nextItemBounds.right)
        assertThat(nextNextItemBounds.bottom).isGreaterThan(nextItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_largerNextItem_nextItemBoundsLargerThanTopItem() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(100.dp)) {
                item { StackItem("Item 0", Modifier.fillMaxWidth().height(10.dp)) }
                item { StackItem("Item 1", Modifier.fillMaxWidth().height(50.dp)) }
            }
        }

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isLessThan(topItemBounds.top)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)
    }

    @Test
    fun positioningAndScale_smallItems_bottomAlignedInViewport() {
        val stackSize = 100.dp
        val itemHeight = 10.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize).testTag("stack")) {
                items(5) { index ->
                    StackItem("Item $index", Modifier.fillMaxWidth().height(itemHeight))
                }
            }
        }

        val stackBounds = rule.onNodeWithTag("stack").getBoundsInRoot()
        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        // Calculate the expected position based on the bottom alignment logic.
        val expectedTopOffset = stackBounds.height - topItemBounds.height - RevealAreaSize
        assertThat(topItemBounds.left).isEqualTo(stackBounds.left)
        assertThat(topItemBounds.top.value).isWithin(1f).of(expectedTopOffset.value)
        assertThat(topItemBounds.right).isEqualTo(stackBounds.right)
        assertThat(topItemBounds.bottom.value)
            .isWithin(1f)
            .of(stackBounds.height.value - RevealAreaSize.value)

        assertThat(nextItemBounds.left).isGreaterThan(topItemBounds.left)
        assertThat(nextItemBounds.top).isGreaterThan(topItemBounds.top)
        assertThat(nextItemBounds.right).isLessThan(topItemBounds.right)
        assertThat(nextItemBounds.bottom).isGreaterThan(topItemBounds.bottom)
    }

    @Test
    fun masking_largerNextItem_clipsNextItemToTopItemShape() {
        val stackSize = 100.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize)) {
                item {
                    StackItem(
                        "Item 0",
                        Modifier.fillMaxWidth()
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red),
                    )
                }
                item {
                    StackItem(
                        "Item 1",
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Green),
                    )
                }
            }
        }

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val topItemTop = with(rule.density) { topItemBounds.top.roundToPx() }
            // The top of the top item is between the top and bottom of the next item
            assertThat(topItemTop).isGreaterThan(0)
            assertThat(topItemTop).isLessThan(pixels.height - 1)

            // The part of the next item above the top item is clipped
            for (x in 0 until pixels.width) {
                for (y in 0 until topItemTop) {
                    assertWithMessage("Pixel at ($x, $y) should not have the next item's color")
                        .that(pixels[x, y].toOpaque())
                        .isNotEqualTo(Color.Green)
                }
            }

            // The bottom of the next item is visible below the top item
            val nextItemBottom = with(rule.density) { nextItemBounds.bottom.roundToPx() }
            val x = (pixels.width / 2)
            val y = nextItemBottom - 1
            val nextItemColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.red)
                .isLessThan(0.2f)
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.green)
                .isGreaterThan(0.3f)
            assertWithMessage("Pixel at ($x, $y) should have the next item's color")
                .that(nextItemColor.blue)
                .isLessThan(0.2f)
        }
    }

    @Test
    fun masking_largerNextNextItem_clipsNextNextItemToNextItemShape() = runTest {
        var topItemHeight = 0
        val stackSize = 100.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.size(stackSize)) {
                item {
                    StackItem(
                        "Item 0",
                        Modifier.fillMaxWidth()
                            .height(80.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red),
                    ) {
                        topItemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        Modifier.fillMaxWidth()
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Green),
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        Modifier.fillMaxWidth()
                            .height(80.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Blue),
                    )
                }
            }
        }

        // Scroll almost fully to the next item to reveal the next-next item.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { state.dispatchRawDelta(topItemHeight * 0.99f) }
        rule.waitForIdle()

        val topItemBounds = rule.onNodeWithTag("Item 0").getBoundsInRoot()
        val nextItemBounds = rule.onNodeWithTag("Item 1").getBoundsInRoot()
        val nextNextItemBounds = rule.onNodeWithTag("Item 2").getBoundsInRoot()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()

            val topItemBottom = with(rule.density) { topItemBounds.bottom.roundToPx() }
            // The bottom of the top item is visible at the top of the next-next item bounds
            assertThat(topItemBottom).isGreaterThan(0)

            val nextItemTop = with(rule.density) { nextItemBounds.top.roundToPx() }
            val nextItemBottom = with(rule.density) { nextItemBounds.bottom.roundToPx() }
            // The top of the next item is between the top and bottom of the next-next item
            assertThat(nextItemTop).isGreaterThan(0)
            assertThat(nextItemTop).isLessThan(nextItemBottom)
            // The top of the next item is below the bottom of the top item
            assertThat(nextItemTop).isGreaterThan(topItemBottom)

            // The part of the next-next item above the next item and below the top item is clipped
            for (x in 0 until pixels.width) {
                for (y in topItemBottom + 1 until nextItemTop) {
                    assertWithMessage(
                            "Pixel at ($x, $y) should not have the next-next item's color"
                        )
                        .that(pixels[x, y].toOpaque())
                        .isNotEqualTo(Color.Blue)
                }
            }

            // The bottom of the next-next item is visible below the next item
            val nextNextItemBottom = with(rule.density) { nextNextItemBounds.bottom.roundToPx() }
            assertThat(nextNextItemBottom).isGreaterThan(nextItemBottom)
            val x = (pixels.width / 2)
            val y = nextNextItemBottom - 1
            val nextNextItemColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.red)
                .isLessThan(0.2f)
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.green)
                .isLessThan(0.2f)
            assertWithMessage("Pixel at ($x, $y) should have the next-next item's color")
                .that(nextNextItemColor.blue)
                .isGreaterThan(0.3f)
        }
    }

    @Test
    fun masking_narrowDecoration_doesNotClip() {
        val narrowDecorationWidth = 50.dp
        val narrowDecorationHeight = 200.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Red, contains a narrow decoration, which does not mask Item 1
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.size(
                                    width = narrowDecorationWidth,
                                    height = narrowDecorationHeight,
                                )
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                    }
                }
                item {
                    // Item 1: Blue, should not be clipped by Item 0.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val narrowDecorationWidthPx = with(rule.density) { narrowDecorationWidth.roundToPx() }
            val narrowDecorationHeightPx = with(rule.density) { narrowDecorationHeight.roundToPx() }
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage("Item 1 outside narrow decoration should not be clipped")
                .that(pixels[pixels.width / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun masking_narrowDecorationBecomeWide_clipsAfterWidening() {
        var widthFraction by mutableStateOf(0.5f)
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Red, contains a narrow decoration initially.
                    Box(
                        Modifier.focusable()
                            .fillMaxWidth(widthFraction)
                            .height(10.dp)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red)
                    )
                }
                item {
                    // Item 1: Blue, should not be clipped by Item 0 initially.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 1 should not be clipped")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Blue)
        }

        rule.runOnIdle { widthFraction = 1f }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 1 should now be clipped")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isNotEqualTo(Color.Blue)
        }
    }

    @Test
    fun masking_decorationHeightDecreases_updatesMask() {
        var heightFraction by mutableStateOf(0.6f)
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Red, contains a large decoration initially.
                    Box(
                        Modifier.focusable()
                            .fillMaxWidth()
                            .fillMaxHeight(heightFraction)
                            .itemDecoration(RectangleShape)
                            .background(Color.Red)
                    )
                }
                item { StackItem("Item 1", Modifier.background(Color.Blue)) }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 0 is in the middle of the stack viewport")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Red)
        }

        rule.runOnIdle { heightFraction = 0.1f }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Item 1 should now be clipped further")
                .that(pixels[pixels.width / 2, (pixels.height * 0.8f).toInt()].toOpaque())
                .isNotIn(listOf(Color.Red, Color.Blue))
        }
    }

    @Test
    fun masking_decorationNodeReused_updatesMask() {
        val stackHeight = 100.dp
        val stackHeightPx = stackHeight.toPx()
        rule.setContent {
            Box(Modifier.background(Color.Red)) {
                VerticalStack(modifier = Modifier.size(stackHeight).testTag("stack")) {
                    items(10) {
                        Box(
                            Modifier.focusable()
                                .fillMaxWidth()
                                .fillMaxHeight(0.1f)
                                .itemDecoration(RectangleShape)
                                .background(Color.Green)
                        )
                    }
                    item {
                        StackItem(
                            "Item 1",
                            Modifier.itemDecoration(RectangleShape).background(Color.Blue),
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("The center pixel should have the outer Box color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Red)
        }

        repeat(9) {
            // Scroll the stack sufficiently to trigger a modifier node reuse.
            performIndirectSwipe(stackHeightPx)
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("The center pixel should have the outer Box color")
                .that(pixels[pixels.width / 2, pixels.height / 2].toOpaque())
                .isEqualTo(Color.Red)
        }
    }

    @Test
    fun masking_multipleDecorations_clipsToWidest() {
        val narrowDecorationWidth = 50.dp
        val narrowDecorationHeight = 200.dp
        val wideDecorationHeight = 10.dp
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Contains two decorations.
                    // 1. Narrow but tall (Red)
                    // 2. Wide (Green)
                    // The clip should align to the Wide one (Green).
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.size(
                                    width = narrowDecorationWidth,
                                    height = narrowDecorationHeight,
                                )
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                        Box(
                            Modifier.padding(top = narrowDecorationHeight)
                                .fillMaxWidth()
                                .height(wideDecorationHeight)
                                .itemDecoration(RectangleShape)
                                .background(Color.Green)
                        )
                    }
                }
                item {
                    // Item 1: Blue, should be clipped by Item 0.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val narrowDecorationWidthPx = with(rule.density) { narrowDecorationWidth.roundToPx() }
            val narrowDecorationHeightPx = with(rule.density) { narrowDecorationHeight.roundToPx() }
            val wideDecorationHeightPx = with(rule.density) { wideDecorationHeight.roundToPx() }
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage(
                    "Item 1 outside narrow decoration but above clip line should be clipped"
                )
                .that(pixels[narrowDecorationWidthPx * 2, narrowDecorationHeightPx / 2].toOpaque())
                .isNotEqualTo(Color.Blue)

            val belowY = with(rule.density) { 50.dp.roundToPx() }
            assertWithMessage("Item 1 should be visible below the clip line)")
                .that(
                    pixels[
                            pixels.width / 2,
                            narrowDecorationHeightPx + wideDecorationHeightPx + belowY]
                        .toOpaque()
                )
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun masking_multipleEqualWidthDecorations_clipsAboveTopMost() {
        val topOffset = 100.dp
        val decorationHeight = 10.dp
        val interDecorationDistance = 100.dp
        val state = StackState()
        rule.setContent {
            Box(Modifier.background(Color.Red)) {
                VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                    item {
                        Column(Modifier.padding(top = topOffset).fillMaxSize().focusable()) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .height(decorationHeight)
                                    .itemDecoration(RectangleShape)
                                    .background(Color.Green)
                            )
                            Spacer(Modifier.height(interDecorationDistance))
                            Box(
                                Modifier.fillMaxWidth()
                                    .height(decorationHeight)
                                    .itemDecoration(RectangleShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Large item", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val offsetX = pixels.width / 2
            val topOffsetPx = topOffset.toPx()
            val decorationHeightPx = decorationHeight.toPx()
            val interDecorationDistancePx = interDecorationDistance.toPx()

            assertWithMessage("Next item is masked above the first decoration")
                .that(pixels[offsetX, topOffsetPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage("Next item is not masked between the two decorations")
                .that(
                    pixels[
                            offsetX,
                            topOffsetPx + decorationHeightPx + interDecorationDistancePx / 2]
                        .toOpaque()
                )
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun masking_removeDecoration_updatesClip() {
        val narrowDecorationWidth = 50.dp
        val narrowDecorationHeight = 200.dp
        val wideDecorationHeight = 10.dp
        val state = StackState()
        var showWideDecoration by mutableStateOf(true)
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    // Item 0: Contains two decorations.
                    // 1. Narrow but tall (Red) -- always present
                    // 2. Wide (Green) -- gets removed
                    // The clip should initially align to the green one and then the red one.
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.size(
                                    width = narrowDecorationWidth,
                                    height = narrowDecorationHeight,
                                )
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                        if (showWideDecoration) {
                            Box(
                                Modifier.padding(top = narrowDecorationHeight)
                                    .fillMaxWidth()
                                    .height(wideDecorationHeight)
                                    .itemDecoration(RectangleShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                }
                item {
                    // Item 1: Blue, should be clipped by Item 0.
                    StackItem("Item 1", Modifier.background(Color.Blue))
                }
            }
        }
        val narrowDecorationWidthPx = with(rule.density) { narrowDecorationWidth.roundToPx() }
        val narrowDecorationHeightPx = with(rule.density) { narrowDecorationHeight.roundToPx() }

        // Initially the clip line is aligned with the top of the wide decoration.
        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage(
                    "Item 1 outside narrow decoration but above the wide decoration should be clipped"
                )
                .that(pixels[narrowDecorationWidthPx * 2, narrowDecorationHeightPx / 2].toOpaque())
                .isNotEqualTo(Color.Blue)
        }

        rule.runOnIdle { showWideDecoration = false }

        // Now the clip line is aligned with the top of the narrow decoration.
        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()

            assertWithMessage("The narrow decoration of Item 0 should be visible")
                .that(pixels[narrowDecorationWidthPx / 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Red)

            assertWithMessage("Item 1 outside narrow decoration should now be visible")
                .that(pixels[narrowDecorationWidthPx * 2, narrowDecorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun masking_changeDecorationOffset_updatesClip() {
        val state = StackState()
        val initialOffset = 200.dp
        val decorationHeight = 10.dp
        val shadowOffset = 50.dp
        var offsetDp by mutableStateOf(initialOffset)
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                item {
                    Box(Modifier.fillMaxSize().focusable()) {
                        Box(
                            Modifier.padding(top = offsetDp)
                                .fillMaxWidth()
                                .height(decorationHeight)
                                .itemDecoration(RectangleShape)
                                .background(Color.Red)
                        )
                    }
                }
                item { StackItem("Item 1", Modifier.background(Color.Green)) }
            }
        }
        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val centerX = pixels.width / 2
            assertWithMessage("Initially clips above the initial offset")
                .that(pixels[centerX, initialOffset.toPx() / 2].toOpaque())
                .isNotIn(listOf(Color.Red, Color.Green))
            assertWithMessage("Item 1 is visible right below Item 0")
                .that(
                    pixels[
                            centerX,
                            initialOffset.toPx() + decorationHeight.toPx() + shadowOffset.toPx()]
                        .toOpaque()
                )
                .isEqualTo(Color.Green)
        }

        rule.runOnIdle { offsetDp = initialOffset + decorationHeight + shadowOffset * 2 }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val centerX = pixels.width / 2
            assertWithMessage("Item 1 is clipped where it was visible before")
                .that(
                    pixels[
                            centerX,
                            initialOffset.toPx() + decorationHeight.toPx() + shadowOffset.toPx()]
                        .toOpaque()
                )
                .isNotIn(listOf(Color.Red, Color.Green))
        }
    }

    @Test
    fun masking_roundedShape_clipsAtTopRadius() {
        val topOffset = 100.dp
        val cornerRadius = 100.dp
        val shape = RoundedCornerShape(cornerRadius)
        val state = StackState()
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                    item {
                        // Item 0: Rounded Rect with the widest point at the 'cornerRadius' Y offset
                        Box(Modifier.fillMaxSize().focusable()) {
                            Box(
                                Modifier.padding(top = topOffset)
                                    .fillMaxWidth()
                                    .height(cornerRadius * 2)
                                    .clip(shape)
                                    .itemDecoration(shape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Item 1", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val topOffsetPx = topOffset.toPx()
            val cornerRadiusPx = cornerRadius.toPx()
            val cornerRadiusLegPx = cornerRadiusPx / sqrt(2f)
            val offsetPx = (cornerRadiusPx - cornerRadiusLegPx).toInt()

            assertWithMessage("Pixels outside of the top-left rounded corner should be clipped")
                .that(pixels[offsetPx, topOffsetPx + offsetPx - 2])
                .isEqualTo(Color.Red)
            assertWithMessage("Pixels inside of the rounded corner should have the Item 0 color")
                .that(pixels[offsetPx, topOffsetPx + offsetPx + 2])
                .isEqualTo(Color.Green)
            assertWithMessage(
                    "Pixels outside of the bottom-left rounded corner should have the Item 1 color"
                )
                .that(pixels[offsetPx, topOffsetPx + cornerRadiusPx * 2 - offsetPx + 2].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun masking_genericShape_selectsWidestPoint() {
        val topOffset = 100.dp
        val decorationHeight = 100.dp
        // Shape definition:
        // - Starts at 60% height (padding at top).
        // - Widest point is at 80% of the total size.
        // - Ends at 100% height.
        // Bounds: top=0.6, bottom=1.0. Height=0.4.
        // Widest point relative to bounds: At 0.2 (which is 0.8 absolute - 0.6 top).
        val shiftedDiamondShape = GenericShape { size, _ ->
            moveTo(size.width / 2f, size.height * 0.6f) // Start 60% down
            lineTo(size.width, size.height * 0.8f) // Widest point at 80%
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height * 0.8f) // Widest point at 80%
            close()
        }

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                VerticalStack(state = StackState(), modifier = Modifier.testTag("stack")) {
                    item {
                        Box(Modifier.fillMaxSize().focusable()) {
                            Box(
                                Modifier.padding(top = topOffset)
                                    .fillMaxWidth()
                                    .height(decorationHeight)
                                    .clip(shiftedDiamondShape)
                                    .itemDecoration(shiftedDiamondShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Item 1", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val topOffsetPx = topOffset.toPx()
            val decorationHeightPx = decorationHeight.toPx()
            val offsetXPx = pixels.width / 4

            assertWithMessage("Pixels above the widest point should be clipped")
                .that(
                    pixels[offsetXPx, (topOffsetPx + decorationHeightPx * 0.5f - 1).toInt()]
                        .toOpaque()
                )
                .isEqualTo(Color.Red)

            assertWithMessage(
                    "Pixels at 70% (between calculated relative height and absolute height) should be clipped"
                )
                .that(
                    pixels[offsetXPx, (topOffsetPx + decorationHeightPx * 0.7f - 1).toInt()]
                        .toOpaque()
                )
                .isEqualTo(Color.Red)

            assertWithMessage("Pixels below the widest point should have Item 1 color")
                .that(
                    pixels[offsetXPx, (topOffsetPx + decorationHeightPx * 0.95f).toInt()].toOpaque()
                )
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun masking_genericShape_picksTopMostWidestPoint() {
        val state = StackState()
        val topOffset = 100.dp
        val decorationHeight = 100.dp
        // Widest points are at 0.3 (top) and 0.7 (bottom).
        // The masking logic should pick the top widest line.
        val indentedRhombusShape = GenericShape { size, _ ->
            apply {
                moveTo(size.width * 0.5f, 0f)
                lineTo(size.width, size.height * 0.3f)
                lineTo(size.width * 0.6f, size.height * 0.5f)
                lineTo(size.width, size.height * 0.7f)
                lineTo(size.width * 0.5f, size.height)
                lineTo(0f, size.height * 0.7f)
                lineTo(size.width * 0.4f, size.height * 0.5f)
                lineTo(0f, size.height * 0.3f)
                close()
            }
        }
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                VerticalStack(state = state, modifier = Modifier.testTag("stack")) {
                    item {
                        Box(Modifier.fillMaxSize().focusable()) {
                            Box(
                                Modifier.padding(top = topOffset)
                                    .fillMaxWidth()
                                    .height(decorationHeight)
                                    .clip(indentedRhombusShape)
                                    .itemDecoration(indentedRhombusShape)
                                    .background(Color.Green)
                            )
                        }
                    }
                    item { StackItem("Item 1", Modifier.background(Color.Blue)) }
                }
            }
        }

        rule.onNodeWithTag("stack").captureToImage().run {
            val pixels = toPixelMap()
            val topOffsetPx = topOffset.toPx()
            val decorationHeightPx = decorationHeight.toPx()
            val offsetXPx = pixels.width / 4
            assertWithMessage("Pixels above the top widest point should be clipped")
                .that(pixels[offsetXPx, topOffsetPx + 5].toOpaque())
                .isEqualTo(Color.Red)
            assertWithMessage("Pixels in the indented area should have Item 1 color")
                .that(pixels[offsetXPx, topOffsetPx + decorationHeightPx / 2].toOpaque())
                .isEqualTo(Color.Blue)
            assertWithMessage("Pixels below the widest bottom point should have Item 1 color")
                .that(pixels[offsetXPx, topOffsetPx + decorationHeightPx - 5].toOpaque())
                .isEqualTo(Color.Blue)
        }
    }

    @Test
    fun initialFocus_largerSecondItem_focusesOnFirstItem() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item { Box(Modifier.focusable().size(10.dp).testTag("Item 0")) }
                item { Box(Modifier.fillMaxSize().focusable().testTag("Item 1")) }
            }
        }

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.onNodeWithTag("Item 1").assertIsNotFocused()
    }

    @Test
    fun focusReenter_afterFocusMove_focusesOnTopItem() = runTest {
        val state = StackState()
        val anotherFocusTargetRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            Column {
                VerticalStack(state = state) {
                    item { StackItem("Item 0") }
                    item { StackItem("Item 1") }
                }
                Box(Modifier.size(100.dp).focusRequester(anotherFocusTargetRequester).focusable())
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsFocused()

        rule.runOnIdle { anotherFocusTargetRequester.requestFocus() }

        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsNotFocused()

        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsFocused()
    }

    @Test
    fun swipeForward_movesFocusForward() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()

            assertThat(item2FocusEvents).isEmpty()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackward_movesFocusBackward() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(2)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
            assertThat(item2FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(-itemHeight)

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(-itemHeight)

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeForwardShortDistance_doesNotMoveFocus() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe((itemHeight * 0.1f).toInt())

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeBackwardShortDistance_doesNotMoveFocus() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(2)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
            assertThat(item1FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe(-(itemHeight * 0.1f).toInt())

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeForwardAlmostItemHeight_movesFocusToNextItem() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe((itemHeight * 0.9f).toInt())

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackwardAlmostItemHeight_movesFocusToPreviousItem() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(2)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
            assertThat(item1FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe(-(itemHeight * 0.9f).toInt())

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }
    }

    @Test
    fun swipeForward_stackIsNotFocused_doesNotMoveFocus() {
        val nonStackFocusRequester = FocusRequester()
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
            Box(modifier = Modifier.focusRequester(nonStackFocusRequester).focusTarget())
        }
        rule.runOnIdle { nonStackFocusRequester.requestFocus() }
        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()

        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsNotFocused()
        rule.onNodeWithTag("Item 2").assertIsNotFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
            assertThat(item2FocusEvents).isEmpty()
        }
    }

    @Test
    fun scrollToItem_movesFocus() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()

        runOnUiThread { state.scrollToItem(2) }

        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).isEmpty()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun edgeScrim_onIndirectPressAndRelease_drawsScrim() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.background(Color.Red)) {
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Green)) }
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Blue)) }
            }
        }
        val x = 0
        val y = 0

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color before press")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }

        val press = performIndirectPress()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val scrimTopPixelColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.red)
                .isGreaterThan(0.9f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.green)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.blue)
                .isZero()
        }

        performIndirectRelease(previousMotionEvent = press)

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color after release")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }
    }

    @Test
    fun edgeScrim_onPointerPressAndRelease_drawsScrim() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state, modifier = Modifier.background(Color.Red)) {
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Green)) }
                item { Box(Modifier.focusable().fillMaxSize().background(Color.Blue)) }
            }
        }
        val x = 0
        val y = 0

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color before press")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }

        rule.onRoot().performTouchInput { down(center) }

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val scrimTopPixelColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.red)
                .isGreaterThan(0.9f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.green)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on press")
                .that(scrimTopPixelColor.blue)
                .isZero()
        }

        rule.onRoot().performTouchInput { up() }

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the item's color after release")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }
    }

    @Test
    fun swipeForward_largeDistance_stopsAtNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        assertThat(state.topItem).isEqualTo(0)

        performIndirectSwipe(itemHeight * 3)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(1)
        rule.onNodeWithText("Item 0").assertIsNotDisplayed()
        rule.onNodeWithText("Item 1").assertIsDisplayed()
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 3").assertIsDisplayed()
        rule.onNodeWithText("Item 4").assertIsNotDisplayed()
    }

    @Test
    fun swipeBackward_largeDistance_stopsAtPreviousItem() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(3) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(3)

        performIndirectSwipe(-itemHeight * 3)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsNotDisplayed()
    }

    @Test
    fun flingForward_highVelocity_stopsAtNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(10) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        performIndirectSwipe(itemHeight, durationMillis = FlingDuration)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(1)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
    }

    @Test
    fun twoFlingsForward_stopsAtNextNextItem() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(10) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        performIndirectSwipe(itemHeight, durationMillis = FlingDuration)
        performIndirectSwipe(itemHeight, durationMillis = FlingDuration)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
    }

    @Test
    fun flingBackward_highVelocity_stopsAtPreviousItem() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(10) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(3) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(3)

        performIndirectSwipe(-itemHeight, durationMillis = FlingDuration)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(2)
        rule.onNodeWithText("Item 2").assertIsDisplayed()
        rule.onNodeWithText("Item 1").assertIsNotDisplayed()
    }

    @Test
    fun dragForwardAndBackward_sameGesture_canReachPreviousItem() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.waitForIdle()
        assertThat(state.topItem).isEqualTo(2)

        val press = performIndirectPress()
        val moveForward =
            performIndirectMove(distancePx = itemHeight.toFloat() * 3, previousMotionEvent = press)
        val moveBackward =
            performIndirectMove(
                distancePx = -itemHeight.toFloat() * 2,
                previousMotionEvent = moveForward,
            )
        performIndirectRelease(previousMotionEvent = moveBackward)
        rule.waitForIdle()

        assertThat(state.topItem).isEqualTo(1)
        rule.onNodeWithText("Item 1").assertIsDisplayed()
    }

    @Test
    fun recompositions_afterInitialComposition_doesNotRecompose() {
        var recompositionCount = 0
        rule.setContent {
            VerticalStack {
                item {
                    StackItem("Item 0")
                    SideEffect { recompositionCount++ }
                }
            }
        }

        rule.runOnIdle { assertThat(recompositionCount).isEqualTo(1) }
    }

    @Test
    fun recompositions_afterScroll_doesNotRecompose() {
        val stackHeight = 100.dp
        val stackHeightPx = with(rule.density) { stackHeight.roundToPx() }
        var item0RecompositionCount = 0
        var item1RecompositionCount = 0
        var item2RecompositionCount = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.size(stackHeight), state = state) {
                item {
                    StackItem("Item 0")
                    SideEffect { item0RecompositionCount++ }
                }
                item {
                    StackItem("Item 1")
                    SideEffect { item1RecompositionCount++ }
                }
                item {
                    StackItem("Item 2")
                    SideEffect { item2RecompositionCount++ }
                }
            }
        }

        performIndirectSwipe(stackHeightPx)

        rule.runOnIdle {
            assertThat(state.topItem).isEqualTo(1)
            assertThat(item0RecompositionCount).isEqualTo(1)
            assertThat(item1RecompositionCount).isEqualTo(1)
            assertThat(item2RecompositionCount).isEqualTo(1)
        }
    }

    @Composable
    private fun StackItem(
        text: String,
        modifier: Modifier = Modifier,
        onHeightChanged: (Int) -> Unit = {},
    ) {
        Box(
            modifier
                .onSizeChanged { onHeightChanged(it.height) }
                .fillMaxSize()
                .focusable()
                .testTag(text)
        ) {
            Text(text)
        }
    }

    private fun performIndirectPress() = rule.onRoot().performIndirectPress(rule)

    private fun performIndirectMove(distancePx: Float, previousMotionEvent: MotionEvent) =
        rule
            .onRoot()
            .performIndirectMove(
                rule = rule,
                distancePx = distancePx,
                previousMotionEvent = previousMotionEvent,
            )

    private fun performIndirectRelease(previousMotionEvent: MotionEvent) =
        rule.onRoot().performIndirectRelease(rule, previousMotionEvent)

    private fun performIndirectSwipe(distancePx: Int, durationMillis: Long = 200L) {
        require(distancePx != 0)
        rule
            .onRoot()
            .performIndirectSwipe(rule, distancePx.toFloat(), moveDuration = durationMillis)
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }

    private fun Color.toOpaque(): Color = copy(alpha = 1.0f)

    private fun Dp.toPx(): Int = with(rule.density) { roundToPx() }
}

/** A short swipe duration to trigger a fling. */
private const val FlingDuration: Long = 50L
