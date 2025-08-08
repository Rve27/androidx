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

package androidx.pdf.annotation.converters

import android.graphics.pdf.component.PdfAnnotation as AospPdfAnnotation
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.Converter
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation

/**
 * Responsible for creating [Converter] instances that can transform specific subtypes of
 * [PdfAnnotation] into their corresponding AOSP framework [AospPdfAnnotation] representations
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal object PdfAnnotationConvertersFactory {
    private val stampAnnotationConverter = StampAnnotationConverter()

    /**
     * Creates and returns a [Converter] for the given [PdfAnnotation].
     *
     * @param F The specific subtype of [PdfAnnotation] for which to create a converter.
     * @param annot The [PdfAnnotation] instance for which a converter is needed.
     * @return A [Converter] capable of converting the input [annot] to an [AospPdfAnnotation].
     * @throws UnsupportedOperationException if a converter for the provided [annot] type is not
     *   supported.
     */
    @Suppress("UNCHECKED_CAST")
    fun <F : PdfAnnotation> create(annot: PdfAnnotation): Converter<F, AospPdfAnnotation> {
        val value =
            when (annot) {
                is StampAnnotation -> stampAnnotationConverter
                else ->
                    throw UnsupportedOperationException(
                        "PdfAnnotation :: ${annot.javaClass.simpleName} is not supported!"
                    )
            }
        return value as Converter<F, AospPdfAnnotation>
    }
}
