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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.text.AnnotatedString

/**
 * A [TextStyleBuffer] implemented as an interval tree. It is also order-aware; styles are returned
 * in the order they were added.
 */
internal class TextStyleBuffer<T>(source: TextStyleBuffer<T>? = null) {
    private val intervalTree: IntIntervalTree<T> =
        source?.let { IntIntervalTree(it.intervalTree) } ?: IntIntervalTree()

    /**
     * Adds the style defined between an interval defined by [start] and [end].
     *
     * @param style The style to be added.
     * @param start The start index of the interval, inclusive.
     * @param end The end index of the interval, must be > [start], exclusive.
     * @return true if the style is added, false otherwise.
     */
    fun addStyle(style: T, start: Int, end: Int): Boolean {
        return intervalTree.addInterval(style, start, end)
    }

    /**
     * Returns the styles that overlap with the interval defined by [start] and [end]. The overlap
     * is inclusive on [start] but exclusive at the [end].
     *
     * @return a list of [AnnotatedString.Range]s representing the styles in the buffer in the order
     *   they are added.
     */
    fun getStyles(start: Int, end: Int): List<AnnotatedString.Range<T>> {
        val result = mutableListOf<AnnotatedString.Range<T>>()
        intervalTree.forEachIntervalInRange(start, end) { item, intervalStart, intervalEnd ->
            result.add(AnnotatedString.Range(item, intervalStart, intervalEnd))
        }
        return result
    }

    /**
     * Returns all styles in the buffer.
     *
     * This method is equivalent to call [getStyles] with full range but is optimized to be faster,
     * especially when a large number of styles are stored.
     *
     * @return A list of [AnnotatedString.Range]s representing the styles in the buffer in the order
     *   they were added.
     */
    fun getAllStyles(): List<AnnotatedString.Range<T>> {
        val result = mutableListOf<AnnotatedString.Range<T>>()
        intervalTree.forAllIntervals { item, start, end ->
            result.add(AnnotatedString.Range(item, start, end))
        }
        return result
    }

    /**
     * Removes the style defined between a [start] and an [end] coordinate.
     *
     * @param style The style to be removed.
     * @param start The start index of the interval
     * @param end The end index of the interval, must be > [start]
     * @return true if the style is removed, false otherwise.
     */
    fun removeStyle(style: T, start: Int, end: Int): Boolean {
        return intervalTree.removeInterval(style, start, end)
    }

    /** Clears all styles from this buffer and prepares it for reuse. */
    fun clear() {
        intervalTree.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextStyleBuffer<T>) return false

        return intervalTree == other.intervalTree
    }

    override fun hashCode(): Int {
        return intervalTree.hashCode()
    }
}
