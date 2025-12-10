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
package androidx.compose.foundation.layout.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridScope
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GridDemo() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        MixedSizingDemo()
        Spacer(Modifier.height(32.dp))
        FlexibleSizingDemo()
        Spacer(Modifier.height(32.dp))
        NegativeIndicesDemo()
        Spacer(Modifier.height(32.dp))
        GapsDemo()
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MixedSizingDemo() {
    DemoHeader("Mixed Sizing: Fixed vs Fraction vs Flex")
    Grid(
        config = {
            columns(
                GridTrackSize.Fixed(100.dp),
                GridTrackSize.Percentage(0.3f),
                GridTrackSize.Flex(1.fr),
            )
            row(100.dp)
        },
        modifier = Modifier.demoContainer(),
    ) {
        GridDemoItem(text = "Fixed\n100dp", color = Color.Red, row = 1, column = 1)
        GridDemoItem(text = "Fraction\n30% of Total", color = Color.Blue, row = 1, column = 2)
        GridDemoItem(text = "Flex\nRest of Space", color = Color.Green, row = 1, column = 3)
    }
}

@Composable
private fun FlexibleSizingDemo() {
    DemoHeader("Flexible (Fr) Sizing")
    Grid(
        config = {
            column(80.dp)
            column(1.fr)
            column(2.fr)
            row(1.fr)
            row(60.dp)
        },
        modifier = Modifier.height(200.dp).demoContainer(borderColor = Color.Magenta),
    ) {
        val rowLabels = listOf("Flex 1.fr", "60dp")
        val colLabels = listOf("Fixed 80dp", "Flex 1.fr", "Flex 2.fr")

        rowLabels.forEachIndexed { rowIndex, rowLabel ->
            colLabels.forEachIndexed { colIndex, colLabel ->
                GridDemoItem(
                    text = "H: $rowLabel\nW: $colLabel",
                    row = rowIndex + 1,
                    column = colIndex + 1,
                )
            }
        }
    }
}

@Composable
fun NegativeIndicesDemo() {
    DemoHeader("Negative Indices")
    Grid(
        config = {
            repeat(3) { column(60.dp) }
            repeat(3) { row(60.dp) }
            gap(4.dp)
        },
        modifier = Modifier.border(1.dp, Color.Gray),
    ) {
        GridDemoItem(text = "TL", row = 1, column = 1, color = Color.Red)
        GridDemoItem("TR", row = 1, column = -1, color = Color.Blue)
        GridDemoItem("BL", row = -1, column = 1, color = Color.Green)
        GridDemoItem("BR", row = -1, column = -1, color = Color.Yellow)
        GridDemoItem("Center", row = 2, column = 2, color = Color.Gray)
    }
}

@Composable
private fun GapsDemo() {
    DemoHeader("Gaps Demo")
    Grid(
        config = {
            column(GridTrackSize.Fixed(100.dp))
            column(GridTrackSize.Flex(1.fr))
            column(GridTrackSize.Fixed(200.dp))
            row(GridTrackSize.Fixed(60.dp))
            row(GridTrackSize.Fixed(100.dp))
            gap(row = 12.dp, column = 6.dp)
        },
        modifier = Modifier.demoContainer(borderColor = Color.Cyan),
    ) {
        repeat(6) {
            val row = (it / 3) + 1
            val col = (it % 3) + 1
            GridDemoItem(text = "Item ${it + 1}", measureSize = false, row = row, column = col)
        }
    }
}

@Composable
private fun DemoHeader(text: String) =
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp),
    )

@Composable
private fun GridScope.GridDemoItem(
    text: String,
    modifier: Modifier = Modifier,
    row: Int? = null,
    column: Int? = null,
    rowSpan: Int = 1,
    columnSpan: Int = 1,
    color: Color = Color.Green,
    measureSize: Boolean = true,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    var finalModifier = modifier.fillMaxSize()

    if (row != null && column != null) {
        finalModifier = finalModifier.gridItem(row, column, rowSpan, columnSpan)
    } else if (rowSpan > 1 || columnSpan > 1) {
        finalModifier = finalModifier.gridItem(rowSpan = rowSpan, columnSpan = columnSpan)
    }

    Box(
        finalModifier
            .onSizeChanged { size = it }
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.5f))
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, fontSize = 10.sp, textAlign = TextAlign.Center)
            if (measureSize) {
                val w = with(density) { size.width.toDp().value.toInt() }
                val h = with(density) { size.height.toDp().value.toInt() }
                Text(text = "$w x $h", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun Modifier.demoContainer(borderColor: Color = Color.Black) =
    this.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.5f)).padding(8.dp)
