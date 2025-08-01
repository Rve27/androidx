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

import android.content.Context
import android.os.Build
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.StampAnnotation
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.gson.JsonSyntaxException
import java.io.FileOutputStream
import java.io.IOException
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM, codeName = "VanillaIceCream")
class AnnotationUtilsTest {

    @Test
    fun writingAnnotationToFile_writeAndReadFromSamePfd() = runTest {
        // Define the expected annotationsData to be written.
        val expectedAnnotations =
            listOf(
                PdfAnnotationData(EditId(pageNum = 0, value = "0"), getSampleStampAnnotation(0)),
                PdfAnnotationData(EditId(pageNum = 1, value = "1"), getSampleStampAnnotation(1)),
            )

        // Get the application context.
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create a ParcelFileDescriptor for the test PDF document in read-write mode.
        val pfd = createPfd(context, TEST_ANNOTATIONS_FILE, "rwt")

        // Write annotations and then seek to the beginning of the file to read
        writeAnnotationsToFile(pfd, expectedAnnotations)

        // Read annotations from the same PFD
        val actualAnnotations = readAnnotationsFromPfd(pfd)

        pfd.close()
        assertNotNull(actualAnnotations)
        assertEquals(actualAnnotations.size, 2)
        for (i in 0 until expectedAnnotations.size) {
            assert(actualAnnotations[i].annotation is StampAnnotation)
            assertStampAnnotationEquals(
                expectedAnnotations[i].annotation as StampAnnotation,
                actualAnnotations[i].annotation as StampAnnotation,
            )
        }
    }

    @Test
    fun writingAnnotationToFile_writeAndReadMultipleTimesFromSamePfd() = runTest {
        val expectedAnnotations =
            listOf(
                PdfAnnotationData(EditId(pageNum = 0, value = "0"), getSampleStampAnnotation(0)),
                PdfAnnotationData(EditId(pageNum = 1, value = "1"), getSampleStampAnnotation(1)),
            )

        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create a ParcelFileDescriptor for the test PDF document in read-write mode.
        val pfd = createPfd(context, TEST_ANNOTATIONS_FILE, "rwt")

        // Write annotations and then seek to the beginning of the file to read
        writeAnnotationsToFile(pfd, expectedAnnotations)

        // Read annotations from the same PFD
        var actualAnnotations = readAnnotationsFromPfd(pfd)

        assertNotNull(actualAnnotations)
        assertEquals(actualAnnotations.size, 2)
        for (i in 0 until expectedAnnotations.size) {
            assert(actualAnnotations[i].annotation is StampAnnotation)
            assertStampAnnotationEquals(
                expectedAnnotations[i].annotation as StampAnnotation,
                actualAnnotations[i].annotation as StampAnnotation,
            )
        }

        // Again Read annotations from the same PFD to verify pfd is still open
        actualAnnotations = readAnnotationsFromPfd(pfd)

        pfd.close()
        assertNotNull(actualAnnotations)
        assertEquals(actualAnnotations.size, 2)
        for (i in 0 until expectedAnnotations.size) {
            assert(actualAnnotations[i].annotation is StampAnnotation)
            assertStampAnnotationEquals(
                expectedAnnotations[i].annotation as StampAnnotation,
                actualAnnotations[i].annotation as StampAnnotation,
            )
        }
    }

    @Test
    fun readAnnotationsFromPfd_emptyFile() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pfd = createPfd(context, TEST_ANNOTATIONS_FILE, "rwt")

        val annotations = readAnnotationsFromPfd(pfd)
        assertEquals(0, annotations.size)
        pfd.close()
    }

    @Test
    fun readAnnotationsFromPfd_onlyWriteAccess_throwsJsonSyntaxException() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pfd = createPfd(context, TEST_ANNOTATIONS_FILE, "wt")

        val outputStream = FileOutputStream(pfd.fileDescriptor)

        val jsonString = "{}"
        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        outputStream.flush()
        assertThrows(
            IOException::class.java,
            {
                val annotations = readAnnotationsFromPfd(pfd)
            },
        )

        pfd.close()
    }

    @Test
    fun readAnnotationsFromPfd_malformedJson_throwsJsonSyntaxException1() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pfd = createPfd(context, TEST_ANNOTATIONS_FILE, "rw")

        val jsonString = "}{}"
        val fileOutputStream = FileOutputStream(pfd.fileDescriptor)
        fileOutputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        fileOutputStream.close()

        pfd.resetToStartingPosition()

        assertThrows(
            JsonSyntaxException::class.java,
            {
                val annotations = readAnnotationsFromPfd(pfd)
            },
        )

        pfd.close()
    }

    private val TEST_ANNOTATIONS_FILE = "annotationsTest.json"
}
