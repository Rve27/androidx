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

package androidx.pdf.ink

import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.pdf.annotation.draftstate.ImmutableAnnotationEditsDraftState
import androidx.pdf.annotation.draftstate.SimpleAnnotationEditsDraftState
import androidx.pdf.annotation.models.PdfAnnotation
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class EditableDocumentViewModel() : ViewModel() {

    private val draftPfd = createDraftPfd()
    private val annotationEditDraft = SimpleAnnotationEditsDraftState(draftPfd)
    private val _annotationDraftStateFlow =
        MutableStateFlow(ImmutableAnnotationEditsDraftState(emptyMap()))

    /** Stream of annotation edit draft states. */
    val annotationDraftStateFlow: StateFlow<ImmutableAnnotationEditsDraftState>
        get() = _annotationDraftStateFlow.asStateFlow()

    /** Adds a [PdfAnnotation] to the draft state. */
    fun addAnnotations(annotation: PdfAnnotation) {
        annotationEditDraft.addEdit(annotation)
        _annotationDraftStateFlow.update { annotationEditDraft.toImmutableDraftState() }
    }

    override fun onCleared() {
        super.onCleared()
        draftPfd.close()
    }

    private companion object {
        fun createDraftPfd(): ParcelFileDescriptor {
            val tempFile = Files.createTempFile("PDF_ANNOTATIONS_DRAFT", ".txt").toFile()
            return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_WRITE)
        }
    }
}
