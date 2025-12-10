/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class GridTest : LayoutTest() {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testGrid_itemModifierChange_triggersRelayout() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            var targetRow by mutableStateOf(1)

            // Latch for initial layout (Row 1)
            val initialLatch = CountDownLatch(1)
            // Latch for update layout (Row 2)
            val updateLatch = CountDownLatch(1)

            val childPosition = Ref<Offset>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    Box(
                        Modifier.gridItem(row = targetRow, column = 1)
                            .size(sizeDp)
                            .onGloballyPositioned { coordinates ->
                                childPosition.value = coordinates.localToRoot(Offset.Zero)
                                if (initialLatch.count > 0) {
                                    initialLatch.countDown()
                                } else {
                                    updateLatch.countDown()
                                }
                            }
                    )
                }
            }

            // 1. Verify Initial Position (Row 1 -> Index 0 -> 0px)
            assertTrue(
                "Timed out waiting for initial layout",
                initialLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(Offset(0f, 0f), childPosition.value)

            // 2. Update State
            targetRow = 2

            // 3. Verify Updated Position (Row 2 -> Index 1 -> 50px)
            assertTrue(
                "Timed out waiting for layout update",
                updateLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(Offset(0f, size.toFloat()), childPosition.value)
        }

    @Test
    fun testGrid_configStateChange_updatesLayout() =
        with(density) {
            var trackSize by mutableStateOf(50.dp)

            // Use separate latches for the initial pass and the update pass.
            val initialLayoutLatch = CountDownLatch(1)
            val updateLayoutLatch = CountDownLatch(1)

            val childSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // Reading 'trackSize' here registers a dependency.
                        // When 'trackSize' changes, this lambda re-executes, triggering
                        // the MeasurePolicy to re-measure.
                        column(GridTrackSize.Fixed(trackSize))
                        row(GridTrackSize.Fixed(trackSize))
                    }
                ) {
                    Box(
                        Modifier.gridItem(1, 1).fillMaxSize().onGloballyPositioned { coordinates ->
                            childSize.value = coordinates.size

                            // If the first latch is still open, this is the initial layout.
                            // Otherwise, we are in the update phase.
                            if (initialLayoutLatch.count > 0) {
                                initialLayoutLatch.countDown()
                            } else {
                                updateLayoutLatch.countDown()
                            }
                        }
                    )
                }
            }

            // Verify Initial State (50.dp)
            // Wait for the FIRST layout pass to complete
            assertTrue(
                "Timed out waiting for initial layout",
                initialLayoutLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(
                "Initial size incorrect",
                IntSize(50.dp.roundToPx(), 50.dp.roundToPx()),
                childSize.value,
            )

            // Change State
            trackSize = 100.dp

            // Verify Update (100.dp)
            // Wait for the SECOND layout pass (recomposition) to complete
            assertTrue(
                "Timed out waiting for layout update",
                updateLayoutLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(
                "Grid did not update track size after config state changed",
                IntSize(100.dp.roundToPx(), 100.dp.roundToPx()),
                childSize.value,
            )
        }

    @Test
    fun testGrid_fixedTracks_sizeCorrectly() =
        with(density) {
            val size1 = 50
            val size2 = 100
            val size1Dp = size1.toDp()
            val size2Dp = size2.toDp()

            val positionedLatch = CountDownLatch(2)
            val childSize = Array(2) { Ref<IntSize>() }
            val childPosition = Array(2) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size1Dp))
                        column(GridTrackSize.Fixed(size2Dp))
                        row(GridTrackSize.Fixed(size1Dp))
                    }
                ) {
                    // R1, C1
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    // R1, C2
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // Check Item 1 (50x50) at (0,0)
            assertEquals(IntSize(size1, size1), childSize[0].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)

            // Check Item 2 (100x50) at (50,0)
            assertEquals(IntSize(size2, size1), childSize[1].value)
            assertEquals(Offset(size1.toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_fractionTracks() =
        with(density) {
            val totalSize = 200
            val totalSizeDp = totalSize.toDp()

            // Col 1: 25% = 50px
            // Col 2: 75% = 150px
            val expectedCol1 = (totalSize * 0.25f).roundToInt()
            val expectedCol2 = (totalSize * 0.75f).roundToInt()

            val positionedLatch = CountDownLatch(2)
            val childSize = Array(2) { Ref<IntSize>() }
            val childPosition = Array(2) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Percentage(0.25f))
                        column(GridTrackSize.Percentage(0.75f))
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.size(totalSizeDp, 50.dp),
                ) {
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(IntSize(expectedCol1, 50.dp.roundToPx()), childSize[0].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)

            assertEquals(IntSize(expectedCol2, 50.dp.roundToPx()), childSize[1].value)
            assertEquals(Offset(expectedCol1.toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_percentageRows_resolvesAgainstHeight() =
        with(density) {
            // Scenario:
            // Fixed Height Container (200px).
            // Row 1: 25% (50px)
            // Row 2: 75% (150px)

            val totalHeight = 200
            val expectedRow1 = (totalHeight * 0.25f).roundToInt()
            val expectedRow2 = (totalHeight * 0.75f).roundToInt()

            val latch = CountDownLatch(2)
            val sizes = Array(2) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(50.dp))
                        row(GridTrackSize.Percentage(0.25f))
                        row(GridTrackSize.Percentage(0.75f))
                    },
                    modifier = Modifier.height(totalHeight.toDp()),
                ) {
                    // Item in Row 1
                    Box(
                        Modifier.gridItem(1, 1).fillMaxSize().saveLayoutInfo(sizes[0], Ref(), latch)
                    )
                    // Item in Row 2
                    Box(
                        Modifier.gridItem(2, 1).fillMaxSize().saveLayoutInfo(sizes[1], Ref(), latch)
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(expectedRow1, sizes[0].value?.height)
            assertEquals(expectedRow2, sizes[1].value?.height)
        }

    @Test
    fun testGrid_flexTracks() =
        with(density) {
            val totalWidth = 300
            val fixedWidth = 100

            // Remaining space = 200
            // Flex 1: 1fr = 200 * (1/4) = 50
            // Flex 2: 3fr = 200 * (3/4) = 150

            val positionedLatch = CountDownLatch(3)
            val childSize = Array(3) { Ref<IntSize>() }
            val childPosition = Array(3) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(fixedWidth.toDp()))
                        column(GridTrackSize.Flex(1.fr))
                        column(GridTrackSize.Flex(3.fr))
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.width(totalWidth.toDp()),
                ) {
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 3)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // Fixed Col
            assertEquals(IntSize(fixedWidth, 50.dp.roundToPx()), childSize[0].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)

            // Flex 1 (50px)
            assertEquals(IntSize(50, 50.dp.roundToPx()), childSize[1].value)
            assertEquals(Offset(fixedWidth.toFloat(), 0f), childPosition[1].value)

            // Flex 3 (150px)
            assertEquals(IntSize(150, 50.dp.roundToPx()), childSize[2].value)
            assertEquals(Offset((fixedWidth + 50).toFloat(), 0f), childPosition[2].value)
        }

    @Test
    fun testGrid_flexTrack_minContent_lowerBound() =
        with(density) {
            val containerWidth = 50
            val itemMinSize = 100
            val latch = CountDownLatch(1)
            val childSize = Ref<IntSize>()

            show {
                // Container is 50px wide, but item needs 100px.
                // Flex track should expand to 100px (min-content), ignoring the 50px constraint.
                Box(Modifier.width(containerWidth.toDp())) {
                    Grid(
                        config = {
                            column(GridTrackSize.Flex(1.fr))
                            row(GridTrackSize.Fixed(50.dp))
                        }
                    ) {
                        IntrinsicItem(
                            minWidth = itemMinSize,
                            minIntrinsicWidth = itemMinSize,
                            maxIntrinsicWidth = itemMinSize,
                            modifier =
                                Modifier.gridItem(1, 1)
                                    .fillMaxHeight()
                                    .saveLayoutInfo(childSize, Ref(), latch),
                        )
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Flex track should not shrink below min-content size",
                itemMinSize,
                childSize.value?.width,
            )
        }

    @Test
    fun testGrid_flexTracks_distributeSpace_afterMinContent() =
        with(density) {
            // Scenario:
            // Container = 200px
            // Track 1 (1fr): Has large content (150px)
            // Track 2 (1fr): Has small content (10px)
            //
            // Calculation:
            // 1. Base Sizes (Pass 1):
            //    Track 1 = 150px
            //    Track 2 = 10px
            //    Used = 160px
            //
            // 2. Remaining Space (Pass 2):
            //    200px - 160px = 40px
            //
            // 3. Distribution (Equal weight 1fr):
            //    Track 1 adds 20px -> Final 170px
            //    Track 2 adds 20px -> Final 30px

            val containerWidth = 200
            val item1MinSize = 150
            val item2MinSize = 10
            val latch = CountDownLatch(2)
            val size1 = Ref<IntSize>()
            val size2 = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Flex(1.fr))
                        column(GridTrackSize.Flex(1.fr))
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.width(containerWidth.toDp()),
                ) {
                    // Item 1: Large Min Content
                    IntrinsicItem(
                        minWidth = item1MinSize,
                        minIntrinsicWidth = item1MinSize,
                        maxIntrinsicWidth = item1MinSize,
                        modifier =
                            Modifier.gridItem(1, 1)
                                .fillMaxWidth() // <--- ADD THIS
                                .saveLayoutInfo(size1, Ref(), latch),
                    )

                    // Item 2: Small Min Content
                    IntrinsicItem(
                        minWidth = item2MinSize,
                        minIntrinsicWidth = item2MinSize,
                        maxIntrinsicWidth = item2MinSize,
                        modifier =
                            Modifier.gridItem(1, 2)
                                .fillMaxWidth() // <--- ADD THIS
                                .saveLayoutInfo(size2, Ref(), latch),
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals("Track 1 should be Base(150) + Share(20)", 170, size1.value?.width)
            assertEquals("Track 2 should be Base(10) + Share(20)", 30, size2.value?.width)
        }

    @Test
    fun testGrid_rowSpan_expandsRowsToFitContent() =
        with(density) {
            // Scenario:
            // 2 Columns (Fixed 50)
            // 2 Rows (Auto)
            // Item 1 (Col 1, Row 1): Small (10px height)
            // Item 2 (Col 1, Row 2): Small (10px height)
            // Item 3 (Col 2, Row 1, Span 2): Tall (100px height)
            //
            // Expected Behavior:
            // Without spanning logic: Rows would be 10px each (total 20px). Item 3 would overflow.
            // With spanning logic: Item 3 needs 100px.
            // Deficit = 100 - (10 + 10) = 80px.
            // Distribute 80px / 2 rows = +40px each.
            // Final Row Heights: 10 + 40 = 50px each.
            // Total Grid Height: 100px.

            val colWidth = 50.dp
            val smallItemHeight = 10.dp
            val tallItemHeight = 100.dp
            val expectedTotalHeight = 100.dp.roundToPx()

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(colWidth))
                        column(GridTrackSize.Fixed(colWidth))
                        row(GridTrackSize.Auto)
                        row(GridTrackSize.Auto)
                    },
                    modifier =
                        Modifier.onGloballyPositioned {
                            gridSize.value = it.size
                            latch.countDown()
                        },
                ) {
                    // Col 1, Row 1
                    Box(Modifier.gridItem(1, 1).size(colWidth, smallItemHeight))
                    // Col 1, Row 2
                    Box(Modifier.gridItem(2, 1).size(colWidth, smallItemHeight))

                    // Col 2, Row 1, Span 2 (The driver of expansion)
                    Box(
                        Modifier.gridItem(row = 1, column = 2, rowSpan = 2)
                            .size(colWidth, tallItemHeight)
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Grid height should expand to accommodate the tall row-spanning item",
                expectedTotalHeight,
                gridSize.value?.height,
            )
        }

    @Test
    fun testGrid_rowSpan_expandsFlexRows() =
        with(density) {
            // Scenario:
            // Container Height = 100px (Fixed constraint)
            // 2 Rows (Flex 1fr)
            // Item 1 (Row 1): Empty
            // Item 2 (Row 2): Empty
            // Item 3 (Span 2): Tall (200px)
            //
            // Expected Behavior:
            // Flex logic initially splits 100px -> 50px each.
            // Spanning logic sees Item 3 needs 200px.
            // Deficit = 200 - 100 = 100px.
            // Rows should grow to 100px each.
            // Total Height = 200px (Grid expands beyond parent constraint if content demands it).

            val containerHeight = 100.dp
            val tallItemHeight = 200.dp
            val expectedTotalHeight = 200.dp.roundToPx()

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Wrap in a box with fixed height to simulate constraints,
                // but allow Grid to be larger (unbounded internal checks)
                Box(
                    Modifier.height(containerHeight)
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                ) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(50.dp))
                            row(GridTrackSize.Flex(1.fr))
                            row(GridTrackSize.Flex(1.fr))
                        },
                        modifier =
                            Modifier.onGloballyPositioned {
                                gridSize.value = it.size
                                latch.countDown()
                            },
                    ) {
                        // Spanning item forcing expansion
                        Box(
                            Modifier.gridItem(row = 1, column = 1, rowSpan = 2)
                                .size(50.dp, tallItemHeight)
                        )
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Flex rows should expand beyond 1fr share if spanning item requires it",
                expectedTotalHeight,
                gridSize.value?.height,
            )
        }

    @Test
    fun testGrid_explicitPlacement_allowsOverlaps() =
        with(density) {
            // Scenario:
            // Two items explicitly placed in (1, 1).
            // They should occupy the same space. The grid should not throw or shift them.

            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(2)
            val pos = Array(2) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    // Item 1
                    Box(Modifier.gridItem(1, 1).size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // Item 2 (Same Cell)
                    Box(Modifier.gridItem(1, 1).size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(0f, 0f), pos[1].value)
        }

    @Test
    fun testGrid_negativeIndices_placeCorrectly() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()

            val positionedLatch = CountDownLatch(4)
            val childPosition = Array(4) { Ref<Offset>() }
            val dummySize = Array(4) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        repeat(3) { column(GridTrackSize.Fixed(sizeDp)) }
                        repeat(3) { row(GridTrackSize.Fixed(sizeDp)) }
                    }
                ) {
                    // 1. Top-Left (1, 1) -> (0, 0)
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[0], childPosition[0], positionedLatch)
                    )
                    // 2. Top-Right (1, -1) -> (100, 0) (Last Column)
                    Box(
                        Modifier.gridItem(1, -1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[1], childPosition[1], positionedLatch)
                    )
                    // 3. Bottom-Left (-1, 1) -> (0, 100) (Last Row)
                    Box(
                        Modifier.gridItem(-1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[2], childPosition[2], positionedLatch)
                    )
                    // 4. Bottom-Right (-1, -1) -> (100, 100) (Last Row, Last Column)
                    Box(
                        Modifier.gridItem(-1, -1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[3], childPosition[3], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // 1. (0, 0)
            assertEquals(Offset(0f, 0f), childPosition[0].value)
            // 2. (100, 0) -> Col index 2 * 50
            assertEquals(Offset((size * 2).toFloat(), 0f), childPosition[1].value)
            // 3. (0, 100) -> Row index 2 * 50
            assertEquals(Offset(0f, (size * 2).toFloat()), childPosition[2].value)
            // 4. (100, 100)
            assertEquals(Offset((size * 2).toFloat(), (size * 2).toFloat()), childPosition[3].value)
        }

    @Test
    fun testGrid_invalidNegativeIndices_fallbackToAuto() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(2)
            val pos1 = Ref<Offset>()
            val pos2 = Ref<Offset>()
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // 2x2 Grid
                        repeat(2) { column(GridTrackSize.Fixed(sizeDp)) }
                        repeat(2) { row(GridTrackSize.Fixed(sizeDp)) }
                    }
                ) {
                    // Case 1: Valid Negative (-1 -> Index 1)
                    Box(
                        Modifier.gridItem(row = -1, column = -1)
                            .size(sizeDp)
                            .saveLayoutInfo(dummy, pos1, latch)
                    )

                    // Case 2: Invalid Negative (-5 -> Index -3 -> Invalid)
                    // Should be treated as "Unspecified" and auto-placed.
                    // Since (1,1) is empty (Item 1 is at 1,1 0-based), it should go to (0,0).
                    Box(
                        Modifier.gridItem(row = -5, column = -5)
                            .size(sizeDp)
                            .saveLayoutInfo(dummy, pos2, latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Item 1 (Valid -1,-1): Bottom-Right (50, 50)
            assertEquals(Offset(size.toFloat(), size.toFloat()), pos1.value)

            // Item 2 (Invalid -5,-5): Auto-placed to first available slot (0,0)
            assertEquals(Offset(0f, 0f), pos2.value)
        }

    @Test
    fun testGrid_spanning() {
        val colSize = 50
        val rowSize = 50

        val positionedLatch = CountDownLatch(1)
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()

        show {
            Grid(
                config = {
                    repeat(3) { column(GridTrackSize.Fixed(colSize.toDp())) }
                    repeat(3) { row(GridTrackSize.Fixed(rowSize.toDp())) }
                }
            ) {
                // Item at R2, C2 spanning 2 rows and 2 columns
                // Should be at (50, 50) with size (100, 100)
                Box(
                    Modifier.gridItem(row = 2, column = 2, rowSpan = 2, columnSpan = 2)
                        .fillMaxSize()
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(colSize * 2, rowSize * 2), childSize.value)
        assertEquals(Offset(colSize.toFloat(), rowSize.toFloat()), childPosition.value)
    }

    @Test
    fun testGrid_spanEntireGrid() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(1)
            val itemSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(4) { column(GridTrackSize.Fixed(sizeDp)) }
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    // Spans all 4 columns
                    Box(
                        Modifier.gridItem(1, 1, columnSpan = 4)
                            .fillMaxSize()
                            .saveLayoutInfo(itemSize, Ref(), latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(size * 4, itemSize.value?.width)
        }

    @Test
    fun testGrid_respectsMinConstraints_expandsToFill() =
        with(density) {
            val smallTrackSize = 50.dp
            val largeParentSize = 100.dp
            val expectedSize = 100.dp.roundToPx()

            val positionedLatch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(smallTrackSize))
                        row(GridTrackSize.Fixed(smallTrackSize))
                    },
                    // Force the Grid to be larger than its content
                    modifier =
                        Modifier.size(largeParentSize).onGloballyPositioned { coordinates ->
                            gridSize.value = coordinates.size
                            positionedLatch.countDown()
                        },
                ) { /* empty */
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Grid should expand to satisfy min constraints",
                IntSize(expectedSize, expectedSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_respectsMaxConstraints_coercesSize() =
        with(density) {
            val largeTrackSize = 200.dp
            val smallParentSize = 100.dp
            val expectedSize = 100.dp.roundToPx()

            val positionedLatch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Wrap in Box with propagateMinConstraints=false to test pure max constraints
                Box(Modifier.size(smallParentSize)) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(largeTrackSize))
                            row(GridTrackSize.Fixed(largeTrackSize))
                        },
                        modifier =
                            Modifier.onGloballyPositioned { coordinates ->
                                gridSize.value = coordinates.size
                                positionedLatch.countDown()
                            },
                    ) { /* empty */
                    }
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Grid should respect max constraints even if tracks are larger",
                IntSize(expectedSize, expectedSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_respectsConstraints_whenContentOverflows() =
        with(density) {
            val parentSize = 100
            val contentSize = 200 // Larger than parent

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Parent container restricts size to 100x100
                Box(Modifier.size(parentSize.toDp())) {
                    Grid(
                        config = {
                            // Grid wants to be 200x200
                            column(GridTrackSize.Fixed(contentSize.toDp()))
                            row(GridTrackSize.Fixed(contentSize.toDp()))
                        },
                        modifier =
                            Modifier.onGloballyPositioned {
                                gridSize.value = it.size
                                latch.countDown()
                            },
                    ) {
                        Box(Modifier.gridItem(1, 1).fillMaxSize())
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Assert that Grid reported the PARENT'S size (clamped), not the content size
            assertEquals(
                "Grid should be clamped to parent max width/height",
                IntSize(parentSize, parentSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_respectsConstraints_whenContentUnderflows() =
        with(density) {
            val minSize = 200
            val contentSize = 50 // Smaller than parent min

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Parent enforces minimum size of 200x200 (e.g. fillMaxSize)
                Box(Modifier.requiredSize(minSize.toDp())) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(contentSize.toDp()))
                            row(GridTrackSize.Fixed(contentSize.toDp()))
                        },
                        modifier =
                            Modifier.fillMaxSize() // Request to fill parent
                                .onGloballyPositioned {
                                    gridSize.value = it.size
                                    latch.countDown()
                                },
                    ) {
                        Box(Modifier.gridItem(1, 1).fillMaxSize())
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Assert that Grid expanded to meet the minimum constraints
            assertEquals(
                "Grid should expand to meet min constraints",
                IntSize(minSize, minSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_percentageTrack_inIndefiniteContainer_fallbacksToAuto() =
        with(density) {
            val positionedLatch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Wrap in a Row to provide infinite width constraint
                Row {
                    Grid(
                        config = {
                            // 50% of Infinity cannot be calculated.
                            // Fallback to Auto (MaxContent) and fit the item.
                            column(GridTrackSize.Percentage(0.5f))
                            row(GridTrackSize.Fixed(50.dp))
                        },
                        modifier =
                            Modifier.onGloballyPositioned { coordinates ->
                                gridSize.value = coordinates.size
                                positionedLatch.countDown()
                            },
                    ) {
                        // The item is 10.dp wide. The track should expand to fit this.
                        Box(Modifier.gridItem(1, 1).size(10.dp))
                    }
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // Width should be 10.dp (Size of the content), NOT 0.
            // Height should be 50.dp (Fixed)
            assertEquals(IntSize(10.dp.roundToPx(), 50.dp.roundToPx()), gridSize.value)
        }

    @Test
    fun testGrid_zeroSizeTrack_layoutCorrectly() =
        with(density) {
            val size = 50
            val latch = CountDownLatch(2)
            val pos = Array(2) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size.toDp()))
                        column(GridTrackSize.Fixed(0.dp)) // Zero width column
                        column(GridTrackSize.Fixed(size.toDp()))
                        row(GridTrackSize.Fixed(size.toDp()))
                    }
                ) {
                    // Item 1: Col 1
                    Box(
                        Modifier.gridItem(1, 1)
                            .size(size.toDp())
                            .saveLayoutInfo(dummy, pos[0], latch)
                    )
                    // Item 2: Col 3 (Skipping Col 2 which is 0 width)
                    Box(
                        Modifier.gridItem(1, 3)
                            .size(size.toDp())
                            .saveLayoutInfo(dummy, pos[1], latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Item 1 at 0
            assertEquals(Offset(0f, 0f), pos[0].value)
            // Item 2 at 50 + 0 = 50
            assertEquals(Offset(size.toFloat(), 0f), pos[1].value)
        }

    @Test
    fun testGrid_zeroSizeChildren() {
        val trackSize = 50
        val latch = CountDownLatch(1)
        val gridSize = Ref<IntSize>()

        show {
            Grid(
                config = {
                    column(GridTrackSize.Fixed(trackSize.toDp()))
                    row(GridTrackSize.Fixed(trackSize.toDp()))
                },
                modifier =
                    Modifier.onGloballyPositioned {
                        gridSize.value = it.size
                        latch.countDown()
                    },
            ) {
                // Place a zero-sized item.
                // It should still occupy the logical cell (1,1), but draw nothing.
                // The Grid should still size itself to the Fixed tracks (50x50).
                Box(Modifier.gridItem(1, 1).size(0.dp))
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(trackSize, trackSize), gridSize.value)
    }

    @Test
    fun testGrid_itemFillsCell_whenRequested() {
        val trackSize = 100
        val latch = CountDownLatch(1)
        val childSize = Ref<IntSize>()

        show {
            Grid(
                config = {
                    column(GridTrackSize.Fixed(trackSize.toDp()))
                    row(GridTrackSize.Fixed(trackSize.toDp()))
                }
            ) {
                // Item has no intrinsic size, but requests fillMaxSize().
                // It should fill the definition of the track (100x100).
                Box(Modifier.gridItem(1, 1).fillMaxSize().saveLayoutInfo(childSize, Ref(), latch))
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(trackSize, trackSize), childSize.value)
    }

    @Test
    fun testGrid_gaps() =
        with(density) {
            val size = 50
            val gap = 10
            val gapDp = gap.toDp()
            val sizeDp = size.toDp()

            val positionedLatch = CountDownLatch(2)
            val childPosition = Array(2) { Ref<Offset>() }
            // Use explicit dummy refs instead of passing null to avoid overload ambiguity
            val dummySize = Array(2) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp))
                        column(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                        gap(gapDp)
                    }
                ) {
                    // Item 1: (0, 0)
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[0], childPosition[0], positionedLatch)
                    )
                    // Item 2: (50 + 10, 0) = (60, 0)
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), childPosition[0].value)
            assertEquals(Offset((size + gap).toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_flexWithGaps() =
        with(density) {
            val size = 50
            val gap = 10
            // Available space for tracks: 50 - 10 = 40.
            // 1.fr + 1.fr = 2 parts. 40 / 2 = 20 per track.
            val expectedColWidth = (size - gap) / 2

            val positionedLatch = CountDownLatch(2)
            val childSize = Array(2) { Ref<IntSize>() }
            val childPosition = Array(2) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Flex(1.fr))
                        column(GridTrackSize.Flex(1.fr))
                        row(GridTrackSize.Fixed(size.toDp()))
                        gap(gap.toDp())
                    },
                    modifier = Modifier.requiredSize(size.toDp()),
                ) {
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
            assertEquals(IntSize(expectedColWidth, size), childSize[0].value)
            assertEquals(IntSize(expectedColWidth, size), childSize[1].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)
            assertEquals(Offset((expectedColWidth + gap).toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_spanningWithGaps() {
        val size = 50
        val gap = 10
        // Spanning 2 columns: size + gap + size = 50 + 10 + 50 = 110
        val expectedWidth = size * 2 + gap

        val positionedLatch = CountDownLatch(1)
        val childSize = Ref<IntSize>()
        val dummyPos = Ref<Offset>()

        show {
            Grid(
                config = {
                    repeat(3) { column(GridTrackSize.Fixed(size.toDp())) }
                    row(GridTrackSize.Fixed(size.toDp()))
                    gap(gap.toDp())
                }
            ) {
                Box(
                    Modifier.gridItem(row = 1, column = 1, columnSpan = 2)
                        .fillMaxSize()
                        .saveLayoutInfo(childSize, dummyPos, positionedLatch)
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(expectedWidth, size), childSize.value)
    }

    @Test
    fun testGrid_gapPrecedence_specificOverridesGeneric() =
        with(density) {
            // Scenario:
            // gap(10) sets both.
            // rowGap(20) overrides row gap.
            // columnGap(5) overrides column gap.
            // Result: RowGap = 20, ColumnGap = 5.

            val size = 50
            val baseGap = 10
            val rowGapOverride = 20
            val colGapOverride = 5

            val sizeDp = size.toDp()
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(2) { column(GridTrackSize.Fixed(sizeDp)) }
                        repeat(2) { row(GridTrackSize.Fixed(sizeDp)) }

                        gap(baseGap.toDp()) // Sets both to 10
                        rowGap(rowGapOverride.toDp()) // Overrides row to 20
                        columnGap(colGapOverride.toDp()) // Overrides col to 5
                    }
                ) {
                    // (0,0)
                    Box(Modifier.gridItem(1, 1).size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // (0,1) -> X should be Size + ColGap(5)
                    Box(Modifier.gridItem(1, 2).size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                    // (1,0) -> Y should be Size + RowGap(20)
                    Box(Modifier.gridItem(2, 1).size(sizeDp).saveLayoutInfo(dummy, pos[2], latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset((size + colGapOverride).toFloat(), 0f), pos[1].value)
            assertEquals(Offset(0f, (size + rowGapOverride).toFloat()), pos[2].value)
        }

    @Test
    fun testGrid_alignment() =
        with(density) {
            val cellSize = 100
            val itemSize = 40
            // Center: (100 - 40) / 2 = 30
            val expectedOffset = 30f
            val cellSizeDp = cellSize.toDp()
            val itemSizeDp = itemSize.toDp()

            val positionedLatch = CountDownLatch(1)
            val childPosition = Ref<Offset>()
            val dummySize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(cellSizeDp))
                        row(GridTrackSize.Fixed(cellSizeDp))
                    }
                ) {
                    // Item smaller than cell, aligned center
                    Box(
                        Modifier.gridItem(1, 1, alignment = Alignment.Center)
                            .size(itemSizeDp)
                            .saveLayoutInfo(dummySize, childPosition, positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(expectedOffset, expectedOffset), childPosition.value)
        }

    @Test
    fun testGrid_alignment_spanning() =
        with(density) {
            val colSize = 50
            val rowSize = 60
            val gap = 10
            val itemSize = 40

            // Calculate total area dimensions including the gap
            val spannedWidth = (colSize * 2) + gap
            val spannedHeight = (rowSize * 2) + gap

            // Alignment.BottomEnd calculation:
            // x = ContainerWidth - ItemWidth
            // y = ContainerHeight - ItemHeight
            val expectedX = (spannedWidth - itemSize).toFloat()
            val expectedY = (spannedHeight - itemSize).toFloat()

            val positionedLatch = CountDownLatch(1)
            val childPosition = Ref<Offset>()
            val dummySize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(2) { column(GridTrackSize.Fixed(colSize.toDp())) }
                        repeat(2) { row(GridTrackSize.Fixed(rowSize.toDp())) }
                        gap(gap.toDp())
                    }
                ) {
                    Box(
                        Modifier.gridItem(
                                1,
                                1,
                                rowSpan = 2,
                                columnSpan = 2,
                                alignment = Alignment.BottomEnd,
                            )
                            .size(itemSize.toDp())
                            .saveLayoutInfo(dummySize, childPosition, positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
            assertEquals(Offset(expectedX, expectedY), childPosition.value)
        }

    @Test
    fun testGrid_alignment_rtl() =
        with(density) {
            val cellSize = 100
            val itemSize = 40
            val cellSizeDp = cellSize.toDp()
            val itemSizeDp = itemSize.toDp()

            val positionedLatch = CountDownLatch(2)
            val startAlignedPos = Ref<Offset>()
            val absLeftAlignedPos = Ref<Offset>()

            show {
                // Force RTL layout direction
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(cellSizeDp))
                            row(GridTrackSize.Fixed(cellSizeDp))
                        }
                    ) {
                        // 1. Alignment.TopStart (Relative 2D Alignment)
                        // In RTL, "Start" is visually on the RIGHT.
                        // Cell Width 100. Item 40.
                        // Expect visual position: 100 - 40 = 60px from visual Left.
                        Box(
                            Modifier.gridItem(1, 1, alignment = Alignment.TopStart)
                                .size(itemSizeDp)
                                .saveLayoutInfo(Ref(), startAlignedPos, positionedLatch)
                        )

                        // 2. AbsoluteAlignment.TopLeft (Absolute 2D Alignment)
                        // "Left" is always visually Left, regardless of layout direction.
                        // Expect visual position: 0px from visual Left.
                        Box(
                            // Use TopLeft (2D) instead of Left (1D) to satisfy the Alignment
                            // type requirement
                            Modifier.gridItem(
                                    1,
                                    1,
                                    alignment = androidx.compose.ui.AbsoluteAlignment.TopLeft,
                                )
                                .size(itemSizeDp)
                                .saveLayoutInfo(Ref(), absLeftAlignedPos, positionedLatch)
                        )
                    }
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // 1. Start (Right side of container)
            assertEquals(
                "Alignment.TopStart in RTL should be visually on the Right (60px from left)",
                Offset(60f, 0f),
                startAlignedPos.value,
            )

            // 2. Absolute Left (Left side of container)
            assertEquals(
                "AbsoluteAlignment.TopLeft in RTL should visually remain on the Left (0px)",
                Offset(0f, 0f),
                absLeftAlignedPos.value,
            )
        }

    @Test
    fun testGrid_nestedGrid() =
        with(density) {
            val outerSize = 100
            val outerSizeDp = outerSize.toDp()

            // Inner grid will be placed in a 100x100 cell.
            // It will have 2 columns of 50 each.
            val latch = CountDownLatch(1)
            val innerItemSize = Ref<IntSize>()

            show {
                // Outer Grid
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(outerSizeDp))
                        row(GridTrackSize.Fixed(outerSizeDp))
                    }
                ) {
                    // Inner Grid placed at (1,1) of Outer Grid
                    Grid(
                        modifier = Modifier.gridItem(1, 1).fillMaxSize(),
                        config = {
                            column(GridTrackSize.Flex(1.fr))
                            column(GridTrackSize.Flex(1.fr))
                            row(GridTrackSize.Flex(1.fr))
                        },
                    ) {
                        // Item inside Inner Grid (Col 1)
                        Box(
                            Modifier.gridItem(1, 1)
                                .fillMaxSize()
                                .saveLayoutInfo(innerItemSize, Ref(), latch)
                        )
                        // Item inside Inner Grid (Col 2) just to fill space
                        Box(Modifier.gridItem(1, 2).fillMaxSize())
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))
            assertEquals(IntSize(50, 100), innerItemSize.value)
        }

    @Test(expected = IllegalArgumentException::class)
    fun testGrid_invalidIndices_throws() {
        show {
            Grid(
                config = {
                    column(GridTrackSize.Fixed(10.dp))
                    row(GridTrackSize.Fixed(10.dp))
                }
            ) {
                Box(Modifier.gridItem(100000, 1)) // Out of bounds
            }
        }
    }

    @Composable
    private fun IntrinsicItem(
        minWidth: Int,
        minIntrinsicWidth: Int,
        maxIntrinsicWidth: Int,
        modifier: Modifier = Modifier,
    ) {
        Layout(
            modifier,
            measurePolicy =
                object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints,
                    ): MeasureResult {
                        return layout(
                            constraints.minWidth.coerceAtLeast(minWidth),
                            constraints.minHeight,
                        ) {}
                    }

                    override fun IntrinsicMeasureScope.minIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int,
                    ) = minIntrinsicWidth

                    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int,
                    ) = maxIntrinsicWidth
                },
        )
    }
}
