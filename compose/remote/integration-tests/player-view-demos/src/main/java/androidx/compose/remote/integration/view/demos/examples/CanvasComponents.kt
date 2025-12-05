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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.computePosition
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@Suppress("RestrictedApiAndroidX")
fun RcCanvasComponents1(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        8,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            column(Modifier.fillMaxSize().background(Color.YELLOW).padding(16)) {
                canvas(Modifier.fillMaxSize().background(Color.BLUE)) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    painter.setColor(Color.RED).setStrokeWidth(4f).commit()
                    drawLine(0f, 0f, w, h)
                    drawLine(0f, h, w, 0f)
                    box(
                        Modifier.background(Color.CYAN).size(300, 200).computePosition {
                            x = w / 2f - RFloat(width.toFloat()) / 2f
                            y = h / 2f - RFloat(height.toFloat()) / 2f
                        }
                    ) {
                        text(
                            "Hello, World!",
                            autosize = true,
                            textAlign = CoreText.TEXT_ALIGN_CENTER,
                        )
                    }
                }
            }
        }
    }
}
