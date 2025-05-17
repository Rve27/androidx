/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

private val Items = (0..100).toList().map { it.toString() }

/** A simple Layout that will place items right to left with scrolling support. */
@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
@Preview
fun LazyLayoutScrollableSample() {
    /** Saves the deltas from the scroll gesture. */
    var scrollAmount by remember { mutableFloatStateOf(0f) }

    /**
     * Lazy Layout measurement starts at a known position identified by the first item's position.
     */
    var firstVisibleItemIndex = remember { 0 }
    var firstVisibleItemOffset = remember { 0 }

    // A scrollable state is needed for scroll support
    val scrollableState = rememberScrollableState { delta ->
        scrollAmount += delta
        delta // assume we consumed everything.
    }

    // Create an item provider
    val itemProvider = remember {
        {
            object : LazyLayoutItemProvider {
                override val itemCount: Int
                    get() = Items.size

                @Composable
                override fun Item(index: Int, key: Any) {
                    MyLazyLayoutContent(index, Items[index])
                }
            }
        }
    }

    LazyLayout(
        modifier = Modifier.size(500.dp).scrollable(scrollableState, Orientation.Horizontal),
        itemProvider = itemProvider,
    ) { constraints ->
        // plug the measure policy, this is how we create and layout items.
        val placeablesCache = mutableIntObjectMapOf<Placeable>()
        measureLayout(
            scrollAmount, // will trigger a re-measure when it changes.
            firstVisibleItemIndex,
            firstVisibleItemOffset,
            Items.size,
            placeablesCache,
            constraints,
        ) { newFirstVisibleItemIndex, newFirstVisibleItemOffset ->
            // update the information about the anchor item.
            firstVisibleItemIndex = newFirstVisibleItemIndex
            firstVisibleItemOffset = newFirstVisibleItemOffset

            // resets the scrolling state
            scrollAmount = 0f
        }
    }
}

@Composable
private fun MyLazyLayoutContent(index: Int, item: String) {
    Box(
        modifier =
            Modifier.width(100.dp)
                .height(100.dp)
                .background(color = if (index % 2 == 0) Color.Red else Color.Green)
    ) {
        Text(text = item)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyLayoutMeasureScope.measureLayout(
    scrollAmount: Float,
    firstVisibleItemIndex: Int,
    firstVisibleItemOffset: Int,
    itemCount: Int,
    placeablesCache: MutableIntObjectMap<Placeable>,
    containerConstraints: Constraints,
    updatePositions: (Int, Int) -> Unit,
): MeasureResult {
    /** 1) Resolve layout information and constraints. */
    val viewportSize = containerConstraints.maxWidth
    // children are only restricted on the cross axis size.
    val childConstraints = Constraints(maxWidth = Constraints.Infinity, maxHeight = viewportSize)

    /** 2) Initialize data holders for the pass. */
    // All items that will be placed in this layout in the layout pass.
    val placeables = mutableListOf<Pair<Placeable, Int>>()
    // The anchor points, we start from a known position, the position of the first item.
    var currentFirstVisibleItemIndex = firstVisibleItemIndex
    var currentFirstVisibleItemOffset = firstVisibleItemOffset
    // represents the real amount of scroll we applied as a result of this measure pass.
    val scrollDelta = scrollAmount.fastRoundToInt()
    // The amount of space available to items.
    val maxOffset = containerConstraints.maxWidth
    // tallest item from the ones we've created in this layout.This will determined the cross axis
    // size of this layout
    var maxCrossAxis = 0

    /** 3) Apply Scroll */
    // applying the whole requested scroll offset.
    currentFirstVisibleItemOffset -= scrollDelta

    // if the current scroll offset is less than minimally possible we reset. Imagine we've reached
    // the bounds at the start of the layout and we tried to scroll back.
    if (currentFirstVisibleItemIndex == 0 && currentFirstVisibleItemOffset < 0) {
        currentFirstVisibleItemOffset = 0
    }

    /** 4) Consider we scrolled back */
    while (currentFirstVisibleItemOffset < 0 && currentFirstVisibleItemIndex > 0) {
        val previous = currentFirstVisibleItemIndex - 1
        val measuredItem = createItem(previous, childConstraints, placeablesCache)
        placeables.add(0, measuredItem to currentFirstVisibleItemOffset)
        maxCrossAxis = maxOf(maxCrossAxis, measuredItem.height)
        currentFirstVisibleItemOffset += measuredItem.width
        currentFirstVisibleItemIndex = previous
    }

    // if we were scrolled backward, but there were not enough items before. this means
    // not the whole scroll was consumed
    if (currentFirstVisibleItemOffset < 0) {
        val notConsumedScrollDelta = -currentFirstVisibleItemOffset
        currentFirstVisibleItemOffset = 0
    }

    /** 5) Compose forward. */
    var index = currentFirstVisibleItemIndex
    var currentMainAxisOffset = -currentFirstVisibleItemOffset

    // first we need to skip items we already composed while composing backward
    var indexInVisibleItems = 0
    while (indexInVisibleItems < placeables.size) {
        if (currentMainAxisOffset >= maxOffset) {
            // this item is out of the bounds and will not be visible.
            placeables.removeAt(indexInVisibleItems)
        } else {
            index++
            currentMainAxisOffset += placeables[indexInVisibleItems].first.width
            indexInVisibleItems++
        }
    }

    // then composing visible items forward until we fill the whole viewport.
    while (
        index < itemCount &&
            (currentMainAxisOffset < maxOffset ||
                currentMainAxisOffset <= 0 ||
                placeables.isEmpty())
    ) {
        val measuredItem = createItem(index, childConstraints, placeablesCache)
        val measuredItemPosition = currentMainAxisOffset

        currentMainAxisOffset += measuredItem.width

        if (currentMainAxisOffset <= 0 && index != itemCount - 1) {
            // this item is offscreen and will not be visible. advance firstVisibleItemIndex
            currentFirstVisibleItemIndex = index + 1
            currentFirstVisibleItemOffset -= measuredItem.width
        } else {
            maxCrossAxis = maxOf(maxCrossAxis, measuredItem.height)
            placeables.add(measuredItem to measuredItemPosition)
        }
        index++
    }

    val layoutWidth = containerConstraints.constrainWidth(currentMainAxisOffset)
    val layoutHeight = containerConstraints.constrainHeight(maxCrossAxis)

    /** 7) Update state information. */
    updatePositions(currentFirstVisibleItemIndex, currentFirstVisibleItemOffset)

    /** 8) Perform layout. */
    return layout(layoutWidth, layoutHeight) {
        // since this is a linear list all items are placed on the same cross-axis position
        for ((placeable, position) in placeables) {
            placeable.place(position, 0)
        }
    }
}

private fun LazyLayoutMeasureScope.createItem(
    index: Int,
    constraints: Constraints,
    placeablesCache: IntObjectMap<Placeable>,
): Placeable {

    val cachedPlaceable = placeablesCache[index]
    return if (cachedPlaceable == null) {
        val measurables = compose(index)
        require(measurables.size == 1) { "Only one composable item emission is supported." }
        measurables[0].measure(constraints)
    } else {
        cachedPlaceable
    }
}

private class BasicLazyLayoutState : ScrollableState {
    var firstVisibleItemIndex = 0
        private set

    var firstVisibleItemOffset = 0
        private set

    var scrollAmount by mutableFloatStateOf(0.0f)
        private set

    private val backingState = ScrollableState { performScroll(it) }

    private fun performScroll(delta: Float): Float {
        scrollAmount += delta
        return delta // assume we consumed everything
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) = backingState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float): Float = backingState.dispatchRawDelta(delta)

    fun updatePositions(newFirstVisibleItemIndex: Int, newFirstVisibleItemOffset: Int) {
        firstVisibleItemIndex = newFirstVisibleItemIndex
        firstVisibleItemOffset = newFirstVisibleItemOffset
        scrollAmount = 0.0f // reset scroll
    }

    override val isScrollInProgress: Boolean
        get() = backingState.isScrollInProgress
}

private class BasicLazyLayoutItemProvider(
    private val items: List<String>,
    private val content: @Composable (item: String, index: Int) -> Unit,
) : LazyLayoutItemProvider {
    override val itemCount: Int = items.size

    @Composable
    override fun Item(index: Int, key: Any) {
        content.invoke(items[index], index)
    }
}
