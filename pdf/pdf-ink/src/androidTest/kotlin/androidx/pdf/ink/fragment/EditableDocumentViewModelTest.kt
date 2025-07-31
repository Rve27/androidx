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

package androidx.pdf.ink.fragment

import android.graphics.RectF
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.ink.EditableDocumentViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class EditableDocumentViewModelTest {

    private lateinit var annotationsViewModel: EditableDocumentViewModel

    @Before
    fun setup() {
        annotationsViewModel = EditableDocumentViewModel()
    }

    @Test
    fun addAnnotations_updatesDraftState_forSingleAnnotation() = runTest {
        val annotation = createAnnotation(pageNum = 0)

        annotationsViewModel.addAnnotations(annotation)
        val firstPageEdits: Map<EditId, PdfAnnotationData>? =
            annotationsViewModel.annotationDraftStateFlow.value.edits[0]

        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(1)

        val addedAnnotationData = firstPageEdits!!.values.first()
        assertThat(addedAnnotationData.annotation).isEqualTo(annotation)
    }

    @Test
    fun addAnnotations_updatesDraftState_forMultipleAnnotationsOnSamePage() = runTest {
        val annotation1 = createAnnotation(pageNum = 0, bounds = RectF(0f, 0f, 50f, 50f))
        val annotation2 = createAnnotation(pageNum = 0, bounds = RectF(50f, 50f, 100f, 100f))

        annotationsViewModel.addAnnotations(annotation1)
        annotationsViewModel.addAnnotations(annotation2)

        val firstPageEdits: Map<EditId, PdfAnnotationData>? =
            annotationsViewModel.annotationDraftStateFlow.value.edits[0]
        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(2)

        val annotationsInState = firstPageEdits!!.values.map { it.annotation }
        assertThat(annotationsInState).containsExactly(annotation1, annotation2)
    }

    @Test
    fun addAnnotations_updatesDraftState_forMultipleAnnotationsOnDifferentPages() = runTest {
        val annotationPage0 = createAnnotation(pageNum = 0)
        val annotationPage1 = createAnnotation(pageNum = 1)

        annotationsViewModel.addAnnotations(annotationPage0)
        annotationsViewModel.addAnnotations(annotationPage1)

        val firstPageEdits: Map<EditId, PdfAnnotationData>? =
            annotationsViewModel.annotationDraftStateFlow.value.edits[0]
        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(1)
        assertThat(firstPageEdits!!.values.first().annotation).isEqualTo(annotationPage0)

        val secondPageEdits: Map<EditId, PdfAnnotationData>? =
            annotationsViewModel.annotationDraftStateFlow.value.edits[1]
        assertThat(secondPageEdits).isNotNull()
        assertThat(secondPageEdits).hasSize(1)
        assertThat(secondPageEdits!!.values.first().annotation).isEqualTo(annotationPage1)
    }

    fun createAnnotation(
        pageNum: Int = 0,
        bounds: RectF = RectF(0f, 0f, 100f, 100f),
    ): PdfAnnotation {
        return StampAnnotation(
            pageNum = pageNum,
            bounds = bounds,
            pdfObjects =
                listOf(PathPdfObject(brushColor = 0, brushWidth = 0f, inputs = emptyList())),
        )
    }
}
