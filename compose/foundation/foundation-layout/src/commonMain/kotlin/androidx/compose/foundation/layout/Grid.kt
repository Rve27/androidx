/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.layout

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.GridScope.Companion.GridIndexUnspecified
import androidx.compose.foundation.layout.GridScope.Companion.MaxGridIndex
import androidx.compose.foundation.layout.internal.JvmDefaultWithCompatibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.jvm.JvmInline

/**
 * A 2D layout composable that arranges children into a grid of rows and columns.
 *
 * The [Grid] allows defining explicit tracks (columns and rows) with various sizing capabilities,
 * including fixed sizes (`dp`), flexible fractions (`fr`), percentages, and content-based sizing
 * (`Auto`).
 *
 * **Key Features:**
 * * **Explicit vs. Implicit:** You define the main structure via [config] (explicit tracks). If
 *   items are placed outside these defined bounds, or if auto-placement creates new rows/columns,
 *   the grid automatically extends using implicit sizing (defaults to `Auto`).
 * * **Flexible Sizing:** Use [Fr] units (e.g., `1.fr`, `2.fr`) to distribute available space
 *   proportionally among tracks.
 * * **Auto-placement:** Items without a specific [GridScope.gridItem] modifier flow automatically
 *   into the next available cell based on the configured [GridFlow]. .
 *
 * @param config A block that defines the columns, rows, and gaps of the grid. This block runs
 *   during the measure pass, enabling efficient updates based on state.
 * @param modifier The modifier to be applied to the layout.
 * @param content The content of the grid. Direct children can use [GridScope.gridItem] to configure
 *   their position and span.
 * @see GridScope.gridItem
 * @see GridConfigurationScope
 */
@Composable
inline fun Grid(
    noinline config: GridConfigurationScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable GridScope.() -> Unit,
) {
    Layout(
        content = { GridScopeInstance.content() },
        modifier = modifier,
        measurePolicy =
            MeasurePolicy { _, constraints ->
                // Implementation to be added in follow-up CL
                layout(constraints.minWidth, constraints.minHeight) {}
            },
    )
}

/** Scope for the children of [Grid]. */
@LayoutScopeMarker
@Immutable
@JvmDefaultWithCompatibility
interface GridScope {
    /**
     * Configures the position, span, and alignment of an element within a [Grid] layout.
     *
     * Apply this modifier to direct children of a [Grid] composable.
     *
     * **Default Behavior:** If this modifier is not applied to a child, the child will be
     * automatically placed in the next available cell (spanning 1 row and 1 column) according to
     * the configured [GridFlow].
     *
     * **Indexing:** Grid row and column indices are **1-based**.
     * * **Positive** values count from the start (1 is the first row/column).
     * * **Negative** values count from the end (-1 is the last explicitly defined row/column).
     *
     * **Auto-placement:** If [row] or [column] are left to their default value
     * ([GridIndexUnspecified]), the [Grid] layout will automatically place the item based on the
     * configured [GridFlow].
     *
     * @param row The specific 1-based row index to place the item in. Positive values count from
     *   the start (1 is the first row). Negative values count from the end (-1 is the last row).
     *   Must be within the range [-[MaxGridIndex], [MaxGridIndex]]. Defaults to
     *   [GridIndexUnspecified] for auto-placement.
     * @param column The specific 1-based column index to place the item in. Positive values count
     *   from the start (1 is the first column). Negative values count from the end (-1 is the last
     *   column). Must be within the range [-[MaxGridIndex], [MaxGridIndex]]. Defaults to
     *   [GridIndexUnspecified] for auto-placement.
     * @param rowSpan The number of rows this item should occupy. Must be greater than 0. Defaults
     *   to 1.
     * @param columnSpan The number of columns this item should occupy. Must be greater than 0.
     *   Defaults to 1.
     * @param alignment Specifies how the content should be aligned within the grid cell(s) it
     *   occupies. Defaults to [Alignment.TopStart].
     * @throws IllegalArgumentException if [row] or [column] (when specified) are outside the valid
     *   range, or if [rowSpan] or [columnSpan] are less than 1.
     * @see GridIndexUnspecified
     * @see MaxGridIndex
     */
    @Stable
    fun Modifier.gridItem(
        row: Int = GridIndexUnspecified,
        column: Int = GridIndexUnspecified,
        rowSpan: Int = 1,
        columnSpan: Int = 1,
        alignment: Alignment = Alignment.TopStart,
    ): Modifier

    /**
     * Configures the position, span, and alignment of an element within a [Grid] layout using
     * ranges.
     *
     * This convenience overload converts [IntRange] inputs into row/column indices and spans.
     *
     * **Equivalence:**
     * - `rows = 4..5` maps to `row = 4`, `rowSpan = 2`.
     * - `columns = 1..1` maps to `column = 1`, `columnSpan = 1`.
     *
     * Example: `Modifier.gridItem(rows = 2..3, columns = 1..2)` is functionally equivalent to
     * `Modifier.gridItem(row = 2, rowSpan = 2, column = 1, columnSpan = 2)`.
     *
     * @param rows The range of rows to occupy (e.g., `1..2`). The start determines the row index,
     *   and the size of the range determines the span.
     * @param columns The range of columns to occupy (e.g., `1..3`). The start determines the column
     *   index, and the size of the range determines the span.
     * @param alignment Specifies how the content should be aligned within the grid cell(s).
     *   Defaults to [Alignment.TopStart].
     * @see Modifier.gridItem
     */
    @Stable
    fun Modifier.gridItem(
        rows: IntRange,
        columns: IntRange,
        alignment: Alignment = Alignment.TopStart,
    ): Modifier

    companion object {
        /**
         * The maximum allowed index for a row or column (inclusive).
         *
         * This hard limit prevents performance degradation, layout timeouts, or memory issues
         * potentially caused by accidental loop overflows or unreasonably large sparse grid
         * definitions.
         */
        const val MaxGridIndex: Int = 1000
        /**
         * Sentinel value indicating that a grid position (row or column) is not manually specified
         * and should be determined automatically by the layout flow.
         */
        const val GridIndexUnspecified: Int = 0
    }
}

/** Internal implementation of [GridScope]. Stateless object to avoid allocations. */
@PublishedApi
internal object GridScopeInstance : GridScope {

    override fun Modifier.gridItem(
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int,
        alignment: Alignment,
    ): Modifier {
        if (row != GridIndexUnspecified) {
            require(row in -MaxGridIndex..MaxGridIndex) {
                "row must be between -$MaxGridIndex and $MaxGridIndex"
            }
        }
        if (column != GridIndexUnspecified) {
            require(column in -MaxGridIndex..MaxGridIndex) {
                "column must be between -$MaxGridIndex and $MaxGridIndex"
            }
        }
        require(rowSpan > 0) { "rowSpan must be > 0" }
        require(columnSpan > 0) { "columnSpan must be > 0" }
        return this.then(GridItemElement(row, column, rowSpan, columnSpan, alignment))
    }

    override fun Modifier.gridItem(
        rows: IntRange,
        columns: IntRange,
        alignment: Alignment,
    ): Modifier {
        require(!rows.isEmpty()) { "Row range ($rows) cannot be empty" }
        require(!columns.isEmpty()) { "Column range ($columns) cannot be empty" }

        val row = rows.first
        val rowSpan = rows.last - rows.first + 1
        val column = columns.first
        val columnSpan = columns.last - columns.first + 1
        return this.gridItem(row, column, rowSpan, columnSpan, alignment)
    }
}

/**
 * Scope for configuring the structure of a [Grid].
 *
 * This interface is implemented by the configuration block in [Grid]. It allows defining columns,
 * rows, and gaps.
 */
@LayoutScopeMarker
interface GridConfigurationScope : Density {

    /**
     * The direction in which items that do not specify a position are placed. Defaults to
     * [GridFlow.Row].
     */
    var flow: GridFlow

    /** Defines a fixed-width column. Maps to [GridTrackSize.Fixed]. */
    fun column(size: Dp)

    /** Defines a flexible column. Maps to [GridTrackSize.Flex]. */
    fun column(weight: Fr)

    /** Defines a percentage-based column. Maps to [GridTrackSize.Percentage]. */
    fun column(percentage: Float)

    /** Defines a new column track with the specified [size]. */
    fun column(size: GridTrackSize)

    /** Defines a fixed-width row. Maps to [GridTrackSize.Fixed]. */
    fun row(size: Dp)

    /** Defines a flexible row. Maps to [GridTrackSize.Flex]. */
    fun row(weight: Fr)

    /** Defines a percentage-based row. Maps to [GridTrackSize.Percentage]. */
    fun row(percentage: Float)

    /** Defines a new row track with the specified [size]. */
    fun row(size: GridTrackSize)

    /**
     * Sets both the row and column gaps (gutters) to [all].
     *
     * **Precedence:** If this is called multiple times, or mixed with [columnGap] or [rowGap], the
     * **last call** takes precedence.
     *
     * @throws IllegalArgumentException if [all] is negative.
     */
    fun gap(all: Dp)

    /**
     * Sets independent gaps for rows and columns.
     *
     * **Precedence:** If this is called multiple times, or mixed with [columnGap] or [rowGap], the
     * **last call** takes precedence.
     *
     * @throws IllegalArgumentException if [row] or [column] is negative.
     */
    fun gap(row: Dp, column: Dp)

    /**
     * Sets the gap (gutter) size between columns.
     *
     * **Precedence:** If this is called multiple times, the **last call** takes precedence. This
     * call will overwrite the column component of any previous [gap] call.
     *
     * @throws IllegalArgumentException if [gap] is negative.
     */
    fun columnGap(gap: Dp)

    /**
     * Sets the gap (gutter) size between rows.
     *
     * **Precedence:** If this is called multiple times, the **last call** takes precedence. This
     * call will overwrite the row component of any previous [gap] call.
     *
     * @throws IllegalArgumentException if [gap] is negative.
     */
    fun rowGap(gap: Dp)

    /** Creates an [Fr] unit from an [Int]. */
    @Stable
    val Int.fr: Fr
        get() = Fr(this.toFloat())

    /** Creates an [Fr] unit from a [Float]. */
    @Stable
    val Float.fr: Fr
        get() = Fr(this)

    /** Creates an [Fr] unit from a [Double]. */
    @Stable
    val Double.fr: Fr
        get() = Fr(this.toFloat())
}

/** Adds multiple columns with the specified [specs]. */
fun GridConfigurationScope.columns(vararg specs: GridTrackSpec) {
    for (spec in specs) {
        if (spec is GridTrackSize) {
            column(spec)
        }
    }
}

/** Adds multiple rows with the specified [specs]. */
fun GridConfigurationScope.rows(vararg specs: GridTrackSpec) {
    for (spec in specs) {
        if (spec is GridTrackSize) {
            row(spec)
        }
    }
}

/** Defines the direction in which auto-placed items flow within the grid. */
@JvmInline
value class GridFlow @PublishedApi internal constructor(private val bits: Int) {

    companion object {
        /** Items are placed filling the first row, then moving to the next row. */
        inline val Row
            get() = GridFlow(0)

        /** Items are placed filling the first column, then moving to the next column. */
        inline val Column
            get() = GridFlow(1)
    }

    override fun toString(): String =
        when (this) {
            Row -> "Row"
            Column -> "Column"
            else -> "GridFlow($bits)"
        }
}

/**
 * Represents a flexible unit used for sizing [Grid] tracks.
 *
 * One [Fr] unit represents a fraction of the *remaining* space in the grid container after
 * [GridTrackSize.Fixed] and [GridTrackSize.Percentage] tracks have been allocated.
 */
@JvmInline
value class Fr(val value: Float) {
    override fun toString(): String = "$value.fr"
}

/**
 * Marker interface to enable vararg usage with [GridTrackSize].
 *
 * This allows the configuration DSL to accept [GridTrackSize] items in a vararg (e.g.,
 * `columns(Fixed(10.dp), Flex(1.fr))`), bypassing the Kotlin limitation on value class varargs.
 */
sealed interface GridTrackSpec

/**
 * Defines the size of a track (a row or a column) in a [Grid].
 *
 * Use the companion functions (e.g., [Fixed], [Flex]) to create instances.
 */
@Immutable
@JvmInline
value class GridTrackSize internal constructor(internal val encodedValue: Long) : GridTrackSpec {

    internal val type: Int
        get() = (encodedValue ushr 32).toInt()

    internal val value: Float
        get() = Float.fromBits(encodedValue.toInt())

    override fun toString(): String =
        when (type) {
            TypeFixed -> "Fixed(${value}dp)"
            TypePercentage -> "Percentage($value)"
            TypeFlex -> "Flex(${value}fr)"
            TypeMinContent -> "MinContent"
            TypeMaxContent -> "MaxContent"
            TypeAuto -> "Auto"
            else -> "Unknown"
        }

    companion object {
        internal const val TypeFixed = 1
        internal const val TypePercentage = 2
        internal const val TypeFlex = 3
        internal const val TypeMinContent = 4
        internal const val TypeMaxContent = 5
        internal const val TypeAuto = 6

        /**
         * A track with a fixed [Dp] size.
         *
         * @param size The size of the track.
         * @throws IllegalArgumentException if [size] is negative or [Dp.Unspecified].
         */
        @Stable
        fun Fixed(size: Dp): GridTrackSize {
            require(size != Dp.Unspecified && size.value >= 0f) {
                "Fixed size must be non-negative and specified (was $size)"
            }
            return pack(TypeFixed, size.value)
        }

        /**
         * A track sized as a percentage of the **total** available size of the grid container.
         * **Note:** In this implementation, percentages are calculated based on the **remaining
         * available space after gaps**. This differs from the W3C CSS Grid spec, where percentages
         * are based on the container size regardless of gaps. This behavior prevents unexpected
         * overflows when mixing gaps and percentages (e.g., `50%` + `50%` + `gap` will fit
         * perfectly here, but would overflow in CSS).
         *
         * @param value The percentage of the container size.
         * @throws IllegalArgumentException if [value] is negative.
         */
        @Stable
        fun Percentage(@FloatRange(from = 0.0) value: Float): GridTrackSize {
            require(value >= 0f) { "Percentage cannot be negative" }
            return pack(TypePercentage, value)
        }

        /**
         * A flexible track that takes a share of the **remaining** space after Fixed and Percentage
         * tracks are allocated.
         *
         * @param weight The flexible weight. Space is distributed proportional to this weight
         *   divided by the total flex weight. Must be non-negative.
         * @throws IllegalArgumentException if [weight] is negative.
         */
        @Stable
        fun Flex(@FloatRange(from = 0.0) weight: Fr): GridTrackSize {
            require(weight.value >= 0f) { "Flex weight must be positive" }
            return pack(TypeFlex, weight.value)
        }

        /** A track that sizes itself to fit the minimum intrinsic size of its contents. */
        @Stable val MinContent = pack(TypeMinContent, 0f)

        /** A track that sizes itself to fit the maximum intrinsic size of its contents. */
        @Stable val MaxContent = pack(TypeMaxContent, 0f)

        /**
         * A track that behaves automatically, typically similar to [MinContent] or [Flex] depending
         * on context.
         */
        @Stable val Auto = pack(TypeAuto, 0f)

        private fun pack(type: Int, value: Float): GridTrackSize {
            // Pack Type (High 32) and Float bits (Low 32) into one Long.
            // Mask 0xFFFFFFFFL prevents sign extension when casting int to long.
            val raw = (type.toLong() shl 32) or (value.toRawBits().toLong() and 0xFFFFFFFFL)
            return GridTrackSize(raw)
        }
    }
}

/**
 * The modifier element that creates and updates [GridItemNode].
 *
 * @property row The 1-based row index, or [GridScope.GridIndexUnspecified] for auto-placement.
 * @property column The 1-based column index, or [GridScope.GridIndexUnspecified] for
 *   auto-placement.
 * @property rowSpan The number of rows the item should occupy.
 * @property columnSpan The number of columns the item should occupy.
 * @property alignment The alignment of the content within the grid cell.
 * @see GridItemNode
 */
private class GridItemElement(
    val row: Int,
    val column: Int,
    val rowSpan: Int,
    val columnSpan: Int,
    val alignment: Alignment,
) : ModifierNodeElement<GridItemNode>() {
    override fun create(): GridItemNode = GridItemNode(row, column, rowSpan, columnSpan, alignment)

    override fun update(node: GridItemNode) {
        node.row = row
        node.column = column
        node.rowSpan = rowSpan
        node.columnSpan = columnSpan
        node.alignment = alignment
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "gridItem"
        properties["row"] = row
        properties["column"] = column
        properties["rowSpan"] = rowSpan
        properties["columnSpan"] = columnSpan
        properties["alignment"] = alignment
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GridItemElement) return false

        if (row != other.row) return false
        if (column != other.column) return false
        if (rowSpan != other.rowSpan) return false
        if (columnSpan != other.columnSpan) return false
        if (alignment != other.alignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + column
        result = 31 * result + rowSpan
        result = 31 * result + columnSpan
        result = 31 * result + alignment.hashCode()
        return result
    }
}

/**
 * The modifier node that provides parent data to the [Grid] layout.
 *
 * This class implements [ParentDataModifierNode], allowing the parent [Grid] layout to inspect the
 * configuration (row, column, spans) of this specific child during the measurement phase via the
 * [modifyParentData] method.
 *
 * @property row The 1-based row index, or [GridScope.GridIndexUnspecified] for auto-placement.
 * @property column The 1-based column index, or [GridScope.GridIndexUnspecified] for
 *   auto-placement.
 * @property rowSpan The number of rows the item should occupy.
 * @property columnSpan The number of columns the item should occupy.
 * @property alignment The alignment of the content within the grid cell.
 * @throws IllegalArgumentException if [rows] or [columns] ranges are empty, or if the derived
 *   row/column indices or spans do not meet the requirements of the primary [gridItem] function.
 * @see GridScope.gridItem for the public API and input validation.
 */
private class GridItemNode(
    var row: Int,
    var column: Int,
    var rowSpan: Int,
    var columnSpan: Int,
    var alignment: Alignment,
) : Modifier.Node(), ParentDataModifierNode {
    override fun Density.modifyParentData(parentData: Any?) = this@GridItemNode
}
