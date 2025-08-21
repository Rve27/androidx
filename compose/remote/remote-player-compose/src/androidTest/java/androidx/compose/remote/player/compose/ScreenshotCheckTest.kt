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

package androidx.compose.remote.player.compose

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.layout.RemoteCanvas
import androidx.compose.remote.frontend.layout.RemoteOffset
import androidx.compose.remote.frontend.state.rf
import androidx.compose.remote.player.compose.test.rule.RemoteComposeScreenshotTestRule
import androidx.compose.remote.player.view.RemoteComposeDocument
import androidx.compose.ui.graphics.Color
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests to check that the screenshot tests are working. To be removed once real tests are
 * introduced.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class ScreenshotCheckTest {

    @get:Rule
    val remoteComposeTestRule = RemoteComposeScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun screenshotTests_withFrontend() {
        remoteComposeTestRule.runScreenshotTest {
            RemoteCanvas { drawCircle(Color.Red, 100f.rf, RemoteOffset(100f, 100f)) }
        }
    }

    @Test
    fun screenshotTests_withCoreDocument() {
        val rcContext =
            RemoteComposeContextAndroid(
                AndroidxPlatformServices(),
                RemoteComposeWriter.HTag(Header.DOC_CONTENT_DESCRIPTION, "Test"),
                RemoteComposeWriter.HTag(Header.DOC_DESIRED_FPS, 120),
            ) {
                painter.setColor(android.graphics.Color.RED).commit()
                drawCircle(100f, 100f, 100f)
            }
        val document =
            RemoteComposeDocument(
                    ByteArrayInputStream(
                        rcContext.mRemoteWriter.buffer(),
                        0,
                        rcContext.bufferSize(),
                    )
                )
                .document

        remoteComposeTestRule.runScreenshotTest(document = document)
    }
}
