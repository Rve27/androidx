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
import androidx.annotation.RestrictTo
import androidx.camera.video.Speed.Companion.SPEED_1_16X
import androidx.camera.video.Speed.Companion.SPEED_1_4X
import androidx.camera.video.Speed.Companion.SPEED_1_8X
import androidx.camera.video.Speed.Companion.SPEED_AUTO

/**
 * Represents a playback or capture speed, often used for slow-motion presentations.
 *
 * This class provides several predefined, common slow-motion speed constants: [SPEED_1_4X],
 * [SPEED_1_8X] and [SPEED_1_16X].
 *
 * @param multiplier The multiplier representing the speed. For example, 0.5 (or 1/2) represents
 *   half speed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) // TODO(b/404096374): High-speed public API
public class Speed
@RestrictTo(RestrictTo.Scope.LIBRARY)
public constructor(public val multiplier: Rational) {
    init {
        require(
            (multiplier.numerator > 0 && multiplier.denominator > 0) || multiplier == Rational.NaN
        ) {
            "Invalid multiplier: $multiplier"
        }
    }

    private val name: String by lazy {
        if (multiplier == Rational.NaN) {
            "Auto"
        } else if (multiplier.numerator >= multiplier.denominator) {
            // ex: 1x, 2x, 3x
            "${multiplier.numerator}x"
        } else {
            // ex: 1/4x, 1/8x, 1/16x
            "${multiplier.numerator}/${multiplier.denominator}x"
        }
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Speed

        return multiplier == other.multiplier
    }

    override fun hashCode(): Int {
        return multiplier.hashCode()
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        /** Represents a speed of no preference. */
        @JvmField
        public val SPEED_AUTO: Speed = Speed(Rational.NaN)

        /** Represents a speed of 1/2x. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        public val SPEED_1_2X: Speed = Speed(Rational(1, 2))

        /** Represents a speed of 1/4x. */
        @JvmField public val SPEED_1_4X: Speed = Speed(Rational(1, 4))

        /** Represents a speed of 1/8x. */
        @JvmField public val SPEED_1_8X: Speed = Speed(Rational(1, 8))

        /** Represents a speed of 1/16x. */
        @JvmField public val SPEED_1_16X: Speed = Speed(Rational(1, 16))

        /** Represents a speed of 1/32x. */
        @JvmField public val SPEED_1_32X: Speed = Speed(Rational(1, 32))

        /**
         * Converts a [Speed] to a [Rational] representing the ratio between the capture rate and
         * the encode rate required to achieve the desired speed.
         *
         * If the input [speed] is [SPEED_AUTO], it returns [Rational.NaN] as the ratio is undefined
         * for automatic speed.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        public fun toCaptureEncodeRatio(speed: Speed): Rational =
            if (speed == SPEED_AUTO) {
                Rational.NaN
            } else {
                Rational(speed.multiplier.denominator, speed.multiplier.numerator)
            }

        /** Creates a [Speed] instance from the given capture rate and encode rate. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        public fun fromCaptureEncodeRates(captureRate: Int, encodeRate: Int): Speed {
            require(captureRate > 0 && encodeRate > 0)
            return Speed(Rational(encodeRate, captureRate))
        }

        /** Creates a [Speed] instance from a capture encode ratio. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        public fun fromCaptureEncodeRatio(captureEncodeRatio: Rational): Speed {
            require(captureEncodeRatio.denominator > 0 && captureEncodeRatio.numerator > 0)
            return Speed(Rational(captureEncodeRatio.denominator, captureEncodeRatio.numerator))
        }
    }
}
