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

package androidx.text.vertical

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * A span that applies a shear (skew) transformation to the vertical writing text.
 *
 * This span inherits from [MetricAffectingSpan], but it is specifically designed for use within a
 * [VerticalTextLayout] and will not have an effect in other contexts.
 *
 * It is used to achieve an italic-like effect for vertical text layout, where traditional italic
 * fonts may not render correctly.
 *
 * The shear value represents the horizontal skew factor for rotated or tate-chu-yoko text. For
 * upright text, this value is used as the vertical skew factor.
 *
 * See [DEFAULT_FONT_SHEAR] for the default value.
 *
 * Note: This span only works with VerticalTextLayout.
 *
 * @property fontShear The shear factor to apply to the text.
 */
public class FontShearSpan(public val fontShear: Float = DEFAULT_FONT_SHEAR) :
    MetricAffectingSpan() {
    override fun updateMeasureState(p0: TextPaint) {}

    override fun updateDrawState(p0: TextPaint?) {}

    public companion object {
        /**
         * Default constant for fontShear.
         *
         * This value represents a shear angle of approximately 9.53 degrees (atan(0.1678842)).
         *
         * Angles in the range of 8 to 12 degrees are widely considered to provide a visually
         * balanced emphasis while maintaining readability, aligning with the general slope observed
         * in many Roman italic and oblique typefaces.
         *
         * This specific value is adopted from the W3C Timed Text Markup Language 2 (TTML2)
         * specification for the
         * [tts:fontShear](https://www.w3.org/TR/2018/REC-ttml2-20181108/#style-attribute-fontShear)
         * attribute which aims to standardize emphasis for such scripts.
         */
        public const val DEFAULT_FONT_SHEAR: Float = 0.1678842f
    }
}
