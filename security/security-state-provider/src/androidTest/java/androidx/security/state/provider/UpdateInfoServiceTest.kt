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

package androidx.security.state.provider

import android.content.Context
import android.content.Intent
import androidx.security.state.IUpdateInfoService
import androidx.security.state.UpdateInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateInfoServiceTest {

    private lateinit var context: Context
    private lateinit var service: TestUpdateInfoService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = TestUpdateInfoService()
        service.attach(context)
    }

    @Test
    fun onBind_returnsBinder_forCorrectAction() {
        val intent = Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE")
        val binder = service.onBind(intent)
        assertNotNull("Should return binder for matching action", binder)
    }

    @Test
    fun onBind_returnsNull_forIncorrectAction() {
        val intent = Intent("com.example.WRONG_ACTION")
        val binder = service.onBind(intent)
        assertNull("Should reject incorrect action", binder)
    }

    @Test
    fun listAvailableUpdates_returnsCachedDataFromManager() {
        // 1. Setup: Seed the SharedPreferences with data
        val updateInfo =
            UpdateInfo.Builder()
                .setComponent("SYSTEM")
                .setSecurityPatchLevel("2025-01-01")
                .setPublishedDateMillis(1L)
                .setLastCheckTimeMillis(1000L)
                .build()

        // Use the Manager directly to seed data
        val manager = UpdateInfoManager(context)
        manager.registerUpdate(updateInfo)
        manager.setLastCheckTimeMillis(1000L)

        // 2. Action: Call the Service method via the Binder interface
        val binder =
            service.onBind(Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE"))
                as IUpdateInfoService

        val result = binder.listAvailableUpdates()

        // 3. Verify
        assertEquals("Should return 1 update", 1, result.updates.size)
        assertEquals("SYSTEM", result.updates[0].component)
        assertEquals("2025-01-01", result.updates[0].securityPatchLevel)
        assertEquals(1L, result.updates[0].publishedDateMillis)
        assertEquals(1000L, result.updates[0].lastCheckTimeMillis)
        assertEquals(1000L, result.lastCheckTimeMillis)
    }
}
