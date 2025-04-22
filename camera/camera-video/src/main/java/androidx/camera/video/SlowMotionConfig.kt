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

import android.util.Range
import androidx.annotation.RestrictTo
import androidx.camera.video.Speed.Companion.fromCaptureEncodeRates

/**
 * Configuration for slow-motion video recording.
 *
 * This class holds the necessary parameters to configure slow-motion video capture, including the
 * desired playback speed, the range of capture frame rates supported, and the encoding frame rate.
 *
 * @param captureFrameRateRange The capture frame rate range supported for this slow-motion
 *   configuration. This [Range] of integers indicates the minimum and maximum frames per second the
 *   camera will capture to achieve the desired slow-motion effect.
 * @param encodeFrameRate The frame rate at which the slow-motion video will be encoded into the
 *   output file. This frame rate, combined with the [captureFrameRateRange], determines the degree
 *   of slow motion achieved during playback. For example, capturing at a high frame rate (within
 *   [captureFrameRateRange]) and encoding at a lower [encodeFrameRate] will result in a slow-motion
 *   effect when played back at the [encodeFrameRate].
 * @param speed The desired playback speed for the slow-motion video. This is represented by a
 *   [Speed] object (e.g., [Speed.SPEED_1_4X] for quarter-speed playback).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) // TODO(b/404096374): High-speed public API
public class SlowMotionConfig
@RestrictTo(RestrictTo.Scope.LIBRARY)
public constructor(
    public val captureFrameRateRange: Range<Int>,
    public val encodeFrameRate: Int,
    public val speed: Speed = fromCaptureEncodeRates(captureFrameRateRange.upper, encodeFrameRate)
) {
    override fun toString(): String {
        return "SlowMotionConfig(" +
            "captureFrameRateRange=$captureFrameRateRange" +
            ", encodeFrameRate=$encodeFrameRate" +
            ", speed=$speed" +
            ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlowMotionConfig

        if (captureFrameRateRange != other.captureFrameRateRange) return false
        if (encodeFrameRate != other.encodeFrameRate) return false
        if (speed != other.speed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = captureFrameRateRange.hashCode()
        result = 31 * result + encodeFrameRate
        result = 31 * result + speed.hashCode()
        return result
    }
}
