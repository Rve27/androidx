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
import androidx.security.state.UpdateInfo
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Base class for implementing the AndroidX Security State Update Provider service.
 *
 * This abstract class provides the foundational implementation for the `IUpdateInfoService` AIDL
 * interface. It manages the complexity of serving security update information to client
 * applications, enforcing a consistent contract for data freshness and resource usage.
 *
 * ### Responsibilities
 * * **IPC Handling:** Manages the AIDL binding process and verifies Intent actions.
 * * **Concurrency Control:** Implements Double-Checked Locking to prevent "thundering herd"
 *   scenarios where multiple clients trigger parallel network requests.
 * * **Rate Limiting:** Enforces a default throttling policy (e.g., maximum one check per hour) to
 *   protect backend infrastructure.
 * * **Caching:** Automatically persists fetched results and freshness metadata to local storage.
 *
 * ### Host Implementation
 * Host applications (such as the System Updater, Google Play Store, or OEM-specific updaters) must
 * extend this class and implement the [fetchUpdates] method to provide the actual network logic.
 *
 * ### Manifest Declaration
 * To expose this service, you must declare it in your `AndroidManifest.xml` with the
 * `exported="true"` attribute and an intent-filter for the `UPDATE_INFO_SERVICE` action:
 * ```xml
 * <service
 *     android:name=".MyUpdateInfoService"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="androidx.security.state.provider.UPDATE_INFO_SERVICE" />
 *     </intent-filter>
 * </service>
 * ```
 */
public abstract class UpdateInfoService : Service() {

    private companion object {
        private const val TAG = "UpdateInfoService"
        private const val ACTION_BIND = "androidx.security.state.provider.UPDATE_INFO_SERVICE"
    }

    /** Lazy-initialized manager for accessing persistent update information. */
    private val updateInfoManager by lazy { UpdateInfoManager(this) }

    /**
     * Rate limiter to prevent abuse of the backend. Defaults to
     * [SharedPreferencesUpdateCheckRateLimiter].
     */
    private val checkRateLimiter: UpdateCheckRateLimiter by lazy {
        SharedPreferencesUpdateCheckRateLimiter(this)
    }

    /**
     * Mutex to serialize network requests across concurrent binder threads. This ensures that if
     * multiple clients bind and request updates simultaneously, only one network request is
     * executed.
     */
    private val networkLock = Mutex()

    /** The implementation of the AIDL interface. */
    private val binder: IUpdateInfoService.Stub =
        object : IUpdateInfoService.Stub() {
            override fun listAvailableUpdates(): UpdateCheckResult {
                // AIDL calls are blocking by default. We use runBlocking to bridge
                // to our suspendable internal logic, allowing us to use Coroutines
                // for the network fetch and locking.
                return runBlocking { handleListUpdates() }
            }
        }

    /**
     * Internal orchestrator for handling the update check request.
     *
     * This implements the "Double-Checked Locking" pattern:
     * 1. Check if cache is fresh (Fast path, no lock).
     * 2. Acquire lock.
     * 3. Check if cache is fresh again (Double-check).
     * 4. Check rate limits.
     * 5. Fetch from network.
     */
    private suspend fun handleListUpdates(): UpdateCheckResult {
        // 1. Fast Path: Check policy without acquiring lock
        if (!shouldFetchUpdates()) {
            return getCachedResult()
        }

        // 2. Slow Path: Acquire lock to prevent parallel network requests
        networkLock.withLock {
            // 3. Double-Check: Re-verify policy inside the lock
            // Another thread might have updated the cache while we were waiting.
            if (!shouldFetchUpdates()) {
                return getCachedResult()
            }

            // 4. Rate Limit Check: Protect the backend
            if (shouldThrottle()) {
                Log.i(TAG, "Update check skipped due to rate limiting.")
                return getCachedResult()
            }

            // 5. Execute Fetch
            try {
                // Record the attempt *before* the network call to ensure we count it
                // even if it times out or fails late.
                checkRateLimiter.noteAttempt()

                // BLOCKING: Call the host's implementation
                val newUpdates = fetchUpdates()

                // Success: Update cache and timestamp
                newUpdates.forEach { update -> updateInfoManager.registerUpdate(update) }
                updateInfoManager.setLastCheckTimeMillis(System.currentTimeMillis())
            } catch (e: Exception) {
                onFetchFailed(e)
            }

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

    /**
     * Performs the actual network request to fetch fresh updates from the backend.
     *
     * Implementations of this method should:
     * * Block until the network request completes.
     * * Return the list of updates found.
     * * Throw an exception if the network request fails (this will be caught and logged).
     *
     * **Note:** This method is only called if [shouldFetchUpdates] returns `true` and the rate
     * limiter allows the request.
     *
     * @return A list of [UpdateInfo] objects currently available for the device.
     */
    protected abstract suspend fun fetchUpdates(): List<UpdateInfo>

    /**
     * Determines if the local cache is stale and a refresh should be attempted.
     *
     * The default implementation returns `true` if the data is older than 1 hour. Hosts can
     * override this to implement custom caching policies (e.g., 4 hours, 24 hours).
     *
     * @return `true` if a network fetch should be attempted.
     */
    protected open fun shouldFetchUpdates(): Boolean {
        val lastCheckTimeMillis = updateInfoManager.getLastCheckTimeMillis()
        val dataAge = System.currentTimeMillis() - lastCheckTimeMillis
        return dataAge > TimeUnit.HOURS.toMillis(1)
    }

    /**
     * Checks if the operation should be throttled based on the rate limiter.
     *
     * The default implementation delegates to an internal rate limiter that enforces a cooling-off
     * period (e.g., one check per hour). Hosts can override this to inject custom rate limiting
     * logic (e.g., battery checks).
     *
     * @return `true` if the request should be blocked.
     */
    protected open fun shouldThrottle(): Boolean {
        return checkRateLimiter.shouldThrottle()
    }

    /**
     * Callback for handling exceptions that occur during [fetchUpdates].
     *
     * The default implementation logs the error to Logcat. Hosts can override this to report errors
     * to their own telemetry or analytics systems.
     *
     * @param e The exception thrown by [fetchUpdates].
     */
    protected open fun onFetchFailed(e: Exception) {
        Log.w(TAG, "Failed to fetch updates", e)
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
