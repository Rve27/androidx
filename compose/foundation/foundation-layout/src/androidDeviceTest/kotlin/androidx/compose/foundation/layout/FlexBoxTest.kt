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

package androidx.compose.foundation.layout

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FlexBoxTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    // Direction Tests
    @Test
    fun testFlexBox_directionRow_defaultsToRow() {
        var width = 0
        var height = 0
        val positions = mutableListOf<Float>()

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                width = it.width
                                height = it.height
                            }
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
                                    positions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positions).containsExactly(0f, 20f, 40f).inOrder()
        Truth.assertThat(width).isEqualTo(60)
        Truth.assertThat(height).isEqualTo(20)
    }

    @Test
    fun testFlexBox_directionRow() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(config = { direction = FlexDirection.Row }) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(xPositions).containsExactly(0f, 20f, 40f).inOrder()
    }

    @Test
    fun testFlexBox_directionColumn() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(config = { direction = FlexDirection.Column }) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(yPositions).containsExactly(0f, 20f, 40f).inOrder()
    }

    @Test
    fun testFlexBox_directionRowReverse() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = { direction = FlexDirection.RowReverse },
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Items should be placed from right edge: 180, 160, 140
        Truth.assertThat(xPositions).containsExactly(180f, 160f, 140f).inOrder()
    }

    @Test
    fun testFlexBox_directionColumnReverse() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxHeight(),
                        config = { direction = FlexDirection.ColumnReverse },
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Items should be placed from bottom edge: 180, 160, 140
        Truth.assertThat(yPositions).containsExactly(180f, 160f, 140f).inOrder()
    }

    @Test
    fun testFlexBox_wrap() {
        var height = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { height = it.height },
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                        },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 5 items fit per row (100 / 20), so 6 items = 2 rows = 40 height
        Truth.assertThat(height).isEqualTo(40)
    }

    @Test
    fun testFlexBox_wrapReverse() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.WrapReverse
                        }
                    ) {
                        repeat(6) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // First row (items 0-4) should be at bottom (y=20), second row (item 5) at top (y=0)
        Truth.assertThat(yPositions.take(5)).containsExactly(20f, 20f, 20f, 20f, 20f)
        Truth.assertThat(yPositions[5]).isEqualTo(0f)
    }

    @Test
    fun testFlexBox_columnWrap() {
        var width = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { width = it.width },
                        config = {
                            direction = FlexDirection.Column
                            wrap = FlexWrap.Wrap
                        },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 5 items fit per column (100 / 20), so 6 items = 2 columns = 40 width
        Truth.assertThat(width).isEqualTo(40)
    }

    // JustifyContent Tests

    @Test
    fun testFlexBox_justifyContentStart() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction = FlexDirection.Row
                            justifyContent = FlexJustifyContent.Start
                        },
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(xPositions).containsExactly(0f, 20f, 40f).inOrder()
    }

    @Test
    fun testFlexBox_justifyContentEnd() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction = FlexDirection.Row
                            justifyContent = FlexJustifyContent.End
                        },
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Items at end: 200 - 60 = 140 start position
        Truth.assertThat(xPositions).containsExactly(140f, 160f, 180f).inOrder()
    }

    @Test
    fun testFlexBox_justifyContentCenter() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction = FlexDirection.Row
                            justifyContent = FlexJustifyContent.Center
                        },
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Items centered: (200 - 60) / 2 = 70 start position
        Truth.assertThat(xPositions).containsExactly(70f, 90f, 110f).inOrder()
    }

    @Test
    fun testFlexBox_justifyContentSpaceBetween() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction = FlexDirection.Row
                            justifyContent = FlexJustifyContent.SpaceBetween
                        },
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Space between: (200 - 60) / 2 = 70 gap between items
        // Positions: 0, 90, 180
        Truth.assertThat(xPositions).containsExactly(0f, 90f, 180f).inOrder()
    }

    @Test
    fun testFlexBox_justifyContentSpaceAround() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction = FlexDirection.Row
                            justifyContent = FlexJustifyContent.SpaceAround
                        },
                    ) {
                        repeat(5) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Space around: (200 - 100) / 5 = 20 per item
        // Half space on edges = 10, full space between = 20
        // Positions: 10, 50, 90, 130, 170
        Truth.assertThat(xPositions).containsExactly(10f, 50f, 90f, 130f, 170f).inOrder()
    }

    @Test
    fun testFlexBox_justifyContentSpaceEvenly() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction = FlexDirection.Row
                            justifyContent = FlexJustifyContent.SpaceEvenly
                        },
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Space evenly: (200 - 60) / 4 = 35 gap
        // Positions: 35, 90, 145
        Truth.assertThat(xPositions).containsExactly(35f, 90f, 145f).inOrder()
    }

    // AlignItems Tests

    @Test
    fun testFlexBox_alignItemsStart() {
        val yPositions = mutableListOf<Float>()
        val itemSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            alignItems = FlexAlignItems.Start
                        }
                    ) {
                        itemSizes.forEachIndexed { _, size ->
                            Box(
                                Modifier.size(20.dp, size.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // All items aligned to start (top)
        Truth.assertThat(yPositions).containsExactly(0f, 0f, 0f)
    }

    @Test
    fun testFlexBox_alignItemsEnd() {
        val yPositions = mutableListOf<Float>()
        val itemSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            alignItems = FlexAlignItems.End
                        }
                    ) {
                        itemSizes.forEachIndexed { _, size ->
                            Box(
                                Modifier.size(20.dp, size.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Max height is 40, items aligned to bottom
        // Positions: 40-20=20, 40-40=0, 40-30=10
        Truth.assertThat(yPositions).containsExactly(20f, 0f, 10f)
    }

    @Test
    fun testFlexBox_alignItemsCenter() {
        val yPositions = mutableListOf<Float>()
        val itemSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            alignItems = FlexAlignItems.Center
                        }
                    ) {
                        itemSizes.forEachIndexed { _, size ->
                            Box(
                                Modifier.size(20.dp, size.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Max height is 40, items centered
        // Positions: (40-20)/2=10, (40-40)/2=0, (40-30)/2=5
        Truth.assertThat(yPositions).containsExactly(10f, 0f, 5f)
    }

    @Test
    fun testFlexBox_alignItemsStretch() {
        val heights = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            alignItems = FlexAlignItems.Stretch
                        }
                    ) {
                        // This item will decide the line height
                        Box(Modifier.width(20.dp).height(40.dp))
                        repeat(2) { _ ->
                            Box(
                                Modifier.width(20.dp)
                                    // No height specified - should stretch
                                    .onSizeChanged { heights.add(it.height) }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // All items should stretch to container height or max sibling height
        Truth.assertThat(heights).containsExactly(40, 40)
    }

    // Gap Tests

    @Test
    fun testFlexBox_rowGap() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                            rowGap = 10.dp
                        }
                    ) {
                        repeat(10) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 5 items per row, 2 rows
        // Row 1: y=0, Row 2: y=20+10=30
        val uniqueYPositions = yPositions.distinct().sorted()
        Truth.assertThat(uniqueYPositions).containsExactly(0f, 30f).inOrder()
    }

    @Test
    fun testFlexBox_columnGap() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            columnGap = 10.dp
                        }
                    ) {
                        repeat(3) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Items with 10dp gap: 0, 30, 60
        Truth.assertThat(xPositions).containsExactly(0f, 30f, 60f).inOrder()
    }

    @Test
    fun testFlexBox_gap() {
        var height = 0
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { height = it.height },
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                            gap(10.dp) // Sets both rowGap and columnGap
                        },
                    ) {
                        repeat(6) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // With 10dp column gap: items at 0, 30, 60 (3 per row fits in 100dp)
        // Then wrap to second row
        // Height should be 20 + 10 + 20 = 50
        Truth.assertThat(height).isEqualTo(50)
    }

    @Test
    fun testFlexBox_wrap_excludesTrailingGap() {
        var height = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { height = it.height },
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                            gap(10.dp)
                        },
                    ) {
                        // Item 1: 45
                        // Gap: 10
                        // Item 2: 45
                        // Total required: 45 + 10 + 45 = 100.
                        // If trailing gap is incorrectly counted: 100 + 10 = 110 (Wrap).
                        // Correct behavior: 100 (No Wrap).
                        Box(Modifier.size(45.dp))
                        Box(Modifier.size(45.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        // Should fit on 1 line. Height = 45.
        Truth.assertThat(height).isEqualTo(45)
    }

    @Test
    fun testFlexBox_wrap_gapPreservedAfterLineBreak() {
        var height = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { height = it.height },
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                            gap(15.dp)
                        },
                    ) {
                        Box(Modifier.size(100.dp, 20.dp))

                        Box(Modifier.size(45.dp, 20.dp))
                        Box(Modifier.size(45.dp, 20.dp))
                    }
                }
            }
        }

        rule.waitForIdle()

        // Expected: 3 lines.
        // Line 1: 20dp
        // Gap: 15dp
        // Line 2: 20dp
        // Gap: 15dp
        // Line 3: 20dp
        // Total Height: 20 + 15 + 20 + 15 + 20 = 90
        Truth.assertThat(height).isEqualTo(90)
    }

    @Test
    fun testFlexBox_layout_zeroSizeItem_hasGap() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            gap(10.dp)
                        }
                    ) {
                        // Item 1: 0dp
                        Box(
                            Modifier.size(0.dp, 20.dp).onPlaced {
                                xPositions.add(it.positionInParent().x)
                            }
                        )
                        // Item 2: 20dp
                        Box(
                            Modifier.size(20.dp, 20.dp).onPlaced {
                                xPositions.add(it.positionInParent().x)
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Item 1 at 0.
        // Item 2 at 0 + 10 (Gap) = 10.
        Truth.assertThat(xPositions).containsExactly(0f, 10f).inOrder()
    }

    // Flex Item Style Tests

    @Test
    fun testFlexBox_flexGrow() {
        val widths = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = { direction = FlexDirection.Row },
                    ) {
                        // Item with grow=0 (default)
                        Box(Modifier.size(20.dp).onSizeChanged { widths.add(it.width) })
                        // Item with grow=1
                        Box(
                            Modifier.size(20.dp)
                                .flex { grow = 1f }
                                .onSizeChanged { widths.add(it.width) }
                        )
                        // Item with grow=2
                        Box(
                            Modifier.size(20.dp)
                                .flex { grow = 2f }
                                .onSizeChanged { widths.add(it.width) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Available space: 200 - 60 = 140
        // grow=1 gets 140/3 ≈ 46.67, grow=2 gets 140*2/3 ≈ 93.33
        // First item: 20, Second: 20+46=66 (approx), Third: 20+93=113 (approx)
        Truth.assertThat(widths[0]).isEqualTo(20)
        Truth.assertThat(widths[1]).isGreaterThan(20)
        Truth.assertThat(widths[2]).isGreaterThan(widths[1])
    }

    @Test
    fun testFlexBox_flexShrink() {
        val widths = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier = Modifier.width(100.dp),
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.NoWrap
                        },
                    ) {
                        // Item with shrink=0 (won't shrink)
                        Box(
                            Modifier.width(60.dp)
                                .height(20.dp)
                                .flex { shrink = 0f }
                                .onSizeChanged { widths.add(it.width) }
                        )
                        // Item with shrink=1 (default, will shrink)
                        Box(
                            Modifier.widthIn(min = 40.dp, max = 60.dp).height(20.dp).onSizeChanged {
                                widths.add(it.width)
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Total: 120, Available: 100, Overflow: 20
        // First item (shrink=0): stays at 60
        // Second item (shrink=1): shrinks by 20 to 40
        Truth.assertThat(widths[0]).isEqualTo(60)
        Truth.assertThat(widths[1]).isEqualTo(40)
    }

    @Test
    fun testFlexBox_flexBasisDp() {
        val widths = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(config = { direction = FlexDirection.Row }) {
                        Box(
                            Modifier.flex { basis(50.dp) }
                                .height(20.dp)
                                .onSizeChanged { widths.add(it.width) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(widths[0]).isEqualTo(50)
    }

    @Test
    fun testFlexBox_flexBasisPercent() {
        val widths = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = { direction = FlexDirection.Row },
                    ) {
                        Box(
                            Modifier.flex { basis(0.5f) } // 50%
                                .height(20.dp)
                                .onSizeChanged { widths.add(it.width) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(widths[0]).isEqualTo(100) // 50% of 200
    }

    @Test
    fun testFlexBox_alignSelf() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction = FlexDirection.Row
                            alignItems = FlexAlignItems.Start
                        }
                    ) {
                        // Normal item at start
                        Box(
                            Modifier.size(20.dp, 40.dp).onPlaced {
                                yPositions.add(it.positionInParent().y)
                            }
                        )
                        // Item with alignSelf override to End
                        Box(
                            Modifier.size(20.dp)
                                .flex { alignSelf = FlexAlignSelf.End }
                                .onPlaced { yPositions.add(it.positionInParent().y) }
                        )
                        // Item with alignSelf override to Center
                        Box(
                            Modifier.size(20.dp)
                                .flex { alignSelf = FlexAlignSelf.Center }
                                .onPlaced { yPositions.add(it.positionInParent().y) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Line height is 40 (tallest item)
        // First item: y=0 (alignItems=Start)
        // Second item: y=40-20=20 (alignSelf=End)
        // Third item: y=(40-20)/2=10 (alignSelf=Center)
        Truth.assertThat(yPositions).containsExactly(0f, 20f, 10f).inOrder()
    }

    @Test
    fun testFlexBox_order() {
        val xPositions = mutableMapOf<Int, Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(config = { direction = FlexDirection.Row }) {
                        // Item A with order=2
                        Box(
                            Modifier.size(20.dp)
                                .flex { order = 2 }
                                .onPlaced { xPositions[0] = it.positionInParent().x }
                        )
                        // Item B with order=0 (default)
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                xPositions[1] = it.positionInParent().x
                            }
                        )
                        // Item C with order=1
                        Box(
                            Modifier.size(20.dp)
                                .flex { order = 1 }
                                .onPlaced { xPositions[2] = it.positionInParent().x }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Visual order should be: B (order=0), C (order=1), A (order=2)
        // So item B (index 1) at x=0, C (index 2) at x=20, A (index 0) at x=40
        Truth.assertThat(xPositions[1]).isEqualTo(0f) // B first
        Truth.assertThat(xPositions[2]).isEqualTo(20f) // C second
        Truth.assertThat(xPositions[0]).isEqualTo(40f) // A third
    }

    //  Empty and Single Item Tests

    @Test
    fun testFlexBox_empty() {
        var width = 0
        var height = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                width = it.width
                                height = it.height
                            }
                    ) {
                        // No children
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(0)
        Truth.assertThat(height).isEqualTo(0)
    }

    @Test
    fun testFlexBox_singleItem() {
        var width = 0
        var height = 0
        var itemX = 0f
        var itemY = 0f

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                width = it.width
                                height = it.height
                            }
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                itemX = it.positionInParent().x
                                itemY = it.positionInParent().y
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(width).isEqualTo(20)
        Truth.assertThat(height).isEqualTo(20)
        Truth.assertThat(itemX).isEqualTo(0f)
        Truth.assertThat(itemY).isEqualTo(0f)
    }

    //  AlignContent Tests (multi-line)

    @Test
    fun testFlexBox_alignContentStart() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                            alignContent = FlexAlignContent.Start
                        },
                    ) {
                        // Force 2 rows: 5 items of 50dp each = 250dp, wraps at 200dp
                        repeat(6) { _ ->
                            Box(
                                Modifier.size(50.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 4 items per row (200/50), 2 rows
        // AlignContent.Start: rows at top
        val uniqueY = yPositions.distinct().sorted()
        Truth.assertThat(uniqueY).containsExactly(0f, 50f).inOrder()
    }

    @Test
    fun testFlexBox_alignContentCenter() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                            alignContent = FlexAlignContent.Center
                        },
                    ) {
                        repeat(6) { _ ->
                            Box(
                                Modifier.size(50.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 2 rows of 50dp each = 100dp total
        // Centered in 200dp: (200-100)/2 = 50dp offset
        val uniqueY = yPositions.distinct().sorted()
        Truth.assertThat(uniqueY).containsExactly(50f, 100f).inOrder()
    }

    @Test
    fun testFlexBox_alignContentSpaceBetween() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                            alignContent = FlexAlignContent.SpaceBetween
                        },
                    ) {
                        repeat(6) { _ ->
                            Box(
                                Modifier.size(50.dp).onPlaced {
                                    yPositions.add(it.positionInParent().y)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 2 rows, space between: first at 0, second at 200-50=150
        val uniqueY = yPositions.distinct().sorted()
        Truth.assertThat(uniqueY).containsExactly(0f, 150f).inOrder()
    }

    //  Combined/Complex Tests

    @Test
    fun testFlexBox_complexMultiLine() {
        val positions = mutableListOf<Pair<Float, Float>>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.width(100.dp),
                    config = {
                        direction = FlexDirection.Row
                        wrap = FlexWrap.Wrap
                        justifyContent = FlexJustifyContent.SpaceBetween
                        alignItems = FlexAlignItems.Center
                        gap(10.dp)
                    },
                ) {
                    listOf(30, 40, 20, 50, 25).forEachIndexed { _, width ->
                        Box(
                            Modifier.size(width.dp, 20.dp).onPlaced {
                                positions.add(it.positionInParent().x to it.positionInParent().y)
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Verify items are positioned (specific positions depend on implementation)
        Truth.assertThat(positions).hasSize(5)
    }

    @Test
    fun testFlexBox_nestedFlexBox() {
        var outerWidth = 0
        var innerWidth = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.onSizeChanged { outerWidth = it.width },
                    config = { direction = FlexDirection.Column },
                ) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { innerWidth = it.width },
                        config = { direction = FlexDirection.Row },
                    ) {
                        repeat(3) { Box(Modifier.size(20.dp)) }
                    }
                    Box(Modifier.size(100.dp))
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(innerWidth).isEqualTo(60) // 3 * 20
        Truth.assertThat(outerWidth).isEqualTo(100) // Max of children
    }

    //  Style Reuse Tests

    @Test
    fun testFlexBox_reusableStyle() {
        val xPositions1 = mutableListOf<Float>()
        val xPositions2 = mutableListOf<Float>()

        // Define reusable style outside composition
        val centeredRowStyle = FlexBoxConfig {
            direction = FlexDirection.Row
            justifyContent = FlexJustifyContent.Center
        }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Column {
                    FlexBox(modifier = Modifier.width(100.dp), config = centeredRowStyle) {
                        repeat(2) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions1.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                    FlexBox(modifier = Modifier.width(100.dp), config = centeredRowStyle) {
                        repeat(2) { _ ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions2.add(it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Both FlexBoxes should have same positioning
        // Centered: (100 - 40) / 2 = 30
        Truth.assertThat(xPositions1).containsExactly(30f, 50f).inOrder()
        Truth.assertThat(xPositions2).containsExactly(30f, 50f).inOrder()
    }

    //  Edge Cases

    @Test
    fun testFlexBox_zeroSizeChildren() {
        var flexBoxWidth = 0
        var flexBoxHeight = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier =
                        Modifier.onSizeChanged {
                            flexBoxWidth = it.width
                            flexBoxHeight = it.height
                        }
                ) {
                    repeat(3) { Box(Modifier.size(0.dp)) }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(flexBoxWidth).isEqualTo(0)
        Truth.assertThat(flexBoxHeight).isEqualTo(0)
    }

    @Test
    fun testFlexBox_manyChildren() {
        var height = 0
        var itemsPlaced = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(1000.dp)) {
                    FlexBox(
                        modifier = Modifier.width(100.dp).onSizeChanged { height = it.height },
                        config = {
                            direction = FlexDirection.Row
                            wrap = FlexWrap.Wrap
                        },
                    ) {
                        repeat(100) { Box(Modifier.size(10.dp).onPlaced { itemsPlaced++ }) }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 10 items per row (100/10), 10 rows
        Truth.assertThat(height).isEqualTo(100)
        Truth.assertThat(itemsPlaced).isEqualTo(100)
    }

    companion object {
        private val NoOpDensity =
            object : Density {
                override val density = 1f
                override val fontScale = 1f
            }
    }
}
