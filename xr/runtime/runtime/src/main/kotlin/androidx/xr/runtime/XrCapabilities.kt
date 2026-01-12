/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** A device capability that determines how virtual content is added to the real world. */
public class DisplayBlendMode private constructor(private val value: Int) {

    public companion object {
        /** Blending is not supported. */
        @JvmField public val NO_DISPLAY: DisplayBlendMode = DisplayBlendMode(0)
        /**
         * Virtual content is added to the real world by adding the pixel values for each of Red,
         * Green, and Blue components. Alpha is ignored. Black pixels will appear transparent.
         */
        @JvmField public val ADDITIVE: DisplayBlendMode = DisplayBlendMode(1)
        /**
         * Virtual content is added to the real world by alpha blending the pixel values based on
         * the Alpha component.
         */
        @JvmField public val ALPHA_BLEND: DisplayBlendMode = DisplayBlendMode(2)
    }
}

/** Feature that allows tracking of and provides information about scene planes. */
@SuppressWarnings("HiddenSuperclass")
public class PlaneTrackingMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode {
    public companion object {
        /** Planes will not be tracked. */
        @JvmField public val DISABLED: PlaneTrackingMode = PlaneTrackingMode(0)
        /**
         * Horizontal and vertical planes will be tracked. Note that setting this mode will consume
         * additional runtime resources.
         *
         * Supported runtimes:
         * - OpenXR
         * - Play Services
         *
         * Required permissions:
         * - [SCENE_UNDERSTANDING_COARSE][androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE]
         *   (OpenXR runtimes only)
         * - [ACCESS_COARSE_LOCATION][android.Manifest.permission.ACCESS_COARSE_LOCATION] (Play
         *   Services runtimes only)
         * - [CAMERA][android.Manifest.permission.CAMERA] (Play Services runtimes only)
         */
        @JvmField public val HORIZONTAL_AND_VERTICAL: PlaneTrackingMode = PlaneTrackingMode(1)
    }
}

/** Feature that allows tracking of the user's hands and hand joints. */
@SuppressWarnings("HiddenSuperclass")
public class HandTrackingMode
private constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val mode: Int) :
    Config.ConfigMode {
    public companion object {
        /** Hands will not be tracked. */
        @JvmField public val DISABLED: HandTrackingMode = HandTrackingMode(0)
        /**
         * Both the left and right hands will be tracked. Note that setting this mode will consume
         * additional runtime resources.
         *
         * Supported runtimes:
         * - OpenXR
         *
         * Required permissions:
         * - [HAND_TRACKING][androidx.xr.runtime.manifest.HAND_TRACKING]
         */
        @JvmField public val BOTH: HandTrackingMode = HandTrackingMode(1)
    }
}
