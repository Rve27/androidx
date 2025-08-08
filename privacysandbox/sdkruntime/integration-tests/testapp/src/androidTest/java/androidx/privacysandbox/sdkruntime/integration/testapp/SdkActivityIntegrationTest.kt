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

package androidx.privacysandbox.sdkruntime.integration.testapp

import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.sdkruntime.integration.testaidl.ISdkActivityApi
import androidx.privacysandbox.sdkruntime.integration.testaidl.ISdkActivityHandler
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SdkActivityIntegrationTest {

    @get:Rule val testSetup = IntegrationTestSetupRule()

    @Test
    fun startSdkActivityTest() = runTest {
        val uiDevice = testSetup.uiDevice
        val testAppApi = testSetup.testAppApi()
        val testSdk = testAppApi.getOrLoadTestSdk()

        val handler = TestActivityHandler()
        val token = testSdk.registerSdkActivityHandler(handler)
        testAppApi.startSdkActivity(token)
        val startedActivity = handler.waitForSdkActivity()

        val activityIsVisible = uiDevice.wait(Until.hasObject(SDK_ACTIVITY_LAYOUT), TIMEOUT_MS)
        assertThat(activityIsVisible).isTrue()

        startedActivity.finishActivity()
        val activityIsGone = uiDevice.wait(Until.gone(SDK_ACTIVITY_LAYOUT), TIMEOUT_MS)
        assertThat(activityIsGone).isTrue()

        // App Activity should be on top again
        assertThat(testSetup.activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
    }

    private class TestActivityHandler() : ISdkActivityHandler.Stub() {

        var activity: ISdkActivityApi? = null
        val async = CountDownLatch(1)

        override fun onActivityCreated(activityApi: ISdkActivityApi) {
            activity = activityApi
            async.countDown()
        }

        fun waitForSdkActivity(): ISdkActivityApi {
            if (!async.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw TimeoutException("Timeout for onActivityCreated()")
            }
            return activity!!
        }
    }

    companion object {
        const val TIMEOUT_MS = 5000.toLong()
        val SDK_ACTIVITY_LAYOUT: BySelector = By.res(TestAppApi.TEST_SDK_NAME, "sdkActivityLayout")
    }
}
