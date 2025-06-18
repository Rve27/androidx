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

package androidx.xr.glimmer.list

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.xr.glimmer.requirePrecondition
import androidx.xr.glimmer.requirePreconditionNotNull

internal class GlimmerListMeasuredListItem(
    override val index: Int,
    override val key: Any,
    override val contentType: Any?,
    private val placeables: List<Placeable>,
    private val layoutProperties: ListLayoutProperties,
    private val itemsCount: Int,
) : LazyListItemInfo {

    override var offset: Int = 0
        private set

    /** Sum of the main axis sizes of all the inner placeables. */
    override val size: Int

    /** Sum of the main axis sizes of all the inner placeables and [spacing]. */
    val mainAxisSizeWithSpacings: Int

    /** Max of the cross axis sizes of all the inner placeables. */
    val crossAxisSize: Int

    private var mainAxisLayoutSize: Int = -1
    private var minMainAxisOffset: Int = 0
    private var maxMainAxisOffset: Int = 0
    private val isVertical: Boolean
        get() = layoutProperties.isVertical

    private val spacing: Int
        get() = if (index == itemsCount - 1) 0 else layoutProperties.spaceBetweenItems

    // optimized for storing x and y offsets for each placeable one by one.
    // array's size == placeables.size * 2, first we store x, then y.
    private val placeableOffsets: IntArray

    init {
        var mainAxisSize = 0
        var maxCrossAxis = 0
        placeables.fastForEach {
            mainAxisSize += if (isVertical) it.height else it.width
            maxCrossAxis = maxOf(maxCrossAxis, if (!isVertical) it.height else it.width)
        }
        size = mainAxisSize
        mainAxisSizeWithSpacings = (size + spacing).coerceAtLeast(0)
        crossAxisSize = maxCrossAxis
        placeableOffsets = IntArray(placeables.size * 2)
    }

    /**
     * Calculates positions for the inner placeables at [mainAxisOffset] main axis position. If
     * [reverseOrder] is true the inner placeables would be placed in the inverted order.
     */
    fun position(mainAxisOffset: Int, layoutWidth: Int, layoutHeight: Int) {
        this.offset = mainAxisOffset
        mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
        @Suppress("NAME_SHADOWING") var mainAxisOffset = mainAxisOffset
        placeables.fastForEachIndexed { index, placeable ->
            val indexInArray = index * 2
            if (isVertical) {
                placeableOffsets[indexInArray] =
                    requirePreconditionNotNull(layoutProperties.horizontalAlignment) {
                            "null horizontalAlignment when isVertical == true"
                        }
                        .align(placeable.width, layoutWidth, layoutProperties.layoutDirection)
                placeableOffsets[indexInArray + 1] = mainAxisOffset
                mainAxisOffset += placeable.height
            } else {
                placeableOffsets[indexInArray] = mainAxisOffset
                placeableOffsets[indexInArray + 1] =
                    requirePreconditionNotNull(layoutProperties.verticalAlignment) {
                            "null verticalAlignment when isVertical == false"
                        }
                        .align(placeable.height, layoutHeight)
                mainAxisOffset += placeable.width
            }
        }
        minMainAxisOffset = -layoutProperties.beforeContentPadding
        maxMainAxisOffset = mainAxisLayoutSize + layoutProperties.afterContentPadding
    }

    fun place(scope: Placeable.PlacementScope) =
        with(scope) {
            requirePrecondition(mainAxisLayoutSize != -1) { "position() should be called first" }
            repeat(placeables.size) { index ->
                val placeable = placeables[index]
                var offset = getOffset(index)

                if (layoutProperties.reverseLayout) {
                    offset =
                        offset.copy { mainAxisOffset ->
                            mainAxisLayoutSize - mainAxisOffset - placeable.mainAxisSize
                        }
                }
                offset += layoutProperties.visualOffset
                if (isVertical) {
                    placeable.placeWithLayer(offset)
                } else {
                    placeable.placeRelativeWithLayer(offset)
                }
            }
        }

    private fun getOffset(index: Int) =
        IntOffset(placeableOffsets[index * 2], placeableOffsets[index * 2 + 1])

    private val Placeable.mainAxisSize
        get() = if (isVertical) height else width

    private inline fun IntOffset.copy(mainAxisMap: (Int) -> Int): IntOffset =
        if (isVertical) IntOffset(x, mainAxisMap(y)) else IntOffset(mainAxisMap(x), y)
}
