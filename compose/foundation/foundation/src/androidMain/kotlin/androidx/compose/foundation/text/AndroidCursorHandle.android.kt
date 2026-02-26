/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.HandlePopup
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.OffsetProvider
import androidx.compose.foundation.text.selection.SelectionHandleAnchor
import androidx.compose.foundation.text.selection.SelectionHandleInfo
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
import androidx.compose.foundation.text.selection.createHandleImage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

private const val Sqrt2 = 1.41421356f
internal val CursorHandleHeight = 25.dp
internal val CursorHandleWidth = CursorHandleHeight * 2f / (1 + Sqrt2)

@Composable
internal actual fun CursorHandle(
    offsetProvider: OffsetProvider,
    modifier: Modifier,
    minTouchTargetSize: DpSize,
) {
    val finalModifier =
        modifier.semantics {
            this[SelectionHandleInfoKey] =
                SelectionHandleInfo(
                    handle = Handle.Cursor,
                    position = offsetProvider.provide(),
                    anchor = SelectionHandleAnchor.Middle,
                    visible = true,
                )
        }
    HandlePopup(positionProvider = offsetProvider, handleReferencePoint = Alignment.TopCenter) {
        if (minTouchTargetSize.isSpecified) {
            Box(
                modifier =
                    finalModifier.requiredSizeIn(
                        minWidth = minTouchTargetSize.width,
                        minHeight = minTouchTargetSize.height,
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                DefaultCursorHandle()
            }
        } else {
            DefaultCursorHandle(finalModifier)
        }
    }
}

@Composable
/*@VisibleForTesting*/
private fun DefaultCursorHandle(modifier: Modifier = Modifier) {
    Spacer(modifier.size(CursorHandleWidth, CursorHandleHeight).drawCursorHandle())
}

private fun Modifier.drawCursorHandle() = this then DrawCursorHandleElement()

private class DrawCursorHandleElement : ModifierNodeElement<DrawCursorHandleModifierNode>() {
    override fun create() = DrawCursorHandleModifierNode()

    override fun update(node: DrawCursorHandleModifierNode) {}

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        /* none */
    }
}

private class DrawCursorHandleModifierNode :
    DelegatingNode(), CompositionLocalConsumerModifierNode {
    private var drawNode: DrawModifierNode? = null

    private val buildDrawCache: CacheDrawScope.() -> DrawResult = {
        // Cursor handle is the same as a SelectionHandle rotated 45 degrees clockwise.
        val radius = size.width / 2f
        val imageBitmap = createHandleImage(radius = radius)
        // Note that the read of currentValueOf(LocalTextSelectionColors) is snapshot-backed and
        //  will invalidate draw when changed.
        val colorFilter = ColorFilter.tint(currentValueOf(LocalTextSelectionColors).handleColor)
        onDrawWithContent {
            drawContent()
            withTransform({
                translate(left = radius)
                rotate(degrees = 45f, pivot = Offset.Zero)
            }) {
                drawImage(image = imageBitmap, colorFilter = colorFilter)
            }
        }
    }

    override val shouldAutoInvalidate = false

    override fun onAttach() {
        super.onAttach()
        drawNode = delegate(CacheDrawModifierNode(buildDrawCache))
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onReset() {
        super.onReset()
        drawNode = null
    }
}
