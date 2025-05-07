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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.coroutineScope

/**
 * A [Modifier] that enables dragging to resize a pane.
 *
 * @param state The [DragToResizeState] which controls the resizing behavior.
 */
internal fun Modifier.dragToResize(state: DragToResizeState): Modifier =
    this.draggable(state = state, orientation = state.orientation).then(DragToResizeElement(state))

private class DragToResizeElement(
    val state: DragToResizeState,
) : ModifierNodeElement<DragToResizeNode>() {
    override fun create(): DragToResizeNode = DragToResizeNode(state)

    override fun update(node: DragToResizeNode) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragToResize"
        properties["state"] = state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DragToResizeElement) return false

        if (state != other.state) return false

        return true
    }
}

private class DragToResizeNode(
    var state: DragToResizeState,
) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? PaneScaffoldParentDataImpl) ?: PaneScaffoldParentDataImpl()).also {
            it.dragToResizeState = state
        }
}

/**
 * Represents the edge of a resizable pane that is docked, i.e. the edge that will stay in the same
 * position during resizing.
 *
 * For example if the edge is [DockedEdge.Top], the top edge of the pane won't move and the bottom
 * edge will be moveable to resize the pane.
 */
internal enum class DockedEdge {
    /** The top edge of the pane is fixed, and resizing happens by moving the bottom edge. */
    Top,
    /** The bottom edge of the pane is fixed, and resizing happens by moving the top edge. */
    Bottom,
    /** The start edge of the pane is fixed, and resizing happens by moving the end edge. */
    Start,
    /** The end edge of the pane is fixed, and resizing happens by moving the start edge. */
    End
}

/**
 * Creates and remembers a [DragToResizeState] instance.
 *
 * This function is used to create a state object that can be used to track and control the resizing
 * behavior of a composable element via dragging. The state is saved and restored across
 * recompositions and configuration changes using [rememberSaveable].
 *
 * @param dockedEdge The edge to which the element is docked. This determines the orientation of the
 *   resizing operation (horizontal or vertical) and the direction of the size change when dragging.
 */
@Composable
internal fun rememberDragToResizeState(dockedEdge: DockedEdge): DragToResizeState {
    val layoutDirection = LocalLayoutDirection.current
    return rememberSaveable(saver = DragToResizeState.Saver(dockedEdge, layoutDirection)) {
        DragToResizeState(dockedEdge, layoutDirection)
    }
}

private fun DragToResizeState(
    dockedEdge: DockedEdge,
    layoutDirection: LayoutDirection
): DragToResizeState =
    when (dockedEdge) {
        DockedEdge.Top -> DragToResizeState.Top()
        DockedEdge.Bottom -> DragToResizeState.Bottom()
        DockedEdge.Start ->
            if (layoutDirection == LayoutDirection.Ltr) {
                DragToResizeState.Left()
            } else {
                DragToResizeState.Right()
            }
        DockedEdge.End ->
            if (layoutDirection == LayoutDirection.Ltr) {
                DragToResizeState.Right()
            } else {
                DragToResizeState.Left()
            }
    }

/**
 * A state object that can be used to control the resizing behavior of a pane.
 *
 * This class provides a way to track the current size of a resizable pane and to handle drag
 * interactions that modify the size. It supports both horizontal and vertical resizing.
 *
 * This state object is primarily designed for internal use within pane scaffolds.
 *
 * @see androidx.compose.material3.adaptive.layout.dragToResize
 */
@Stable
internal abstract class DragToResizeState private constructor() : DraggableState {
    internal var size: Float by mutableFloatStateOf(Float.NaN)

    internal abstract val orientation: Orientation

    internal open fun getDraggedWidth(measuringWidth: Int, widthRange: IntRange) = measuringWidth

    internal open fun getDraggedHeight(measuringHeight: Int, heightRange: IntRange) =
        measuringHeight

    protected abstract val sizeRange: ClosedFloatingPointRange<Float>

    protected open fun convertDelta(delta: Float) = delta

    protected var isDragged: Boolean = false
    protected var widthRange: ClosedFloatingPointRange<Float> = 0f..0f
    protected var heightRange: ClosedFloatingPointRange<Float> = 0f..0f

    private val dragMutex = MutatorMutex()

    private val dragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
        }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) =
        coroutineScope {
            dragMutex.mutateWith(dragScope, dragPriority, block)
        }

    override fun dispatchRawDelta(delta: Float) {
        if (Snapshot.withoutReadObservation { size.isNaN() }) {
            return
        }
        val actualDelta = convertDelta(delta)
        size = (size + actualDelta).coerceIn(sizeRange)
        isDragged = true
    }

    internal abstract class Horizontal : DragToResizeState() {
        override val sizeRange
            get() = widthRange

        override val orientation: Orientation = Orientation.Horizontal

        override fun getDraggedWidth(measuringWidth: Int, widthRange: IntRange): Int {
            this.widthRange = widthRange.toFloatRange()
            if (size.isNaN() || !isDragged) {
                size = measuringWidth.toFloat()
                return measuringWidth
            }
            dispatchRawDelta(0f) // To re-coerce the size
            return size.toInt()
        }
    }

    internal abstract class Vertical : DragToResizeState() {
        override val sizeRange
            get() = heightRange

        override val orientation: Orientation = Orientation.Vertical

        override fun getDraggedHeight(measuringHeight: Int, heightRange: IntRange): Int {
            this.heightRange = heightRange.toFloatRange()
            if (size.isNaN() || !isDragged) {
                size = measuringHeight.toFloat()
                return measuringHeight
            }
            dispatchRawDelta(0f) // To re-coerce the size
            return size.toInt()
        }
    }

    internal class Left : Horizontal()

    internal class Right : Horizontal() {
        override fun convertDelta(delta: Float) = -delta
    }

    internal class Top : Vertical()

    internal class Bottom : Vertical() {
        override fun convertDelta(delta: Float) = -delta
    }

    companion object {
        internal fun Saver(
            dockedEdge: DockedEdge,
            layoutDirection: LayoutDirection
        ): Saver<DragToResizeState, *> =
            Saver(
                save = {
                    listOf(
                        it.size,
                        it.isDragged,
                        it.widthRange.start,
                        it.widthRange.endInclusive,
                        it.heightRange.start,
                        it.heightRange.endInclusive,
                    )
                },
                restore = {
                    DragToResizeState(dockedEdge, layoutDirection).also { state ->
                        state.size = it[0] as Float
                        state.isDragged = it[1] as Boolean
                        state.widthRange = (it[2] as Float)..(it[3] as Float)
                        state.heightRange = (it[4] as Float)..(it[5] as Float)
                    }
                }
            )
    }
}

private fun IntRange.toFloatRange() = first.toFloat()..last.toFloat()
