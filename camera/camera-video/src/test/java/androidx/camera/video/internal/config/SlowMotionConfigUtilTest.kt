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

import android.os.Build
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_HIGH_SPEED_1080P
import androidx.camera.testing.impl.FrameRateUtil.FPS_120_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_240_240
import androidx.camera.testing.impl.FrameRateUtil.FPS_30
import androidx.camera.testing.impl.FrameRateUtil.FPS_60
import androidx.camera.testing.impl.FrameRateUtil.FPS_960_960
import androidx.camera.video.SlowMotionConfig
import androidx.camera.video.Speed
import androidx.camera.video.Speed.Companion.SPEED_1_32X
import androidx.camera.video.Speed.Companion.SPEED_1_4X
import androidx.camera.video.Speed.Companion.SPEED_1_8X
import androidx.camera.video.internal.config.SlowMotionConfigUtil.resolveSampleRates
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SlowMotionConfigUtilTest {

    private val defaultProfiles = PROFILES_HIGH_SPEED_1080P

    @Test
    fun resolveSlowMotionConfigs_noAudio_returnsConfigs() {
        // Arrange
        val captureFrameRateRanges = setOf(FPS_120_120, FPS_240_240)
        val encodeFrameRate = FPS_30
        val withAudioEnabled = false

        // Act
        val result =
            SlowMotionConfigUtil.resolveSlowMotionConfigs(
                defaultProfiles,
                captureFrameRateRanges,
                encodeFrameRate,
                withAudioEnabled
            )

        // Assert
        assertThat(result)
            .containsExactly(
                SlowMotionConfig(FPS_120_120, FPS_30),
                SlowMotionConfig(FPS_240_240, FPS_30)
            )
    }

    @Test
    fun resolveSlowMotionConfigs_withAudio_returnsConfigsWithSupportedSampleRates() {
        // Arrange
        assumeTrue(isAudioSpeedSupported(SPEED_1_4X, SPEED_1_8X))
        val captureFrameRateRanges = setOf(FPS_120_120, FPS_240_240)
        val encodeFrameRate = FPS_30
        val withAudioEnabled = true

        // Act
        val result =
            SlowMotionConfigUtil.resolveSlowMotionConfigs(
                defaultProfiles,
                captureFrameRateRanges,
                encodeFrameRate,
                withAudioEnabled
            )

        // Assert
        assertThat(result)
            .containsExactly(
                SlowMotionConfig(FPS_120_120, FPS_30),
                SlowMotionConfig(FPS_240_240, FPS_30)
            )
    }

    @Test
    fun resolveSlowMotionConfigs_withAudioAndUnsupportedSampleRates_skipsUnsupportedConfigs() {
        // Arrange: ensure SPEED_1_32X is not supported by audio
        assumeTrue(!isAudioSpeedSupported(SPEED_1_32X))
        // Arrange: generate a SPEED_1_32X slow-motion config
        val captureFrameRateRanges = setOf(FPS_960_960)
        val encodeFrameRate = FPS_30
        val withAudioEnabled = true

        // Act
        val result =
            SlowMotionConfigUtil.resolveSlowMotionConfigs(
                defaultProfiles,
                captureFrameRateRanges,
                encodeFrameRate,
                withAudioEnabled
            )

        // Assert: no common audio sample rates can fulfill SPEED_1_32X.
        assertThat(result).isEmpty()
    }

    @Test
    fun resolveSlowMotionConfigs_supportedSpeedsProvided_returnsConfigsWithProvidedSpeedsOnly() {
        // Arrange
        val captureFrameRateRanges = setOf(FPS_120_120, FPS_240_240)
        val encodeFrameRate = FPS_60
        val withAudioEnabled = false
        val supportedSpeeds = listOf(SPEED_1_4X, SPEED_1_8X)

        // Act
        val result =
            SlowMotionConfigUtil.resolveSlowMotionConfigs(
                defaultProfiles,
                captureFrameRateRanges,
                encodeFrameRate,
                withAudioEnabled,
                supportedSpeeds
            )

        // Assert: 120-60 is not supported.
        assertThat(result).containsExactly(SlowMotionConfig(FPS_240_240, FPS_60))
    }

    private fun isAudioSpeedSupported(vararg speeds: Speed): Boolean {
        val audioProfile = defaultProfiles.audioProfiles[0] // should not be empty.

        return speeds.all { speed -> resolveSampleRates(speed, audioProfile) != null }
    }
}
