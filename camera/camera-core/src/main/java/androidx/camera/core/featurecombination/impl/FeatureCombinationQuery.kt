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

package androidx.camera.core.featurecombination.impl

import android.util.Range
import androidx.camera.core.DynamicRange
import androidx.camera.core.featurecombination.impl.feature.VideoStabilizationFeature
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig

/**
 * Queries whether a combination of features is supported by utilizing the
 * [android.hardware.camera2.CameraDevice.CameraDeviceSetup] API.
 */
public interface FeatureCombinationQuery {
    /**
     * Represents the configuration parameters per stream, e.g. surface configuration, dynamic range
     * etc.
     *
     * @param surfaceConfig The surface configuration for the stream.
     * @param useCaseConfig The use case config for the stream.
     * @param dynamicRange The dynamic range for the stream.
     */
    public data class StreamConfig(
        public val surfaceConfig: SurfaceConfig,
        public val useCaseConfig: UseCaseConfig<*>,
        public val dynamicRange: DynamicRange,
    )

    /**
     * Represents the configuration parameters for querying feature combinations.
     *
     * @param streamConfigs A list of [StreamConfig].
     * @param fpsRange The requested FPS range.
     * @param stabilizationMode The requested video stabilization mode.
     */
    public data class Config(
        public val streamConfigs: List<StreamConfig>,
        public val fpsRange: Range<Int>,
        public val stabilizationMode: VideoStabilizationFeature.StabilizationMode,
    )

    /**
     * Queries whether a combination of features is supported.
     *
     * @param config The [Config] containing the configuration parameters denoting a feature
     *   combination.
     * @return `true` if the feature combination is supported, `false` otherwise.
     */
    public fun isSupported(config: Config): Boolean

    public companion object {
        @JvmField
        public val NO_OP_FEATURE_COMBINATION_QUERY: FeatureCombinationQuery =
            object : FeatureCombinationQuery {
                override fun isSupported(config: Config): Boolean {
                    return false
                }
            }
    }
}
