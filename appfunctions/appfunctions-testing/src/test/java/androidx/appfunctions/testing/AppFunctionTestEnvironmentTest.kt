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

package androidx.appfunctions.testing

import android.os.Build
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class AppFunctionTestEnvironmentTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val appFunctionTestEnvironment = AppFunctionTestEnvironment(targetContext)

    @Test
    fun returnedAppFunctionManagerCompat_observeApi_returnsAllAppFunctions() =
        runBlocking<Unit> {
            val appFunctionManagerCompat = appFunctionTestEnvironment.getAppFunctionManagerCompat()

            val results =
                appFunctionManagerCompat
                    .observeAppFunctions(
                        AppFunctionSearchSpec(packageNames = setOf(context.packageName))
                    )
                    .take(1)
                    .toList()

            assertThat(results.single()).hasSize(5)
        }
}
