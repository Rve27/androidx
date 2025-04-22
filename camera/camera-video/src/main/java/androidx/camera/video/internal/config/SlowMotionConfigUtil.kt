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

package androidx.camera.video.internal.config

import android.util.Range
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Logger
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.video.AudioSpec.SAMPLE_RATE_RANGE_AUTO
import androidx.camera.video.AudioSpec.SOURCE_FORMAT_PCM_16BIT
import androidx.camera.video.SlowMotionConfig
import androidx.camera.video.Speed
import androidx.camera.video.Speed.Companion.toCaptureEncodeRatio
import androidx.camera.video.internal.audio.AudioSettings.COMMON_SAMPLE_RATES
import androidx.camera.video.internal.audio.AudioSource
import androidx.camera.video.internal.config.AudioConfigUtil.AUDIO_CHANNEL_COUNT_DEFAULT
import androidx.camera.video.internal.config.AudioConfigUtil.AUDIO_SAMPLE_RATE_DEFAULT

/**
 * Utility object for resolving a list of [SlowMotionConfig] based on supported video and audio
 * profiles.
 */
public object SlowMotionConfigUtil {

    private const val TAG = "SlowMotionConfigUtil"

    /**
     * Resolves a list of compatible [SlowMotionConfig] based on the provided encoder profiles,
     * supported capture frame rate ranges, encode frame rate, and whether audio is enabled.
     *
     * @param profiles The encoder profiles.
     * @param captureFrameRateRanges The set of supported capture frame rate ranges on the device.
     * @param encodeFrameRate The target encode frame rate.
     * @param withAudioEnabled `true` if audio should be enabled in the slow-motion configurations,
     *   `false` otherwise.
     * @param supportedSpeeds An optional list of [Speed] objects that are explicitly supported. If
     *   provided, only configurations resulting in a speed within this list will be included. If
     *   `null`, all possible speeds based on the other parameters are considered.
     * @return A list of [SlowMotionConfig] that are compatible with the provided parameters.
     *   Returns an empty list if no compatible configurations are found.
     */
    @JvmStatic
    public fun resolveSlowMotionConfigs(
        profiles: EncoderProfilesProxy,
        captureFrameRateRanges: Set<Range<Int>>,
        encodeFrameRate: Int,
        withAudioEnabled: Boolean,
        supportedSpeeds: List<Speed>? = null
    ): List<SlowMotionConfig> {
        val slowMotionConfigs = mutableListOf<SlowMotionConfig>()

        for (captureFrameRateRange in captureFrameRateRanges) {
            val slowMotionConfig = SlowMotionConfig(captureFrameRateRange, encodeFrameRate)
            val speed = slowMotionConfig.speed

            if (supportedSpeeds != null && !supportedSpeeds.contains(speed)) {
                continue
            }

            if (withAudioEnabled) {
                val audioProfile = profiles.audioProfiles.getOrNull(0)
                // Check if the speed supported by audio.
                val resolvedSampleRates = resolveSampleRates(speed, audioProfile)
                if (resolvedSampleRates == null) {
                    continue
                }
            }

            slowMotionConfigs.add(slowMotionConfig)
        }

        return slowMotionConfigs
    }

    @VisibleForTesting
    internal fun resolveSampleRates(
        speed: Speed,
        audioProfile: AudioProfileProxy?
    ): CaptureEncodeRates? {
        // Resolve the sample rates and see if
        // * Capture rate is supported by AudioSource.
        // * Encode rate is in the common sample rates.
        val audioFormat = SOURCE_FORMAT_PCM_16BIT
        val channelCount = audioProfile?.channels ?: AUDIO_CHANNEL_COUNT_DEFAULT
        val defaultSampleRate = audioProfile?.sampleRate ?: AUDIO_SAMPLE_RATE_DEFAULT
        val captureEncodeRatio = toCaptureEncodeRatio(speed)

        val resolvedSampleRates =
            AudioConfigUtil.resolveSampleRates(
                SAMPLE_RATE_RANGE_AUTO,
                defaultSampleRate,
                channelCount,
                audioFormat,
                captureEncodeRatio
            )

        return when {
            !AudioSource.isSettingsSupported(
                resolvedSampleRates.captureRate,
                channelCount,
                audioFormat
            ) -> {
                Logger.w(
                    TAG,
                    "Capture sample rate (${resolvedSampleRates.captureRate}Hz) is not supported."
                )
                null
            }
            !COMMON_SAMPLE_RATES.contains(resolvedSampleRates.encodeRate) -> {
                Logger.w(
                    TAG,
                    "Encode sample rate (${resolvedSampleRates.encodeRate}Hz) is not in the common sample rates $COMMON_SAMPLE_RATES."
                )
                null
            }
            else -> resolvedSampleRates
        }
    }
}
