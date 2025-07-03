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

package androidx.xr.projected.permissions

import android.Manifest
import android.companion.virtual.VirtualDevice
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(AndroidJUnit4::class)
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class ProjectedPermissionsResultContractTest {

    private val appContext: Context = getApplicationContext()
    private val virtualDeviceManager = appContext.getSystemService(VirtualDeviceManager::class.java)
    private lateinit var virtualDevice: VirtualDevice

    @Before
    fun setUp() {
        createVirtualDevice()
        virtualDevice = virtualDeviceManager.virtualDevices.first()
    }

    @Test
    fun createIntent_isForProjectedActivity() {
        val deviceScopedContext = appContext.createDeviceContext(virtualDevice.deviceId)

        val intent =
            ProjectedPermissionsResultContract()
                .createIntent(deviceScopedContext, listOf(REQUEST_DATA_1, REQUEST_DATA_2))

        assertThat(intent.component!!.packageName).isEqualTo(appContext.packageName)
        assertThat(intent.component!!.className)
            .isEqualTo(GoToHostProjectedActivity::class.java.name)
    }

    private fun createVirtualDevice() {
        val virtualDeviceParamsBuilderClass =
            Class.forName("android.companion.virtual.VirtualDeviceParams\$Builder")
        val virtualDeviceParamsClass =
            Class.forName("android.companion.virtual.VirtualDeviceParams")
        var virtualDeviceParamsBuilder =
            ReflectionHelpers.callConstructor(virtualDeviceParamsBuilderClass)
        virtualDeviceParamsBuilder =
            ReflectionHelpers.callInstanceMethod(
                virtualDeviceParamsBuilder,
                "setName",
                ClassParameter(String::class.java, "ProjectionDevice"),
            )
        virtualDeviceParamsBuilder =
            ReflectionHelpers.callInstanceMethod(virtualDeviceParamsBuilder, "build")
        ReflectionHelpers.callInstanceMethod<Any?>(
            virtualDeviceManager,
            "createVirtualDevice",
            ClassParameter(Int::class.javaPrimitiveType, 1),
            ClassParameter(virtualDeviceParamsClass, virtualDeviceParamsBuilder),
        )
    }

    private companion object {
        val REQUEST_DATA_1 =
            ProjectedPermissionsRequestParams(
                permissions =
                    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA),
                rationale = "My rationale 1",
            )
        val REQUEST_DATA_2 =
            ProjectedPermissionsRequestParams(
                permissions = listOf(Manifest.permission.RECORD_AUDIO),
                rationale = "My rationale 2",
            )
    }
}
