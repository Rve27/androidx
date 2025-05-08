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

package androidx.xr.glimmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A surface is a fundamental building block in Glimmer. A surface represents a distinct visual area
 * or 'physical' boundary for components such as buttons and cards. A surface is responsible for:
 * 1) Clipping: a surface clips its children to the shape specified by [shape]
 * 2) Border: a surface draws an inner [border] to emphasize the boundary of the component
 * 3) Background: a surface has a background color of [color]. Content inside surface such as text
 *    and icons should use the corresponding 'on' color from [GlimmerTheme.colors].
 *
 * @sample androidx.xr.glimmer.samples.SurfaceSample
 * @param shape the [Shape] used to clip this surface, and also used to draw the background and
 *   border
 * @param color the background [Color] for this surface
 * @param border an optional inner border for this surface
 */
@Composable
public fun Modifier.surface(
    shape: Shape = SurfaceDefaults.Shape,
    color: Color = GlimmerTheme.colors.surface,
    border: BorderStroke? = SurfaceDefaults.border()
): Modifier =
    this.clip(shape)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .background(color = color, shape = shape)
        .then(SurfaceSemantics)

/** Default values used for [surface]. */
public object SurfaceDefaults {
    /** The default [Shape] used for a [surface] */
    public val Shape: Shape = RoundedCornerShape(40.dp)

    /**
     * Create the default [BorderStroke] used for a [surface]. Use the other overload in order to
     * change the width or color.
     */
    @Composable
    public fun border(): BorderStroke {
        return GlimmerTheme.LocalGlimmerTheme.current.defaultSurfaceBorder
    }

    /**
     * Create the default [BorderStroke] used for a [surface], with optional overrides for [width]
     * and [color].
     *
     * @param width width of the border in [Dp]. Use [Dp.Hairline] for one-pixel border.
     * @param color color to paint the border with
     */
    @Composable
    public fun border(
        width: Dp = DefaultSurfaceBorderWidth,
        color: Color = GlimmerTheme.colors.outline
    ): BorderStroke {
        return BorderStroke(width, color)
    }

    /** Returns the default (cached) border for a [surface]. */
    internal val GlimmerTheme.defaultSurfaceBorder: BorderStroke
        get() {
            return defaultSurfaceBorderCached
                ?: BorderStroke(DefaultSurfaceBorderWidth, colors.outline).also {
                    defaultSurfaceBorderCached = it
                }
        }
}

/** Default border width for a [surface]. */
private val DefaultSurfaceBorderWidth = 3.dp

/**
 * Default semantics for a surface. Extracted to a separate val to avoid re-allocating a lambda, and
 * causing modifier inequality.
 */
private val SurfaceSemantics =
    Modifier.semantics(mergeDescendants = false) { isTraversalGroup = true }
