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

package androidx.compose.ui.graphics

import android.graphics.Rect
import android.graphics.Region
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRectCornersKey
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRectKey
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRegionKey
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class GraphicsLayerSemanticsTest {

    @get:Rule val rule = createComposeRule()

    private val tag = "semantics-test-tag"
    private lateinit var androidComposeView: AndroidComposeView

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun rectangleShape_clip_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp).graphicsLayer(shape = RectangleShape, clip = true).testTag(tag)
            )
        }

        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    fun roundedCornerShape_clip_setsShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp), clip = true)
                    .testTag(tag)
            )
        }

        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
    }

    @Test
    fun roundedCornerShape_noClip_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp), clip = false)
                    .testTag(tag)
            )
        }

        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun roundedCornerShape_clip_rectFillsBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            @Suppress("DEPRECATION")
            val rect = info.extras.getParcelable<Rect>(ExtraDataShapeRectKey)!!
            with(rule.density) {
                assertThat(rect.left).isEqualTo(0)
                assertThat(rect.top).isEqualTo(0)
                assertThat(rect.right).isEqualTo(10.dp.roundToPx())
                assertThat(rect.bottom).isEqualTo(10.dp.roundToPx())

                assertThat(rect.width()).isEqualTo(boundsInScreen.width())
                assertThat(rect.height()).isEqualTo(boundsInScreen.height())
            }

            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
            val corners = info.extras.getFloatArray(ExtraDataShapeRectCornersKey)!!
            with(rule.density) {
                assertThat(corners)
                    .isEqualTo(
                        floatArrayOf(
                            1.dp.toPx(),
                            1.dp.toPx(),
                            2.dp.toPx(),
                            2.dp.toPx(),
                            3.dp.toPx(),
                            3.dp.toPx(),
                            4.dp.toPx(),
                            4.dp.toPx()
                        )
                    )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun roundedCornerShape_clipWithPadding_rectFillsDownsizedBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .padding(2.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            @Suppress("DEPRECATION")
            val rect = info.extras.getParcelable<Rect>(ExtraDataShapeRectKey)!!
            with(rule.density) {
                assertThat(rect.left).isEqualTo(0)
                assertThat(rect.top).isEqualTo(0)
                assertThat(rect.right).isEqualTo(6.dp.roundToPx())
                assertThat(rect.bottom).isEqualTo(6.dp.roundToPx())

                assertThat(rect.width()).isEqualTo(boundsInScreen.width())
                assertThat(rect.height()).isEqualTo(boundsInScreen.height())
            }

            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
            val corners = info.extras.getFloatArray(ExtraDataShapeRectCornersKey)!!
            with(rule.density) {
                assertThat(corners)
                    .isEqualTo(
                        floatArrayOf(
                            1.dp.toPx(),
                            1.dp.toPx(),
                            2.dp.toPx(),
                            2.dp.toPx(),
                            3.dp.toPx(),
                            3.dp.toPx(),
                            4.dp.toPx(),
                            4.dp.toPx()
                        )
                    )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun roundedCornerShape_doubleClipWithPadding_rectFillsOuterClipArea() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .padding(2.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp), clip = true)
                    .padding(2.dp)
                    .graphicsLayer(shape = RoundedCornerShape(4.dp, 3.dp, 2.dp, 1.dp), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            @Suppress("DEPRECATION")
            val rect = info.extras.getParcelable<Rect>(ExtraDataShapeRectKey)!!
            with(rule.density) {
                assertThat(rect.left).isEqualTo(0)
                assertThat(rect.top).isEqualTo(0)
                assertThat(rect.right).isEqualTo(6.dp.roundToPx())
                assertThat(rect.bottom).isEqualTo(6.dp.roundToPx())

                assertThat(rect.width()).isEqualTo(boundsInScreen.width())
                assertThat(rect.height()).isEqualTo(boundsInScreen.height())

                assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
                val corners = info.extras.getFloatArray(ExtraDataShapeRectCornersKey)!!
                with(rule.density) {
                    assertThat(corners)
                        .isEqualTo(
                            floatArrayOf(
                                1.dp.toPx(),
                                1.dp.toPx(),
                                2.dp.toPx(),
                                2.dp.toPx(),
                                3.dp.toPx(),
                                3.dp.toPx(),
                                4.dp.toPx(),
                                4.dp.toPx()
                            )
                        )
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_cutCornerShapeClip_boundingRectFillsBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = CutCornerShape(2.dp), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            val regionBounds = region.bounds
            with(rule.density) {
                assertThat(regionBounds.left).isEqualTo(0)
                assertThat(regionBounds.top).isEqualTo(0)
                assertThat(regionBounds.right).isEqualTo(10.dp.roundToPx())
                assertThat(regionBounds.bottom).isEqualTo(10.dp.roundToPx())

                assertThat(regionBounds.width()).isEqualTo(boundsInScreen.width())
                assertThat(regionBounds.height()).isEqualTo(boundsInScreen.height())
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_insetRectangleClip_shiftsBoundingRectWithinBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = InsetRectangle(insetPx = 1), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            val regionBounds = region.bounds
            with(rule.density) {
                assertThat(regionBounds.left).isEqualTo(1)
                assertThat(regionBounds.top).isEqualTo(1)
                assertThat(regionBounds.right).isEqualTo(10.dp.roundToPx())
                assertThat(regionBounds.bottom).isEqualTo(10.dp.roundToPx())

                assertThat(regionBounds.width()).isEqualTo(boundsInScreen.width() - 1)
                assertThat(regionBounds.height()).isEqualTo(boundsInScreen.height() - 1)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_insetRectangleClipWithPadding_shiftsBoundingRectWithinBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .padding(2.dp)
                    .graphicsLayer(shape = InsetRectangle(insetPx = 1), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            val regionBounds = region.bounds
            with(rule.density) {
                assertThat(regionBounds.left).isEqualTo(1)
                assertThat(regionBounds.top).isEqualTo(1)
                assertThat(regionBounds.right).isEqualTo(6.dp.roundToPx())
                assertThat(regionBounds.bottom).isEqualTo(6.dp.roundToPx())

                assertThat(regionBounds.width()).isEqualTo(boundsInScreen.width() - 1)
                assertThat(regionBounds.height()).isEqualTo(boundsInScreen.height() - 1)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun alphaChange_invalidatesSemanticsVisibility() {
        // Arrange.
        var alpha by mutableStateOf(0f)
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).graphicsLayer(alpha = alpha).testTag(tag))
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        rule.runOnIdle { assertThat(info.isVisibleToUser).isFalse() }

        // Act.
        rule.runOnIdle { alpha = 1f }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info2.isVisibleToUser).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun clipChange_invalidatesSemanticsProperty() {
        // Arrange.
        var shouldClip by mutableStateOf(false)
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp), clip = shouldClip)
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }

        // Act.
        rule.runOnIdle { shouldClip = true }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun shapeChange_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape by mutableStateOf(RectangleShape)
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).graphicsLayer(shape = currentShape, clip = true).testTag(tag))
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }

        // Act.
        rule.runOnIdle { currentShape = RoundedCornerShape(1.dp) }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
        }
    }

    private class InsetRectangle(val insetPx: Int) : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
            Outline.Generic(
                Path().apply {
                    moveTo(insetPx.toFloat(), insetPx.toFloat())
                    lineTo(size.width, insetPx.toFloat())
                    lineTo(size.width, size.height)
                    lineTo(insetPx.toFloat(), size.height)
                    close()
                }
            )
    }

    private fun ComposeContentTestRule.setContentWithAccessibilityEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            with(androidComposeView.composeAccessibilityDelegate) {
                accessibilityForceEnabledForTesting = true
                onSendAccessibilityEvent = { false }
            }
            content()
        }
    }

    private val View.composeAccessibilityDelegate: AndroidComposeViewAccessibilityDelegateCompat
        get() =
            ViewCompat.getAccessibilityDelegate(this)
                as AndroidComposeViewAccessibilityDelegateCompat

    private fun addExtraDataToAccessibilityNodeInfo(
        virtualViewId: Int,
        info: AccessibilityNodeInfoCompat,
        extra: String
    ) {
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(virtualViewId, info, extra, Bundle())
    }

    private fun AndroidComposeView.createAccessibilityNodeInfo(
        semanticsId: Int
    ): AccessibilityNodeInfoCompat {
        onSemanticsChange()
        val accNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(semanticsId)
        checkNotNull(accNodeInfo) { "Could not find semantics node with id = $semanticsId" }
        return AccessibilityNodeInfoCompat.wrap(accNodeInfo)
    }
}
