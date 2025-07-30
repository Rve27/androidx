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

package androidx.pdf.utils

import android.graphics.RectF
import android.os.ParcelFileDescriptor
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.reflect.TypeToken
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException

/**
 * Writes a list of [PdfAnnotation] objects to a [ParcelFileDescriptor].
 *
 * @param pfd The [ParcelFileDescriptor] to write to.
 * @param annotations The list of [PdfAnnotation] objects to write.
 * @throws IOException If there is an error writing to the file.
 */
internal fun writeAnnotationsToFile(pfd: ParcelFileDescriptor, annotations: List<PdfAnnotation>) {
    val fileDescriptor: FileDescriptor = pfd.fileDescriptor
    // It is the responsibility of the caller to close this pfd.
    FileOutputStream(fileDescriptor).use { outputStream ->
        val gson = Gson()

        // Serialize the list of annotations to a json string
        val jsonString = gson.toJson(annotations)
        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    // To reuse the same PFD for reading after writing, we need to reset its file pointer
    pfd.resetToStartingPosition()
}

/**
 * Reads a list of [PdfAnnotation] objects from a [ParcelFileDescriptor].
 *
 * @param pfd The [ParcelFileDescriptor] to read from.
 * @return A list of [PdfAnnotation] objects read from the file.
 */
internal fun readAnnotationsFromPfd(pfd: ParcelFileDescriptor): List<PdfAnnotation> {
    // TODO: b/434864732 - Use stream to read annotations from file
    val jsonString = readFromPfd(pfd)
    val type = object : TypeToken<List<PdfAnnotation>>() {}.type
    val gson =
        GsonBuilder()
            .registerTypeAdapter(PdfAnnotation::class.java, getStampAnnotationDeserializer())
            .create()
    val annotations = gson.fromJson<List<PdfAnnotation>>(jsonString, type)
    return annotations ?: listOf()
}

/** Returns a [JsonDeserializer] for [StampAnnotation]. */
internal fun getStampAnnotationDeserializer(): JsonDeserializer<StampAnnotation> {
    return JsonDeserializer { json, _, context ->
        val jsonObject = json.asJsonObject

        val pageNum = jsonObject.get("pageNum").asInt
        val bounds = context.deserialize<RectF>(jsonObject.get("bounds"), RectF::class.java)

        val pdfObjectsArray = jsonObject.getAsJsonArray("pdfObjects")
        val pathPdfObjects = mutableListOf<PathPdfObject>()
        for (pdfObjectElement in pdfObjectsArray) {
            pathPdfObjects.add(context.deserialize(pdfObjectElement, PathPdfObject::class.java))
        }
        StampAnnotation(pageNum, bounds, pathPdfObjects)
    }
}
