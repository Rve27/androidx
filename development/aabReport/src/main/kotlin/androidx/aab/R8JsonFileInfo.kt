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

package androidx.aab

import androidx.aab.cli.VERBOSE
import com.android.tools.r8.metadata.R8BuildMetadata
import java.io.InputStream

data class R8JsonFileInfo(
    val dexShas: Set<String>,
    val optimizationEnabled: Boolean,
    val obfuscationEnabled: Boolean,
    val shrinkingEnabled: Boolean,
) {
    companion object {
        const val BUNDLE_LOCATION = "BUNDLE-METADATA/com.android.tools/r8.json"

        @Suppress("UNCHECKED_CAST")
        fun fromJson(src: InputStream): R8JsonFileInfo? {
            val text = src.bufferedReader().readText()
            val metadata = R8BuildMetadata.fromJson(text)
            if (metadata.dexFilesMetadata == null) {
                // assume this isn't properly formed metadata file. For example, have observed
                // json file with just the content `{"version":"8.7.18"}`, from before content was
                // filled
                return null
            }

            return R8JsonFileInfo(
                dexShas = metadata.dexFilesMetadata.map { it.checksum }.toSet(),
                optimizationEnabled = metadata.optionsMetadata.isOptimizationsEnabled,
                obfuscationEnabled = metadata.optionsMetadata.isObfuscationEnabled,
                shrinkingEnabled = metadata.optionsMetadata.isShrinkingEnabled,
            )
        }

        val CSV_TITLES =
            listOf(
                "r8json_metadata",
                "r8json_optimizationEnabled",
                "r8json_obfuscationEnabled",
                "r8json_shrinkingEnabled",
            ) +
                if (VERBOSE) {
                    listOf("r8json_sortedDexChecksumsSha256")
                } else {
                    emptyList()
                }

        fun R8JsonFileInfo?.csvEntries(): List<String> {
            return listOf(
                (this == null).toString(),
                this?.optimizationEnabled.toString(),
                this?.obfuscationEnabled.toString(),
                this?.shrinkingEnabled.toString(),
            ) +
                if (VERBOSE) {
                    listOf(
                        this?.dexShas?.sorted()?.joinToString(separator = INTERNAL_CSV_SEPARATOR)
                            ?: "null"
                    )
                } else {
                    emptyList()
                }
        }
    }
}
