/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo

/**
 * Defines the sampling behavior for a texture.
 *
 * The fields of this sampler are based on the public Filament TextureSampler class but may diverge
 * over time.
 * https://github.com/google/filament/blob/main/android/filament-android/src/main/java/com/google/android/filament/TextureSampler.java
 *
 * @property minFilter an [Int] which describes how neighboring texels are sampled when the rendered
 *   size is smaller than the texture.
 * @property magFilter an [Int] which describes how neighboring texels are sampled when the rendered
 *   size is larger than the texture.
 * @property wrapModeS an [Int] which describes how texture coordinates outside the [0-1] range are
 *   handled.
 * @property wrapModeT an [Int] which describes how texture coordinates outside the [0-1] range are
 *   handled.
 * @property wrapModeR an [Int] which describes how texture coordinates outside the [0-1] range are
 *   handled.
 * @property compareMode an [Int] which describes how depth texture sampling comparisons are
 *   handled.
 * @property compareFunc an [Int] which describes how depth texture sampling comparisons are
 *   evaluated.
 * @property anisotropyLog2 an [Int] which controls the level of anisotropic filtering applied to
 *   the texture. Higher values mean more samples and better quality, at increased GPU cost.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class TextureSampler
internal constructor(
    @MinFilter public val minFilter: Int,
    @MagFilter public val magFilter: Int,
    @WrapMode public val wrapModeS: Int,
    @WrapMode public val wrapModeT: Int,
    @WrapMode public val wrapModeR: Int,
    @CompareMode public val compareMode: Int,
    @CompareFunc public val compareFunc: Int,
    @IntRange(from = 0) public val anisotropyLog2: Int,
) {
    /** Builder for creating a [TextureSampler] instance. */
    public class Builder {
        private var minFilter: Int = MIN_FILTER_LINEAR
        private var magFilter: Int = MAG_FILTER_LINEAR
        private var wrapModeS: Int = WRAP_MODE_REPEAT
        private var wrapModeT: Int = WRAP_MODE_REPEAT
        private var wrapModeR: Int = WRAP_MODE_REPEAT
        private var compareMode: Int = COMPARE_MODE_NONE
        private var compareFunc: Int = COMPARE_FUNC_LESSER_OR_EQUAL
        private var anisotropyLog2: Int = 0

        /** Sets the minification filter. */
        public fun setMinFilter(@MinFilter minFilter: Int): Builder = apply {
            this.minFilter = minFilter
        }

        /** Sets the magnification filter. */
        public fun setMagFilter(@MagFilter magFilter: Int): Builder = apply {
            this.magFilter = magFilter
        }

        /** Sets the wrap mode for the S coordinate. */
        public fun setWrapModeS(@WrapMode wrapModeS: Int): Builder = apply {
            this.wrapModeS = wrapModeS
        }

        /** Sets the wrap mode for the T coordinate. */
        public fun setWrapModeT(@WrapMode wrapModeT: Int): Builder = apply {
            this.wrapModeT = wrapModeT
        }

        /** Sets the wrap mode for the R coordinate. */
        public fun setWrapModeR(@WrapMode wrapModeR: Int): Builder = apply {
            this.wrapModeR = wrapModeR
        }

        /** Sets the compare mode for depth texture comparisons. */
        public fun setCompareMode(@CompareMode compareMode: Int): Builder = apply {
            this.compareMode = compareMode
        }

        /** Sets the compare function for depth texture comparisons. */
        public fun setCompareFunc(@CompareFunc compareFunc: Int): Builder = apply {
            this.compareFunc = compareFunc
        }

        /**
         * Sets the anisotropy level which controls the level of anisotropic filtering applied to
         * the texture, improving the appearance of textures at steep angles. Higher values mean
         * more samples and better quality, but also increased GPU load.
         */
        public fun setAnisotropyLog2(@IntRange(from = 0) anisotropyLog2: Int): Builder = apply {
            this.anisotropyLog2 = anisotropyLog2
        }

        /** Creates a new [TextureSampler] instance with the specified parameters. */
        public fun build(): TextureSampler =
            TextureSampler(
                minFilter = minFilter,
                magFilter = magFilter,
                wrapModeS = wrapModeS,
                wrapModeT = wrapModeT,
                wrapModeR = wrapModeR,
                compareMode = compareMode,
                compareFunc = compareFunc,
                anisotropyLog2 = anisotropyLog2,
            )
    }

    public companion object {
        /** The edge of the texture extends to infinity. */
        public const val WRAP_MODE_CLAMP_TO_EDGE: Int = 0
        /** The texture infinitely repeats in the wrap direction. */
        public const val WRAP_MODE_REPEAT: Int = 1
        /** The texture infinitely repeats and mirrors in the wrap direction. */
        public const val WRAP_MODE_MIRRORED_REPEAT: Int = 2

        /** Defines the constants for texture wrap modes. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(value = [WRAP_MODE_CLAMP_TO_EDGE, WRAP_MODE_REPEAT, WRAP_MODE_MIRRORED_REPEAT])
        internal annotation class WrapMode

        /** No filtering. Nearest neighbor is used. */
        public const val MIN_FILTER_NEAREST: Int = 0
        /** Box filtering. Weighted average of 4 neighbors is used. */
        public const val MIN_FILTER_LINEAR: Int = 1
        /** Mip-mapping is activated, but no filtering occurs. */
        public const val MIN_FILTER_NEAREST_MIPMAP_NEAREST: Int = 2
        /** Box filtering within a mip-map level. */
        public const val MIN_FILTER_LINEAR_MIPMAP_NEAREST: Int = 3
        /** Mip-map levels are interpolated, but no other filtering occurs. */
        public const val MIN_FILTER_NEAREST_MIPMAP_LINEAR: Int = 4
        /** Both interpolated Mip-mapping and linear filtering are used. */
        public const val MIN_FILTER_LINEAR_MIPMAP_LINEAR: Int = 5

        /** Defines the constants for texture minification filters. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            value =
                [
                    MIN_FILTER_NEAREST,
                    MIN_FILTER_LINEAR,
                    MIN_FILTER_NEAREST_MIPMAP_NEAREST,
                    MIN_FILTER_LINEAR_MIPMAP_NEAREST,
                    MIN_FILTER_NEAREST_MIPMAP_LINEAR,
                    MIN_FILTER_LINEAR_MIPMAP_LINEAR,
                ]
        )
        internal annotation class MinFilter

        /** No filtering. Nearest neighbor is used. */
        public const val MAG_FILTER_NEAREST: Int = 0
        /** Box filtering. Weighted average of 4 neighbors is used. */
        public const val MAG_FILTER_LINEAR: Int = 1

        /** Defines the constants for texture magnification filters. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(value = [MAG_FILTER_NEAREST, MAG_FILTER_LINEAR])
        internal annotation class MagFilter

        /** The comparison function is not used. */
        public const val COMPARE_MODE_NONE: Int = 0
        /** The comparison function is used. */
        public const val COMPARE_MODE_COMPARE_TO_TEXTURE: Int = 1

        /** Defines the constants for depth texture comparison modes. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(value = [COMPARE_MODE_NONE, COMPARE_MODE_COMPARE_TO_TEXTURE])
        internal annotation class CompareMode

        /** Passes if the incoming depth is less than or equal to the stored depth. */
        public const val COMPARE_FUNC_LESSER_OR_EQUAL: Int = 0
        /** Passes if the incoming depth is greater than or equal to the stored depth. */
        public const val COMPARE_FUNC_GREATER_OR_EQUAL: Int = 1
        /** Passes if the incoming depth is strictly less than the stored depth. */
        public const val COMPARE_FUNC_LESSER: Int = 2
        /** Passes if the incoming depth is strictly greater than the stored depth. */
        public const val COMPARE_FUNC_GREATER: Int = 3
        /** Passes if the incoming depth is equal to the stored depth. */
        public const val COMPARE_FUNC_EQUAL: Int = 4
        /** Passes if the incoming depth is not equal to the stored depth. */
        public const val COMPARE_FUNC_NOT_EQUAL: Int = 5
        /** Always passes. Depth testing is effectively deactivated. */
        public const val COMPARE_FUNC_ALWAYS: Int = 6
        /** Never passes. The depth test always fails. */
        public const val COMPARE_FUNC_NEVER: Int = 7

        /** Defines the constants for depth texture comparison functions. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            value =
                [
                    COMPARE_FUNC_LESSER_OR_EQUAL,
                    COMPARE_FUNC_GREATER_OR_EQUAL,
                    COMPARE_FUNC_LESSER,
                    COMPARE_FUNC_GREATER,
                    COMPARE_FUNC_EQUAL,
                    COMPARE_FUNC_NOT_EQUAL,
                    COMPARE_FUNC_ALWAYS,
                    COMPARE_FUNC_NEVER,
                ]
        )
        internal annotation class CompareFunc
    }
}
