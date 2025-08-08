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

package androidx.camera.lifecycle

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.camera.core.impl.utils.ContextUtil

/** A singleton to manage the [LifecycleCameraRepository]s, keyed by the device ID. */
internal object LifecycleCameraRepositories {
    private val repositoryMap = mutableMapOf<Int, LifecycleCameraRepository>()

    /**
     * Gets the [LifecycleCameraRepository] instance by the given [Context].
     *
     * If the [Context] holds different device IDs, this method returns different instance of
     * [LifecycleCameraRepository]. [LifecycleCamera] managed by different repository instances will
     * not interfere with each other.
     *
     * @param context The [Context] to get the [LifecycleCameraRepository] instance. If not set, the
     *   default instance will be returned.
     * @return The [LifecycleCameraRepository] instance.
     */
    @JvmStatic
    fun getInstance(context: Context? = null): LifecycleCameraRepository {
        val deviceId = context?.let(ContextUtil::getDeviceId) ?: ContextUtil.getDefaultDeviceId()
        return synchronized(repositoryMap) {
            repositoryMap.getOrPut(deviceId) { LifecycleCameraRepository(deviceId) }
        }
    }

    /**
     * Clears all cached repositories.
     *
     * This is intended for testing purposes to ensure a clean state between tests.
     */
    @VisibleForTesting
    internal fun clear() {
        synchronized(repositoryMap) { repositoryMap.clear() }
    }
}
