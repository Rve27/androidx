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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.capture.shaders.solidColor
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
class RemoteCanvasTest {
    @TestParameter private lateinit var targetPlayer: TargetPlayer

    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = targetPlayer,
        )
    }

    private val tests =
        listOf<@Composable () -> Unit>(
            ::TestDrawAnchoredText_colorAndTextSize,
            ::TestDrawAnchoredText_brushAndTextSize,
        )

    @Test
    fun test() {
        val chunkedTests = tests.chunked(3)
        remoteComposeTestRule.runScreenshotTest {
            RemoteColumn {
                for (testRow in chunkedTests) {
                    RemoteRow {
                        for (testItem in testRow) {
                            Container { testItem() }
                            RemoteBox(modifier = RemoteModifier.width(Padding))
                        }
                    }
                    RemoteBox(modifier = RemoteModifier.height(Padding))
                }
            }
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_colorAndTextSize() {
        val text = RemoteString("Hello")
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = remote.component.width
            drawAnchoredText(
                text = text,
                color = Color.Red,
                anchor = RemoteOffset(w / 2f, 40f),
                textSize = SMALL_FONT_SIZE.rf,
            )
            drawAnchoredText(
                text = text,
                color = Color.Green,
                anchor = RemoteOffset(w / 2f, 80f),
                textSize = MEDIUM_FONT_SIZE.rf,
            )
            drawAnchoredText(
                text = text,
                color = Color.Blue,
                anchor = RemoteOffset(w / 2f, 120f),
                textSize = LARGE_FONT_SIZE.rf,
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_brushAndTextSize() {
        val text = RemoteString("Hello")
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = remote.component.width
            drawAnchoredText(
                text = text,
                brush = RemoteBrush.solidColor(Color.Red),
                anchor = RemoteOffset(w / 2f, 40f),
                textSize = SMALL_FONT_SIZE.rf,
            )
            drawAnchoredText(
                text = text,
                brush = RemoteBrush.solidColor(Color.Green),
                anchor = RemoteOffset(w / 2f, 80f),
                textSize = MEDIUM_FONT_SIZE.rf,
            )
            drawAnchoredText(
                text = text,
                brush = RemoteBrush.solidColor(Color.Blue),
                anchor = RemoteOffset(w / 2f, 120f),
                textSize = LARGE_FONT_SIZE.rf,
            )
        }
    }

    @Composable
    @RemoteComposable
    private fun Container(
        modifier: RemoteModifier = RemoteModifier,
        content: @Composable () -> Unit,
    ) {
        RemoteBox(
            modifier = modifier.size(ContainerSize).background(ContainerColor),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center,
            content = content,
        )
    }

    private companion object {
        val Padding = 24.rdp
        val ContainerSize = 100.rdp
        val ContainerColor = Color(0xFFCFD8DC)
        const val SMALL_FONT_SIZE = 16f
        const val MEDIUM_FONT_SIZE = 32f
        const val LARGE_FONT_SIZE = 48f
    }
}
