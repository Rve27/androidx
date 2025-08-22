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
import androidx.compose.remote.core.Operations.PROFILE_WIDGETS
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.rule.RemoteComposeScreenshotTestRule
import androidx.compose.remote.player.compose.test.util.createBitmap
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
    fun drawBitmap() {
        val document = getCoreDocument {
            drawBitmap(
                image = createBitmap() as Object,
                width = 100,
                height = 100,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawBitmapWithBounds() {
        val document = getCoreDocument {
            drawBitmap(
                image = createBitmap(),
                left = 0f,
                top = 0f,
                right = 100f,
                bottom = 100f,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawBitmapWithId() {
        val document = getCoreDocument {
            val imageId = storeBitmap(createBitmap())
            drawBitmap(
                imageId = imageId,
                left = 0f,
                top = 0f,
                right = 100f,
                bottom = 100f,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Ignore("b/440385172")
    @Test
    fun drawBitmapWithIdAndPosition() {
        val document = getCoreDocument {
            val imageId = storeBitmap(createBitmap())
            drawBitmap(
                imageId = imageId,
                left = 10f,
                top = 10f,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawScaledBitmap() {
        val document = getCoreDocument {
            drawScaledBitmap(
                image = createBitmap() as Object,
                srcLeft = 0f,
                srcTop = 0f,
                srcRight = 100f,
                srcBottom = 100f,
                dstLeft = 0f,
                dstTop = 0f,
                dstRight = 100f,
                dstBottom = 100f,
                scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
                scaleFactor = 0f,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawScaledBitmap_partially_samePosition() {
        val document = getCoreDocument {
            drawScaledBitmap(
                image = createBitmap() as Object,
                srcLeft = 0f,
                srcTop = 0f,
                srcRight = 50f,
                srcBottom = 50f,
                dstLeft = 0f,
                dstTop = 0f,
                dstRight = 50f,
                dstBottom = 50f,
                scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
                scaleFactor = 0f,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawScaledBitmap_partially_differentPosition() {
        val document = getCoreDocument {
            drawScaledBitmap(
                image = createBitmap() as Object,
                srcLeft = 0f,
                srcTop = 0f,
                srcRight = 50f,
                srcBottom = 50f,
                dstLeft = 50f,
                dstTop = 50f,
                dstRight = 100f,
                dstBottom = 100f,
                scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
                scaleFactor = 0f,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawScaledBitmapWithId() {
        val document = getCoreDocument {
            val imageId = storeBitmap(createBitmap())
            drawScaledBitmap(
                imageId = imageId,
                srcLeft = 0f,
                srcTop = 0f,
                srcRight = 100f,
                srcBottom = 100f,
                dstLeft = 0f,
                dstTop = 0f,
                dstRight = 100f,
                dstBottom = 100f,
                scaleType = RemoteComposeWriter.IMAGE_SCALE_NONE,
                scaleFactor = 0f,
                contentDescription = "contentDescription",
            )
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

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

    @Test
    fun drawPathWithPath() {
        val document = getCoreDocument {
            val path = Path()
            path.moveTo(10f, 10f)
            path.lineTo(90f, 90f)
            path.lineTo(10f, 90f)
            path.close()
            drawPath(path)
        }

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

    @Ignore("b/440318500")
    @Test
    fun drawTextOnPathWithTextId() {
        val document = getCoreDocument {
            val textId = textCreateId("Text on path")
            val path = Path()
            path.moveTo(10f, 50f)
            path.lineTo(90f, 50f)
            drawTextOnPath(textId, path, 0f, 0f)
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawTextRun() {
        val document = getCoreDocument { drawTextRun("Hello World", 0, 11, 0, 11, 10f, 50f, false) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawTextRunWithTextId() {
        val document = getCoreDocument {
            val textId = textCreateId("Hello World")
            drawTextRun(textId, 0, 11, 0, 11, 10f, 50f, false)
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawBitmapFontTextRun() {
        val document = getCoreDocument {
            val textId = textCreateId("AB")
            val bitmapId = storeBitmap(createBitmap())
            val glyphs =
                arrayOf(
                    BitmapFontData.Glyph("A", bitmapId, 0, 0, 0, 0, 10, 10),
                    BitmapFontData.Glyph("B", bitmapId, 10, 0, 0, 0, 10, 10),
                )
            val bitmapFontId = addBitmapFont(glyphs)
            drawBitmapFontTextRun(textId, bitmapFontId, 0, 2, 10f, 50f)
        }

        // Not rendering glyphs b/440500282
        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawTextAnchored() {
        val document = getCoreDocument { drawTextAnchored("Anchored", 50f, 50f, 0.5f, 0.5f, 0) }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawTextAnchoredWithStrId() {
        val document = getCoreDocument {
            val strId = textCreateId("Anchored")
            drawTextAnchored(strId, 50f, 50f, 0.5f, 0.5f, 0)
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawBitmapTextAnchored() {
        val document =
            getCoreDocument(
                extraTags = arrayOf(RemoteComposeWriter.HTag(Header.DOC_PROFILES, PROFILE_WIDGETS))
            ) {
                val bitmapId = storeBitmap(createBitmap())
                val glyphs =
                    arrayOf(
                        BitmapFontData.Glyph("A", bitmapId, 0, 0, 0, 0, 10, 10),
                        BitmapFontData.Glyph("B", bitmapId, 10, 0, 0, 0, 10, 10),
                    )
                val bitmapFontId = addBitmapFont(glyphs)
                drawBitmapTextAnchored("AB", bitmapFontId, 0f, 2f, 50f, 50f, 0.5f, 0.5f)
            }

        // Not rendering glyphs b/440500282
        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun drawBitmapTextAnchoredWithTextId() {
        val document =
            getCoreDocument(
                extraTags = arrayOf(RemoteComposeWriter.HTag(Header.DOC_PROFILES, PROFILE_WIDGETS))
            ) {
                val textId = textCreateId("AB")
                val bitmapId = storeBitmap(createBitmap())
                val glyphs =
                    arrayOf(
                        BitmapFontData.Glyph("A", bitmapId, 0, 0, 0, 0, 10, 10),
                        BitmapFontData.Glyph("B", bitmapId, 10, 0, 0, 0, 10, 10),
                    )
                val bitmapFontId = addBitmapFont(glyphs)
                drawBitmapTextAnchored(textId, bitmapFontId, 0f, 2f, 50f, 50f, 0.5f, 0.5f)
            }

        // Not rendering glyphs b/440500282
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

    @Test
    fun drawTweenPathWithPath() {
        val document = getCoreDocument {
            val path1 = Path()
            path1.moveTo(10f, 10f)
            path1.lineTo(90f, 10f)
            path1.lineTo(90f, 90f)
            path1.lineTo(10f, 90f)
            path1.close()

            val path2 = Path()
            path2.moveTo(50f, 50f)
            path2.lineTo(90f, 10f)
            path2.lineTo(90f, 90f)
            path2.lineTo(10f, 90f)
            path2.close()

            drawTweenPath(path1, path2, 0.5f, 0f, 1f)
        }

        remoteComposeTestRule.runScreenshotTest(document = document)
    }
}
