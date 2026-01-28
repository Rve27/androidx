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

package androidx.wear.compose.foundation.lazy

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Interface to define responsive vertical padding for items in [TransformingLazyColumn].
 *
 * This allows items to request specific padding for the top (if they are the first item) or the
 * bottom (if they are the last item) based on the container's height.
 *
 * The final padding applied to the container will be the maximum of the padding calculated by this
 * interface and the `contentPadding` parameter provided to the [TransformingLazyColumn].
 *
 * Implementations of this interface are expected to be provided by design systems, such as by
 * `ResponsiveVerticalPaddingDefaults` in Material3.
 *
 * @sample androidx.wear.compose.foundation.samples.TransformingLazyColumnResponsivePaddingSample
 */
public interface ResponsiveVerticalPadding {
    /** Calculates the top padding based on the container height. */
    public fun calculateTopPadding(containerHeight: Dp): Dp = 0.dp

    /** Calculates the bottom padding based on the container height. */
    public fun calculateBottomPadding(containerHeight: Dp): Dp = 0.dp
}
