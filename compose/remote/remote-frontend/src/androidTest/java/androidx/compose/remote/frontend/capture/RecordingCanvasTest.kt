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

package androidx.compose.remote.frontend.capture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.state.RemoteBoolean
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WIDTH = 400
private const val HEIGHT = 400

/** Emulator-based screenshot test of [RecordingCanvas]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RecordingCanvasTest {
    private val SCREENSHOT_GOLDEN_DIRECTORY = "compose/remote/remote-frontend"

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private val creationState =
        RemoteComposeCreationState(
            AndroidxPlatformServices(),
            1f,
            Size(WIDTH.toFloat(), HEIGHT.toFloat()),
            CoreDocument.DOCUMENT_API_LEVEL,
            Operations.PROFILE_ANDROIDX,
        )

    private val recordingCanvas =
        RecordingCanvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    private val remoteContext = AndroidRemoteContext()

    @Before
    fun setUp() {
        recordingCanvas.setRemoteComposeCreationState(creationState)
    }

    @Test
    fun drawConditionally_true() {
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)
        val document = constructConditionalDocument(flag)
        assertScreenshot(document, "drawConditonally_true")
    }

    @Test
    fun drawConditionally_false() {
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", false)
        val document = constructConditionalDocument(flag)
        assertScreenshot(document, "drawConditonally_false")
    }

    private fun constructConditionalDocument(flag: RemoteBoolean): CoreDocument {
        recordingCanvas.drawConditionally(flag) {
            recordingCanvas.drawText(
                "True",
                10,
                80,
                Paint().apply {
                    color = Color.GREEN
                    textSize = 80f
                },
            )
        }
        recordingCanvas.drawConditionally(!flag) {
            recordingCanvas.drawText(
                "False",
                10,
                80,
                Paint().apply {
                    color = Color.RED
                    textSize = 80f
                },
            )
        }

        return constructDocument()
    }

    private fun constructDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
        }

    private fun assertScreenshot(document: CoreDocument, filename: String) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        remoteContext.useCanvas(canvas)
        document.paint(remoteContext, 0)
        bitmap.assertAgainstGolden(screenshotRule, "${this::class.simpleName}_$filename")
    }
}
