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

package androidx.compose.remote.player.compose.creation

import android.graphics.Path
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.rule.RemoteComposeScreenshotTestRule
import androidx.compose.remote.player.compose.test.util.getCoreDocument
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class DrawOperationsTest {
    @get:Rule
    val remoteComposeTestRule = RemoteComposeScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun drawArc() {
        val document = getCoreDocument { drawArc(0f, 0f, 100f, 100f, 45f, 135f) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawSector() {
        val document = getCoreDocument { drawSector(0f, 0f, 100f, 100f, 45f, 135f) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawCircle() {
        val document = getCoreDocument { drawCircle(50f, 50f, 40f) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawLine() {
        val document = getCoreDocument { drawLine(10f, 10f, 90f, 90f) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawOval() {
        val document = getCoreDocument { drawOval(10f, 20f, 90f, 80f) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawRect() {
        val document = getCoreDocument { drawRect(10f, 20f, 90f, 80f) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawRoundRect() {
        val document = getCoreDocument { drawRoundRect(10f, 20f, 90f, 80f, 15f, 15f) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawPath() {
        val document = getCoreDocument {
            val pathId = pathCreate(10f, 10f)
            pathAppendLineTo(pathId, 90f, 90f)
            pathAppendLineTo(pathId, 10f, 90f)
            pathAppendClose(pathId)
            drawPath(pathId)
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Ignore("b/440318500")
    @Test
    fun drawTextOnPath() {
        val document = getCoreDocument {
            val path = Path()
            path.moveTo(10f, 50f)
            path.lineTo(90f, 50f)
            drawTextOnPath("Text on path", path, 0f, 0f)
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawTextRun() {
        val document = getCoreDocument { drawTextRun("Hello World", 0, 11, 0, 11, 10f, 50f, false) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawTextAnchored() {
        val document = getCoreDocument { drawTextAnchored("Anchored", 50f, 50f, 0.5f, 0.5f, 0) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawTweenPath() {
        val document = getCoreDocument {
            val path1Id = pathCreate(10f, 10f)
            pathAppendLineTo(path1Id, 90f, 10f)
            pathAppendLineTo(path1Id, 90f, 90f)
            pathAppendLineTo(path1Id, 10f, 90f)
            pathAppendClose(path1Id)

            val path2Id = pathCreate(50f, 50f)
            pathAppendLineTo(path2Id, 90f, 10f)
            pathAppendLineTo(path2Id, 90f, 90f)
            pathAppendLineTo(path2Id, 10f, 90f)
            pathAppendClose(path2Id)
            drawTweenPath(path1Id, path2Id, 0.5f, 0f, 1f)
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }
}
