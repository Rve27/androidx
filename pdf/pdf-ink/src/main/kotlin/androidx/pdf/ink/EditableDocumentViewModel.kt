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

import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.pdf.annotation.EditablePdfDocument
import androidx.pdf.annotation.draftstate.AnnotationEditsDraftState
import androidx.pdf.annotation.draftstate.MemoryAnnotationEditsDraftState
import androidx.pdf.annotation.models.AnnotationsDisplayState
import androidx.pdf.annotation.models.PdfAnnotation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class EditableDocumentViewModel(private val state: SavedStateHandle) : ViewModel() {
    private lateinit var annotationEditsDraftState: AnnotationEditsDraftState

    private val _annotationDisplayStateFlow = MutableStateFlow(AnnotationsDisplayState.EMPTY)

    internal val annotationsDisplayStateFlow: StateFlow<AnnotationsDisplayState> =
        _annotationDisplayStateFlow.asStateFlow()

    internal val isEditModeEnabledFlow: StateFlow<Boolean> =
        state.getStateFlow(EDIT_MODE_ENABLED_KEY, false)

    internal var isEditModeEnabled: Boolean
        get() = state[EDIT_MODE_ENABLED_KEY] ?: false
        set(value) {
            state[EDIT_MODE_ENABLED_KEY] = value
        }

    internal var editablePdfDocument: EditablePdfDocument? = null
        set(value) {
            field = value

            if (value != null) {
                maybeInitialiseForDocument(value)
            }
        }

    @VisibleForTesting
    private fun maybeInitialiseForDocument(document: EditablePdfDocument) {
        val documentUri = document.uri

        // If the document has changed, reset the edit states
        if (documentUri != state.get<Uri>(DOCUMENT_URI_KEY)) {
            state[EDIT_MODE_ENABLED_KEY] = false
            state[DOCUMENT_URI_KEY] = documentUri
            editablePdfDocument = document

            annotationEditsDraftState = MemoryAnnotationEditsDraftState()
            _annotationDisplayStateFlow.value =
                AnnotationsDisplayState(
                    draftState = annotationEditsDraftState.toImmutableDraftState(),
                    transformationMatrices = HashMap(),
                )
        }
    }

    /** Adds a [PdfAnnotation] to the draft state. */
    fun addAnnotations(annotation: PdfAnnotation) {
        if (editablePdfDocument != null) {
            annotationEditsDraftState.addEdit(annotation)
            _annotationDisplayStateFlow.update { currentState ->
                currentState.copy(draftState = annotationEditsDraftState.toImmutableDraftState())
            }
        }
    }

    /** Updates the transformation matrices for rendering annotations. */
    fun updateTransformationMatrices(transformationMatrices: Map<Int, Matrix>) {
        if (editablePdfDocument != null) {
            _annotationDisplayStateFlow.update { displayState ->
                displayState.copy(transformationMatrices = transformationMatrices)
            }
        }
    }

    internal companion object {
        const val DOCUMENT_URI_KEY = "documentUri"
        private const val EDIT_MODE_ENABLED_KEY = "isEditModeEnabled"
    }
}
