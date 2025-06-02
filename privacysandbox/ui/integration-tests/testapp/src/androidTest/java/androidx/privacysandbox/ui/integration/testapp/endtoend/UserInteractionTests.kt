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

package androidx.privacysandbox.ui.integration.testapp.endtoend

import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.UiFrameworkOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.ZOrderOption
import androidx.privacysandbox.ui.integration.testapp.fragments.FragmentOptions
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class UserInteractionTests(
    @UiFrameworkOption private val uiFrameworkOption: String,
    @MediationOption private val mediationOption: String,
    @ZOrderOption private val zOrdering: String,
) :
    AutomatedEndToEndTest(
        FragmentOptions.FRAGMENT_SCROLL_HIDDEN,
        uiFrameworkOption,
        mediationOption,
        zOrdering,
        adType = FragmentOptions.AD_TYPE_NON_WEBVIEW,
    ) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "uiFrameworkOption={0}, mediationOption={1}, zOrdering={2}"
        )
        fun parameters(): Collection<Array<String>> {
            return baseParameters()
        }

        const val CHROME_PACKAGE_NAME = "com.android.chrome"
    }

    private lateinit var device: UiDevice
    private lateinit var uiScrollable: UiScrollable
    private lateinit var contentViewUiObject: UiObject

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiScrollable = UiScrollable(UiSelector().scrollable(true))
        contentViewUiObject =
            uiScrollable.getChild(UiSelector().index(0)).getChild(UiSelector().index(0))
    }

    @Test
    fun clickOnRemoteViewTest() {
        clickOnContentView()

        ensureThatChromeStarted()

        device.pressBack()
    }

    internal fun clickOnContentView() {
        device.click(contentViewUiObject.bounds.centerX(), contentViewUiObject.bounds.centerY())
    }

    internal fun ensureThatChromeStarted() {
        assertThat(
                device.wait(
                    Until.hasObject(By.pkg(CHROME_PACKAGE_NAME).depth(0)),
                    UI_CHANGE_WAIT_MS,
                )
            )
            .isTrue()
    }
}
