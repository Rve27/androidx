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
import android.graphics.Paint
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.plus
import androidx.compose.remote.creation.times
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A demo that draws a Pie Chart using procedural creation APIs. */
@Suppress("RestrictedApiAndroidX")
fun demoPieChart(): RemoteComposeContextAndroid {
    val data = floatArrayOf(30f, 20f, 15f, 25f, 10f)
    val names = arrayOf("Android", "iOS", "Web", "Desktop", "Other")
    val colors =
        intArrayOf(
            0xFF4CAF50.toInt(), // Green
            0xFF2196F3.toInt(), // Blue
            0xFFFFC107.toInt(), // Amber
            0xFFE91E63.toInt(), // Pink
            0xFF9C27B0.toInt(), // Purple
        )

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Pie Chart Demo"),
        ) {
            root {
                box(
                    RecordingModifier().fillMaxSize().background(0xFFF0F0F0.toInt()),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val radius = min(w, h) * 0.4f

                        val total = data.sum()
                        var currentAngle = -90f // Start from top

                        for (i in data.indices) {
                            val sweepAngle = (data[i] / total) * 360f

                            // Draw slice
                            painter
                                .setColor(colors[i % colors.size])
                                .setStyle(Paint.Style.FILL)
                                .commit()

                            drawSector(
                                cx - radius,
                                cy - radius,
                                cx + radius,
                                cy + radius,
                                currentAngle,
                                sweepAngle,
                            )

                            // Draw border
                            painter
                                .setColor(Color.WHITE)
                                .setStyle(Paint.Style.STROKE)
                                .setStrokeWidth(2f)
                                .commit()

                            drawSector(
                                cx - radius,
                                cy - radius,
                                cx + radius,
                                cy + radius,
                                currentAngle,
                                sweepAngle,
                            )

                            // Draw Label
                            val labelAngle = (currentAngle + sweepAngle / 2f) * PI.toFloat() / 180f
                            val labelRadius = radius * 0.7f
                            val lx = cx + labelRadius * cos(labelAngle)
                            val ly = cy + labelRadius * sin(labelAngle)

                            painter
                                .setColor(Color.WHITE)
                                .setTextSize(24f)
                                .setStyle(Paint.Style.FILL)
                                .setTypeface(0, 700, false) // Bold
                                .commit()

                            val textId = addText(names[i])
                            drawTextAnchored(textId, lx, ly, 0.5f, 0.5f, 0)

                            currentAngle += sweepAngle
                        }

                        // Draw Legend
                        val legendX = 20f
                        var legendY = 20f
                        for (i in data.indices) {
                            painter
                                .setColor(colors[i % colors.size])
                                .setStyle(Paint.Style.FILL)
                                .commit()
                            drawRect(legendX, legendY, 20f, 20f)

                            painter.setColor(Color.BLACK).setTextSize(20f).commit()
                            val textId = addText(names[i] + " (" + data[i].toInt() + "%)")
                            drawTextAnchored(textId, legendX + 30f, legendY + 15f, 0f, 0.5f, 0)

                            legendY += 30f
                        }
                    }
                }
            }
        }
    return rc
}
