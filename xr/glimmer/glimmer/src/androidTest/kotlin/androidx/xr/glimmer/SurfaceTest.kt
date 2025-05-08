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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun equality() {
        lateinit var surface: Modifier
        lateinit var surfaceWithSameParameters: Modifier
        lateinit var surfaceWithDifferentParameters: Modifier
        rule.setGlimmerThemeContent {
            surface = Modifier.surface(RectangleShape, Color.Blue, BorderStroke(1.dp, Color.Red))
            surfaceWithSameParameters =
                Modifier.surface(RectangleShape, Color.Blue, BorderStroke(1.dp, Color.Red))
            surfaceWithDifferentParameters =
                Modifier.surface(CircleShape, Color.Blue, BorderStroke(1.dp, Color.Red))
        }

        rule.runOnIdle {
            assertThat(surface).isEqualTo(surfaceWithSameParameters)
            assertThat(surface).isNotEqualTo(surfaceWithDifferentParameters)
        }
    }

    @Test
    fun semantics() {
        rule.setGlimmerThemeContent { Box(Modifier.size(100.dp).surface().testTag("surface")) }
        rule
            .onNodeWithTag("surface")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
    }

    @Test
    fun clipsContent() {
        rule.setGlimmerThemeContent {
            with(LocalDensity.current) {
                val outerSize = 100.toDp()
                val innerSize = 50.toDp()
                Box(Modifier.size(outerSize).testTag("outerBox").background(Color.Red)) {
                    Box(
                        Modifier.size(innerSize)
                            .surface(shape = RectangleShape, color = Color.Blue, border = null)
                            .drawWithContent {
                                // Try and draw a rect that would fill the outerSize, if there was
                                // no clipping
                                drawRect(color = Color.Green, size = Size(100f, 100f))
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag("outerBox").captureToImage().assertPixels(
            expectedSize = IntSize(100, 100)
        ) {
            if (it.x < 50 && it.y < 50) {
                // The inner surface should all be green
                Color.Green
            } else {
                // The outer box should be red, as the inner surface should clip the green
                Color.Red
            }
        }
    }

    @Test
    fun cachesBorder() {
        lateinit var defaultBorder: BorderStroke
        lateinit var anotherDefaultBorder: BorderStroke
        lateinit var customBorder: BorderStroke
        rule.setGlimmerThemeContent {
            defaultBorder = SurfaceDefaults.border()
            anotherDefaultBorder = SurfaceDefaults.border()
            customBorder = SurfaceDefaults.border(color = Color.Red)
        }

        rule.runOnIdle {
            assertThat(defaultBorder).isSameInstanceAs(anotherDefaultBorder)
            assertThat(defaultBorder).isNotEqualTo(customBorder)
        }
    }

    @Test
    fun borderValues() {
        lateinit var defaultBorder: BorderStroke
        lateinit var customBorder: BorderStroke
        var outline: Color = Color.Unspecified
        rule.setGlimmerThemeContent {
            outline = GlimmerTheme.colors.outline
            defaultBorder = SurfaceDefaults.border()
            customBorder = SurfaceDefaults.border(color = Color.Red)
        }

        rule.runOnIdle {
            assertThat((defaultBorder.brush as SolidColor).value).isEqualTo(outline)
            assertThat(defaultBorder.width).isEqualTo(3.dp)
            assertThat((customBorder.brush as SolidColor).value).isEqualTo(Color.Red)
            assertThat(customBorder.width).isEqualTo(3.dp)
        }
    }
}
