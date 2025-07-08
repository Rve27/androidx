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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PagerScaffoldScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun horizontalPagerScaffold_indicator_on_bottom(@TestParameter screenSize: ScreenSize) {
        verifyHorizontalPagerScaffold(screenSize = screenSize)
    }

    @Test
    fun verticalPagerScaffold_LTR_indicator_on_left(@TestParameter screenSize: ScreenSize) {
        verifyVerticalPagerScaffold(screenSize = screenSize)
    }

    @Test
    fun verticalPagerScaffold_RTL_indicator_on_right(@TestParameter screenSize: ScreenSize) {
        verifyVerticalPagerScaffold(layoutDirection = LayoutDirection.Rtl, screenSize = screenSize)
    }

    private fun verifyHorizontalPagerScaffold(screenSize: ScreenSize = ScreenSize.SMALL) {
        rule.setContentWithTheme {
            ScreenConfiguration(screenSize.size, isRound = true) {
                Box(modifier = Modifier.testTag(TEST_TAG).fillMaxSize()) {
                    AppScaffold {
                        val pagerState = rememberPagerState(pageCount = { 3 })

                        HorizontalPagerScaffold(
                            pagerState = pagerState,
                            pageIndicator = {
                                Box(
                                    modifier =
                                        Modifier.width(150.dp).height(20.dp).background(Color.Blue)
                                )
                            },
                        ) {
                            HorizontalPager(state = pagerState) { page ->
                                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                    ScreenScaffold {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(text = "Page #$page")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        val screenshot = rule.onNodeWithTag(TEST_TAG).captureToImage()

        if (GENERATE_SCREENSHOTS) {
            screenshot.writeToDevice(testName.goldenIdentifier())
        }

        screenshot.assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }

    private fun verifyVerticalPagerScaffold(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        screenSize: ScreenSize = ScreenSize.SMALL,
    ) {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(layoutDirection)
            ) {
                ScreenConfiguration(screenSize.size, isRound = true) {
                    Box(modifier = Modifier.testTag(TEST_TAG).fillMaxSize()) {
                        AppScaffold {
                            val pagerState = rememberPagerState(pageCount = { 3 })

                            VerticalPagerScaffold(
                                pagerState = pagerState,
                                pageIndicator = {
                                    Box(
                                        modifier =
                                            Modifier.width(20.dp)
                                                .height(150.dp)
                                                .background(Color.Blue)
                                    )
                                },
                            ) {
                                VerticalPager(state = pagerState) { page ->
                                    AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                        ScreenScaffold {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                            ) {
                                                Text(text = "Page #$page")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        val screenshot = rule.onNodeWithTag(TEST_TAG).captureToImage()

        if (GENERATE_SCREENSHOTS) {
            screenshot.writeToDevice(testName.goldenIdentifier())
        }

        screenshot.assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }
}

// To re-generate screenshots locally:
// 1) Set GENERATE_SCREENSHOTS = true
// 2) Run the tests on a Medium Phone API 35 emulator
// 3) Run the following commands:
// adb pull storage/emulated/0/Android/data/androidx.wear.compose.material3.test/cache/screenshots \
//     ../../golden/wear/compose/compose-material3
// mv ../../golden/wear/compose/compose-material3/screenshots/* \
//     ../../golden/wear/compose/compose-material3
// rmdir ../../golden/wear/compose/compose-material3/screenshots
private const val GENERATE_SCREENSHOTS = false
