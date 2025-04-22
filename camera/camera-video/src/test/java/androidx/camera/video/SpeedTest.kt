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

package androidx.camera.video

import android.util.Rational
import androidx.camera.video.Speed.Companion.SPEED_1_4X
import androidx.camera.video.Speed.Companion.SPEED_AUTO
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class SpeedTest {

    @Test
    fun constructor_validRatio_createsSpeedObject() {
        assertThat(Speed(Rational(1, 2)).multiplier).isEqualTo(Rational(1, 2))
        assertThat(Speed(Rational(0, 0)).multiplier) // NaN is Speed.AUTO
            .isEqualTo(Rational.NaN)
    }

    @Test
    fun constructor_invalidRatio_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { Speed(Rational(-1, 2)) }
        assertThrows(IllegalArgumentException::class.java) { Speed(Rational(1, -2)) }
        assertThrows(IllegalArgumentException::class.java) { Speed(Rational(0, 1)) }
        assertThrows(IllegalArgumentException::class.java) { Speed(Rational(1, 0)) }
    }

    @Test
    fun toString_returnsCorrectString() {
        val speedNormal = Speed(Rational(1, 1))
        assertThat(speedNormal.toString()).isEqualTo("1x")

        val speedDown = Speed(Rational(1, 4))
        assertThat(speedDown.toString()).isEqualTo("1/4x")

        val speedUp = Speed(Rational(2, 1))
        assertThat(speedUp.toString()).isEqualTo("2x")
    }

    @Test
    fun companionObject_speedAuto_isCorrect() {
        assertThat(SPEED_AUTO.multiplier).isEqualTo(Rational.NaN)
    }

    @Test
    fun toCaptureEncodeRatio_speedAuto_returnsNan() {
        assertThat(Speed.toCaptureEncodeRatio(SPEED_AUTO)).isEqualTo(Rational.NaN)
    }

    @Test
    fun toCaptureEncodeRatio_returnsCorrectRatio() {
        assertThat(Speed.toCaptureEncodeRatio(Speed(Rational(2, 1)))).isEqualTo(Rational(1, 2))
        assertThat(Speed.toCaptureEncodeRatio(Speed(Rational(10, 1)))).isEqualTo(Rational(1, 10))
    }

    @Test
    fun fromCaptureEncodeRates_validRates_returnsCorrectSpeed() {
        val speed = Speed.fromCaptureEncodeRates(120, 30)
        assertThat(speed).isEqualTo(SPEED_1_4X)
    }

    @Test
    fun fromCaptureEncodeRates_invalidRates_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { Speed.fromCaptureEncodeRates(0, 1) }
        assertThrows(IllegalArgumentException::class.java) { Speed.fromCaptureEncodeRates(1, 0) }
        assertThrows(IllegalArgumentException::class.java) { Speed.fromCaptureEncodeRates(0, 0) }
        assertThrows(IllegalArgumentException::class.java) { Speed.fromCaptureEncodeRates(-1, -1) }
    }

    @Test
    fun fromCaptureEncodeRatio_validRatio_returnsCorrectSpeed() {
        val speed = Speed.fromCaptureEncodeRatio(Rational(120, 30))
        assertThat(speed).isEqualTo(SPEED_1_4X)
    }

    @Test
    fun fromCaptureEncodeRatio_invalidRatio_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            Speed.fromCaptureEncodeRatio(Rational(0, 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Speed.fromCaptureEncodeRatio(Rational(1, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Speed.fromCaptureEncodeRatio(Rational(0, 0))
        }
    }
}
