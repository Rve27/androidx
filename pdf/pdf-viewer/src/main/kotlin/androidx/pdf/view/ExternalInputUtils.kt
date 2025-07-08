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

package androidx.pdf.view

import kotlin.math.roundToInt

internal object ExternalInputUtils {
    fun calculateScroll(viewportDimension: Int, scrollFactor: Int): Int {
        return (viewportDimension.toFloat() / scrollFactor).roundToInt()
    }

    fun calculateGreaterZoom(
        currentZoom: Float,
        baselineZoom: Float,
        zoomLevels: List<Float>,
        maxAbsoluteZoom: Float,
    ): Float {
        val currentZoomLevel = (currentZoom / baselineZoom)
        // Add a small tolerance to ensure a distinct zoom step overcome float inaccuracies.
        // If no higher level is found, return the absolute max
        val zoomFactor =
            zoomLevels.firstOrNull { it > currentZoomLevel + 0.01f } ?: return maxAbsoluteZoom
        return zoomFactor * baselineZoom
    }

    fun calculateSmallerZoom(
        currentZoom: Float,
        baselineZoom: Float,
        zoomLevels: List<Float>,
        minAbsoluteZoom: Float,
    ): Float {
        val currentZoomLevel = (currentZoom / baselineZoom)
        // Add a small tolerance to ensure a distinct zoom step overcome float inaccuracies.
        // If no smaller level is found, return the absolute min
        val zoomFactor =
            zoomLevels.reversed().firstOrNull { it < currentZoomLevel - 0.01f }
                ?: return minAbsoluteZoom
        return zoomFactor * baselineZoom
    }
}
