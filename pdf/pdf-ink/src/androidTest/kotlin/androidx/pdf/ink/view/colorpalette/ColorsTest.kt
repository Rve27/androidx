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

package androidx.pdf.ink.view.colorpalette

import android.content.Context
import androidx.pdf.ink.R
import androidx.pdf.ink.view.colorpalette.model.Color
import androidx.pdf.ink.view.colorpalette.model.getHighlightPaletteItems
import androidx.pdf.ink.view.colorpalette.model.getPenPaletteItems
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun penPalette_contentDescriptionsAreCorrect() {
        val penPalette = getPenPaletteItems(context)

        val expectedColors =
            mapOf(
                0xFF000000.toInt() to context.getString(R.string.pdf_pen_color_palette_item_black),
                0xFF202FB0.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_dark_blue),
                0xFFDD0000.toInt() to context.getString(R.string.pdf_pen_color_palette_item_red),
                0xFF00854C.toInt() to context.getString(R.string.pdf_pen_color_palette_item_green),
                0xFFD9C300.toInt() to context.getString(R.string.pdf_pen_color_palette_item_gold),
                0xFFC2C44D.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_olive_green),
                0xFF7B4B19.toInt() to context.getString(R.string.pdf_pen_color_palette_item_brown),
                0xFF757575.toInt() to context.getString(R.string.pdf_pen_color_palette_item_gray),
                0xFF3D5FEC.toInt() to context.getString(R.string.pdf_pen_color_palette_item_blue),
                0xFFFF4365.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_coral_red),
                0xFF35C369.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_lime_green),
                0xFFF9E100.toInt() to context.getString(R.string.pdf_pen_color_palette_item_yellow),
                0xFFD7E871.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_pale_lime),
                0xFFC48150.toInt() to context.getString(R.string.pdf_pen_color_palette_item_tan),
                0xFFC7C7C7.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_light_gray),
                0xFF9976FF.toInt() to context.getString(R.string.pdf_pen_color_palette_item_purple),
                0xFFFF8FEA.toInt() to context.getString(R.string.pdf_pen_color_palette_item_pink),
                0xFF9BEEC7.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_mint_green),
                0xFFFAB400.toInt() to context.getString(R.string.pdf_pen_color_palette_item_orange),
                0xFF5888B2.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_steel_blue),
                0xFFB8372F.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_deep_red),
                0xFFFFFFFF.toInt() to context.getString(R.string.pdf_pen_color_palette_item_white),
                0xFFC6B9FF.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_lavender),
                0xFFFFD4F8.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_pale_pink),
                0xFFDEFFC9.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_pale_green),
                0xFFFFC8A0.toInt() to context.getString(R.string.pdf_pen_color_palette_item_peach),
                0xFFD1E5EE.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_light_blue),
                0xFFED9D82.toInt() to context.getString(R.string.pdf_pen_color_palette_item_salmon),
            )

        val actualColors =
            penPalette.filterIsInstance<Color>().associate { it.color to it.contentDescription }
        assertEquals(expectedColors.size, actualColors.size)
        expectedColors.forEach { (color, contentDesc) ->
            assertEquals(contentDesc, actualColors[color], "Mismatch for color $color")
        }
    }

    @Test
    fun highlighterPalette_contentDescriptionsAreCorrect() {
        val highlighterPalette = getHighlightPaletteItems(context)

        val expectedColors =
            mapOf(
                0x66000000 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_dark_gray),
                0x66FFFFFF to context.getString(R.string.pdf_highlighter_color_palette_item_white),
                0x66FF0000 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_coral_red),
                0x6600FF00 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_lime_green),
                0x660000FF to
                    context.getString(R.string.pdf_highlighter_color_palette_item_periwinkle_blue),
                0x66FFA500 to context.getString(R.string.pdf_highlighter_color_palette_item_orange),
                0x66FFFF00 to context.getString(R.string.pdf_highlighter_color_palette_item_yellow),
                0x66FFC0CB to
                    context.getString(R.string.pdf_highlighter_color_palette_item_pale_pink),
                0x66ADD8E6 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_light_blue),
                0x6690EE90 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_pale_green),
                0x66FFED45 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_bright_yellow),
                0x66FF8279 to context.getString(R.string.pdf_highlighter_color_palette_item_peach),
                0x66A52A2A to
                    context.getString(R.string.pdf_highlighter_color_palette_item_dusty_rose),
                0x66808000 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_olive_green),
                0x66800080 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_medium_purple),
                0x66008000 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_medium_green),
                0x66DC143C to
                    context.getString(R.string.pdf_highlighter_color_palette_item_rose_pink),
                0x664682B4 to
                    context.getString(R.string.pdf_highlighter_color_palette_item_steel_blue),
                0x666A5ACD to context.getString(R.string.pdf_highlighter_color_palette_item_lilac),
                0x66556B2F to
                    context.getString(R.string.pdf_highlighter_color_palette_item_sage_green),
                0x66DEB887 to context.getString(R.string.pdf_highlighter_color_palette_item_beige),
            )

        val actualColors =
            highlighterPalette.filterIsInstance<Color>().associate {
                it.color to it.contentDescription
            }
        assertEquals(expectedColors.size, actualColors.size)
        expectedColors.forEach { (color, contentDesc) ->
            assertEquals(contentDesc, actualColors[color], "Mismatch for color $color")
        }
    }
}
