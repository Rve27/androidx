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

package androidx.pdf

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

@SuppressLint("BanParcelableUsage")
/**
 * The set of parameters that will be used to render the page of a PDF Document. It can be tuned to
 * set the render mode, render flags and render form content mode.
 *
 * Note: [renderFlags] defaults to [FLAG_RENDER_NONE] which means that no annotations will be
 * rendered on the bitmap. [renderFormContentMode] defaults to [RENDER_FORM_CONTENT_ENABLED] which
 * means that PDF form widgets (if present) will be rendered on the bitmap.
 *
 * @see RenderMode
 * @see RenderFlags
 * @see RenderFormContentMode
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RenderParams(
    @RenderMode public val renderMode: Int,
    @RenderFlags public val renderFlags: Int = FLAG_RENDER_NONE,
    @RenderFormContentMode public val renderFormContentMode: Int = RENDER_FORM_CONTENT_ENABLED,
) : Parcelable {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RENDER_MODE_FOR_DISPLAY, RENDER_MODE_FOR_PRINT)
    public annotation class RenderMode

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        flag = true,
        value =
            [
                FLAG_RENDER_NONE,
                FLAG_RENDER_TEXT_ANNOTATIONS,
                FLAG_RENDER_HIGHLIGHT_ANNOTATIONS,
                FLAG_RENDER_STAMP_ANNOTATIONS,
                FLAG_RENDER_FREETEXT,
            ],
    )
    public annotation class RenderFlags

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RENDER_FORM_CONTENT_ENABLED, RENDER_FORM_CONTENT_DISABLED)
    public annotation class RenderFormContentMode

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(renderMode)
        dest.writeInt(renderFlags)
        dest.writeInt(renderFormContentMode)
    }

    public companion object {
        /** Mode to render the content for display on a screen. */
        public const val RENDER_MODE_FOR_DISPLAY: Int = 1
        /** Mode to render the content for printing. */
        public const val RENDER_MODE_FOR_PRINT: Int = 2

        /** Render flags for annotations */
        public const val FLAG_RENDER_NONE: Int = 0
        /** Flag to enable rendering of text annotation on the page. */
        public const val FLAG_RENDER_TEXT_ANNOTATIONS: Int = 1 shl 1
        /** Flag to enable rendering of highlight annotation on the page. */
        public const val FLAG_RENDER_HIGHLIGHT_ANNOTATIONS: Int = 1 shl 2
        /** Flag to enable rendering of stamp annotation on the page. */
        public const val FLAG_RENDER_STAMP_ANNOTATIONS: Int = 1 shl 3
        /** Flag to enable rendering of freetext annotation on the page. */
        public const val FLAG_RENDER_FREETEXT: Int = 1 shl 4

        /** Mode to include PDF form content in rendered PDF bitmaps. */
        public const val RENDER_FORM_CONTENT_ENABLED: Int = 1
        /** Mode to exclude PDF form content in rendered PDF bitmaps. */
        public const val RENDER_FORM_CONTENT_DISABLED: Int = 2

        @JvmField
        public val CREATOR: Parcelable.Creator<RenderParams> =
            object : Parcelable.Creator<RenderParams> {
                override fun createFromParcel(parcel: Parcel): RenderParams {
                    return RenderParams(parcel.readInt(), parcel.readInt(), parcel.readInt())
                }

                override fun newArray(size: Int): Array<RenderParams?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
