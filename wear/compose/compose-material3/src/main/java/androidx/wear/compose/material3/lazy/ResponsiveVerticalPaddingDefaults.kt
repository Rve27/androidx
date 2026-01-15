/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3.lazy

import androidx.compose.ui.unit.Dp
import androidx.wear.compose.foundation.lazy.ResponsiveVerticalPadding

/**
 * Contains the default responsive vertical padding values for Wear Material 3 components.
 *
 * These defaults are intended to be used with [Modifier.responsiveVerticalPadding] in a
 * [TransformingLazyColumn] to ensure items at the top and bottom of the list have visual spacing
 * consistent with Wear OS design guidelines.
 *
 * @sample androidx.wear.compose.material3.samples.TransformingLazyColumnResponsivePaddingSample
 */
public object ResponsiveVerticalPaddingDefaults {
    /** Symmetric 23% padding (Large) */
    private val Large = responsiveVerticalPadding { containerHeight ->
        containerHeight * LargePaddingFraction
    }

    /** Symmetric 13% padding (Small) */
    private val Small = responsiveVerticalPadding { containerHeight ->
        containerHeight * SmallPaddingFraction
    }

    /** Asymmetric padding: 13% Top, 23% Bottom */
    private val Asymmetric =
        responsiveVerticalPadding(
            calculateTop = { height -> height * SmallPaddingFraction },
            calculateBottom = { height -> height * LargePaddingFraction },
        )

    /** Responsive vertical padding for [androidx.wear.compose.material3.Button]. */
    public val Button: ResponsiveVerticalPadding = Large
    /** Responsive vertical padding for [androidx.wear.compose.material3.ButtonGroup]. */
    public val ButtonGroup: ResponsiveVerticalPadding = Large
    /**
     * Responsive vertical padding for [androidx.wear.compose.material3.Card] and
     * [androidx.wear.compose.material3.TitleCard].
     */
    public val Card: ResponsiveVerticalPadding = Large
    /** Responsive vertical padding for [androidx.wear.compose.material3.CompactButton]. */
    public val CompactButton: ResponsiveVerticalPadding = Small
    /** Responsive vertical padding for [androidx.wear.compose.material3.IconButton]. */
    public val IconButton: ResponsiveVerticalPadding = Small
    /** Responsive vertical padding for [androidx.wear.compose.material3.TextButton]. */
    public val TextButton: ResponsiveVerticalPadding = Small

    /** Responsive vertical padding for [androidx.wear.compose.material3.ListHeader]. */
    public val ListHeader: ResponsiveVerticalPadding = Asymmetric
    /** Responsive vertical padding for [androidx.wear.compose.material3.Text]. */
    public val Text: ResponsiveVerticalPadding = Asymmetric

    /** Creates a [responsiveVerticalPadding] where top and bottom padding are the same. */
    private inline fun responsiveVerticalPadding(
        crossinline calculate: (containerHeight: Dp) -> Dp
    ): ResponsiveVerticalPadding = responsiveVerticalPadding(calculate, calculate)

    /** Creates a [ResponsiveVerticalPadding] with different top and bottom values. */
    private inline fun responsiveVerticalPadding(
        crossinline calculateTop: (containerHeight: Dp) -> Dp,
        crossinline calculateBottom: (containerHeight: Dp) -> Dp,
    ): ResponsiveVerticalPadding =
        object : ResponsiveVerticalPadding {
            override fun calculateTopPadding(containerHeight: Dp): Dp =
                calculateTop(containerHeight)

            override fun calculateBottomPadding(containerHeight: Dp): Dp =
                calculateBottom(containerHeight)
        }

    private const val LargePaddingFraction = 0.23f
    private const val SmallPaddingFraction = 0.13f
}
