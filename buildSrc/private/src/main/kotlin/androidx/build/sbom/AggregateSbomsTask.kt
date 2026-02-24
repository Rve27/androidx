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

package androidx.build.sbom

import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.spdx.jacksonstore.MultiFormatStore
import org.spdx.jacksonstore.MultiFormatStore.Format
import org.spdx.library.ModelCopyManager
import org.spdx.library.SpdxConstants
import org.spdx.library.model.SpdxDocument
import org.spdx.library.model.SpdxElement
import org.spdx.library.model.SpdxModelFactory
import org.spdx.library.model.TypedValue
import org.spdx.library.model.license.ListedLicenses
import org.spdx.storage.simple.InMemSpdxStore

/** Aggregates all project SBOMs into a single SBOM. */
@CacheableTask
abstract class AggregateSbomsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sbomFiles: ConfigurableFileCollection

    @get:OutputFile abstract val aggregateSbomFile: RegularFileProperty

    @TaskAction
    fun merge() {
        val targetStore = MultiFormatStore(InMemSpdxStore(), Format.JSON_PRETTY)
        val documentUri = "https://spdx.google.com/android/androidx/merged/${UUID.randomUUID()}"
        val copyManager = ModelCopyManager()
        val targetDoc = SpdxModelFactory.createSpdxDocument(targetStore, documentUri, copyManager)

        targetDoc.setName("Aggregated SBOM")
        targetDoc.setSpecVersion("SPDX-2.3")
        targetDoc.dataLicense = ListedLicenses.getListedLicenses().getListedLicenseById("CC0-1.0")

        val distinctCreators = mutableSetOf<String>()
        var creationTimestamp = ""

        sbomFiles.files.forEach { file ->
            try {
                val inputStore = MultiFormatStore(InMemSpdxStore(), Format.JSON_PRETTY)
                inputStore.deSerialize(file.inputStream(), false)
                val inputUri = inputStore.documentUris.firstOrNull() ?: return@forEach

                val docTv =
                    inputStore
                        .getAllItems(inputUri, SpdxConstants.CLASS_SPDX_DOCUMENT)
                        .findFirst()
                        .orElse(null) ?: return@forEach

                val inputDoc =
                    SpdxModelFactory.getModelObject(
                        inputStore,
                        inputUri,
                        docTv.id,
                        SpdxConstants.CLASS_SPDX_DOCUMENT,
                        copyManager,
                        false,
                    ) as SpdxDocument

                inputDoc.creationInfo?.let { info ->
                    if (creationTimestamp.isEmpty()) creationTimestamp = info.created
                    distinctCreators.addAll(info.creators)
                }

                inputDoc.documentDescribes.forEach { element ->
                    if (element is SpdxElement) {
                        val tv =
                            copyManager.copy(
                                targetStore,
                                documentUri,
                                inputStore,
                                inputUri,
                                element.id,
                                element.type,
                            ) as TypedValue

                        val copiedElement =
                            SpdxModelFactory.getModelObject(
                                targetStore,
                                documentUri,
                                tv.id,
                                tv.type,
                                copyManager,
                                false,
                            ) as SpdxElement

                        targetDoc.documentDescribes.add(copiedElement)
                    }
                }
            } catch (e: Exception) {
                throw GradleException("Failed to process SBOM file: ${file.path}", e)
            }
        }

        targetDoc.creationInfo =
            targetDoc.createCreationInfo(
                distinctCreators.ifEmpty { listOf("Tool: AndroidXBuild") }.toList(),
                creationTimestamp.ifEmpty { Instant.now().toString() },
            )

        val outputFile = aggregateSbomFile.get().asFile
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { targetStore.serialize(documentUri, it) }
    }

    companion object {
        const val AGGREGATE_SBOMS_TASK_NAME = "aggregateSboms"
    }
}
