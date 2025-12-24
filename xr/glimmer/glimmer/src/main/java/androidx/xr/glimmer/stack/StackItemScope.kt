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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.xr.glimmer.DepthNode
import androidx.xr.glimmer.GlimmerTheme.Companion.LocalGlimmerTheme
import kotlin.math.abs

/** Receiver scope used by item content in [VerticalStack]. */
@Stable
public sealed interface StackItemScope {

    /**
     * Adds a decoration shape for this item, which is used in transition animations (to clip items
     * behind) and depth effect. Each item must have its decoration shape set, as otherwise the
     * clipping and depth will not be applied.
     *
     * @param shape The shape of this stack item.
     */
    public fun Modifier.itemDecoration(shape: Shape): Modifier =
        this then ItemDecorationElement(this@StackItemScope as StackItemScopeImpl, shape)
}

internal class ItemDecorationElement
internal constructor(private val stackItemScope: StackItemScopeImpl, private val shape: Shape) :
    ModifierNodeElement<ItemDecorationNode>() {

    override fun create(): ItemDecorationNode = ItemDecorationNode(stackItemScope, shape)

    override fun update(node: ItemDecorationNode) {
        node.update(stackItemScope, shape)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemDecorationElement) return false

        if (stackItemScope != other.stackItemScope) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stackItemScope.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "stackItemDecoration"
        properties["shape"] = shape
    }
}

internal class StackItemScopeImpl(internal val state: StackState) : StackItemScope {

    internal var index = -1
    internal val decorations = mutableListOf<ItemDecorationNode>()
    internal var coordinates: LayoutCoordinates? = null

    internal var maskOffset by mutableStateOf<Offset?>(null)
    internal var maskOutline by mutableStateOf<Outline?>(null)

    internal fun recalculateMask() {
        var selectedDecoration: ItemDecorationNode? = null
        var selectedDecorationWidth = 0f
        var selectedDecorationHeight = 0f

        decorations.fastForEach { decoration ->
            val bounds = decoration.outline?.bounds ?: return@fastForEach
            if (selectedDecoration == null || bounds.width > selectedDecorationWidth) {
                selectedDecoration = decoration
                selectedDecorationWidth = bounds.width
                selectedDecorationHeight = bounds.height
            } else if (
                abs(bounds.width - selectedDecorationWidth) < 0.01f &&
                    (decoration.offset.y + bounds.height / 2) >
                        (selectedDecoration.offset.y + selectedDecorationHeight / 2)
            ) {
                // In case of equal width decorations, select the one with a lower center.
                selectedDecoration = decoration
            }
        }

        maskOutline = selectedDecoration?.outline
        maskOffset = selectedDecoration?.offset
    }
}

internal class ItemDecorationNode(
    private var stackItemScope: StackItemScopeImpl,
    private var shape: Shape,
) :
    DelegatingNode(),
    LayoutAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    DrawModifierNode {

    internal var size = Size.Zero
    internal var outline: Outline? = null
    internal var offset: Offset = Offset.Zero

    private var depthNode: DepthNode? = null
    private var coordinates: LayoutCoordinates? = null

    override fun onAttach() {
        depthNode = delegate(DepthNode(currentValueOfDepth(), shape))
        stackItemScope.decorations.add(this)
        if (size != Size.Zero) {
            // If this node is reused, update the decoration in case there is no remeasure.
            updateDecoration()
        }
    }

    override fun onRemeasured(size: IntSize) {
        this.size = size.toSize()
        updateDecoration()
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        offset = calculateDecorationOffset()
        stackItemScope.recalculateMask()
    }

    override fun onDetach() {
        stackItemScope.decorations.remove(this)
        depthNode?.let { undelegate(it) }
    }

    override fun ContentDrawScope.draw() {
        val index = stackItemScope.index
        if (index == -1) return
        val state = stackItemScope.state
        val topItem = state.topItem
        val offsetFraction = state.topItemOffsetFraction

        val contentAlpha =
            calculateContentAlpha(index = index, topItem = topItem, offsetFraction = offsetFraction)

        // Apply alpha to the depth separately from the content so that the shadows are not clipped.
        depthNode?.apply { drawDepth(alpha = contentAlpha) }

        // Draw item content with a scrim based on the current alpha.
        drawContent()

        val scrimAlpha =
            calculateScrimAlpha(index = index, topItem = topItem, offsetFraction = offsetFraction)
        val scrimColor = getScrimColor(index = index, topItem = topItem)
        val outline = getDecorationOutline()
        scrimColor?.let {
            // If there is a scrim color, apply it on top of the item.
            drawOutline(outline = outline, color = it, alpha = scrimAlpha)
        }

        if (contentAlpha < 1f) {
            drawOutline(
                outline = outline,
                blendMode = BlendMode.DstOut,
                color = Color.Black,
                alpha = 1f - contentAlpha,
            )
        }
    }

    fun update(stackItemScope: StackItemScopeImpl, shape: Shape) {
        depthNode?.update(currentValueOfDepth(), shape)
        if (this.stackItemScope != stackItemScope || this.shape != shape) {
            this.stackItemScope.decorations.remove(this)
            stackItemScope.decorations.add(this)
            this.stackItemScope = stackItemScope
            this.shape = shape
            updateDecoration()
        }
    }

    private fun ContentDrawScope.getDecorationOutline(): Outline {
        outline?.let {
            return it
        }
        return shape.createOutline(size, layoutDirection, this).also { outline = it }
    }

    private fun updateDecoration() {
        if (size == Size.Zero || !isAttached) return

        offset = calculateDecorationOffset()
        outline = createOutline()

        stackItemScope.recalculateMask()
    }

    private fun createOutline(): Outline {
        val density = currentValueOf(LocalDensity)
        val layoutDirection = currentValueOf(LocalLayoutDirection)
        return shape.createOutline(size, layoutDirection, density)
    }

    private fun calculateDecorationOffset(): Offset =
        coordinates?.let { decorationCoordinates ->
            stackItemScope.coordinates?.localPositionOf(
                sourceCoordinates = decorationCoordinates,
                relativeToSource = Offset.Zero,
            )
        } ?: Offset.Zero

    private fun calculateContentAlpha(index: Int, topItem: Int, offsetFraction: Float): Float =
        when {
            index.isTopItem(topItem = topItem) -> 1f - offsetFraction
            index.isNextItem(topItem = topItem) -> 1f
            index.isNextNextItem(topItem = topItem) -> offsetFraction
            else -> 0f
        }

    private fun calculateScrimAlpha(index: Int, topItem: Int, offsetFraction: Float): Float =
        when {
            index.isNextItem(topItem = topItem) -> (1f - offsetFraction) * MaxItemScrimAlpha
            index.isNextNextItem(topItem = topItem) -> MaxItemScrimAlpha
            else -> 0f
        }

    private fun getScrimColor(index: Int, topItem: Int): Color? =
        when {
            index.isNextItem(topItem = topItem) -> SurfaceLow
            index.isNextNextItem(topItem = topItem) -> SurfaceLow
            else -> null
        }

    private fun currentValueOfDepth() = currentValueOf(LocalGlimmerTheme).depthLevels.level1
}

/** Returns whether the index is of the top of the stack item. */
internal fun Int.isTopItem(topItem: Int) = this == topItem

/** Returns whether the index is of the next item that follows the top of the stack item. */
internal fun Int.isNextItem(topItem: Int) = this == topItem + 1

/** Returns whether the index is of the next-next item after the top of the stack item. */
internal fun Int.isNextNextItem(topItem: Int) = this == topItem + 2

private val SurfaceLow = Color(0xFF4F4F4F)
private val MaxItemScrimAlpha = 0.5f
