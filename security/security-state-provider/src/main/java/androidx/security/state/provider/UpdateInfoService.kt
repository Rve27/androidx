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

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.security.state.IUpdateInfoService
import androidx.security.state.UpdateCheckResult

/**
 * Base class for implementing the AndroidX Security State Update Provider service.
 *
 * Trusted system applications (e.g., GOTA, Google Play Store, OEM Updaters) should extend this
 * class to expose available security updates to the system.
 *
 * **Note:** In this version, the service operates in a "Read-Only" mode. It returns the currently
 * cached data but does not yet trigger real-time network checks. Future versions will add support
 * for on-demand fetching.
 */
public abstract class UpdateInfoService : Service() {

    private companion object {
        private const val TAG = "UpdateInfoService"
        private const val ACTION_BIND = "androidx.security.state.provider.UPDATE_INFO_SERVICE"
    }

    private val updateInfoManager by lazy { UpdateInfoManager(this) }

    /** The AIDL Stub implementation. Delegates the logic to the internal handler. */
    private val binder: IUpdateInfoService.Stub =
        object : IUpdateInfoService.Stub() {
            override fun listAvailableUpdates(): UpdateCheckResult {
                // TODO(b/465464190): Implement rate-limiting logic.
                // For the initial CL, we simply return the cached result.
                // Locking and rate-limiting logic will be added here in the future.
                return getCachedResult()
            }
        }

    /**
     * Handles the binding request.
     *
     * Verifies that the intent action matches the expected [ACTION_BIND]. If the action is missing
     * or incorrect, the binding is rejected to ensure correctness.
     */
    final override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == ACTION_BIND) {
            return binder
        }
        Log.w(TAG, "Rejected binding with unexpected action: ${intent?.action}")
        return null
    }

    /** Helper to construct the result from the current persistence layer. */
    private fun getCachedResult(): UpdateCheckResult {
        return UpdateCheckResult(
            providerPackageName = packageName,
            updates = updateInfoManager.getAllUpdates(),
            lastCheckTimeMillis = updateInfoManager.getLastCheckTimeMillis(),
        )
    }
}
