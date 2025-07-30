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

import androidx.aab.AppMetadataPropsInfo.Companion.csvEntries
import androidx.aab.DexInfo.Companion.csvEntries
import androidx.aab.MappingFileInfo.Companion.csvEntries
import androidx.aab.ProfileInfo.Companion.csvEntries
import androidx.aab.R8JsonFileInfo.Companion.csvEntries
import androidx.aab.cli.VERBOSE
import com.android.tools.build.libraries.metadata.AppDependencies
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/** Separator for CSV output within entries (such as multiple dex SHAs in one column) */
const val INTERNAL_CSV_SEPARATOR = "--"

data class BundleInfo(
    val path: String,
    val profileInfo: ProfileInfo?,
    val dexInfo: List<DexInfo>,
    val mappingFileInfo: MappingFileInfo?,
    val r8JsonFileInfo: R8JsonFileInfo?,
    val dotVersionFiles: Map<String, String>, // map maven coordinates -> version number
    val appBundleDependencies: AppDependencies?,
    val appMetadataPropsInfoMetaInf: AppMetadataPropsInfo?,
    val appMetadataPropsInfoBundleMetadata: AppMetadataPropsInfo?,
) {
    fun toCsvLine(): String {
        return (listOf(path) +
                profileInfo.csvEntries() +
                dexInfo.csvEntries() +
                mappingFileInfo.csvEntries() +
                r8JsonFileInfo.csvEntries() +
                appMetadataPropsInfoBundleMetadata.csvEntries() +
                appMetadataPropsInfoMetaInf.csvEntries())
            .joinToString(separator = ", ")
    }

    companion object {
        val CSV_HEADER =
            (listOf("path") +
                    ProfileInfo.CSV_TITLES +
                    DexInfo.CSV_TITLES +
                    MappingFileInfo.CSV_TITLES +
                    R8JsonFileInfo.CSV_TITLES +
                    AppMetadataPropsInfo.CSV_TITLES_BUNDLE +
                    AppMetadataPropsInfo.CSV_TITLES_META_INF)
                .joinToString(", ")

        // TODO: Move to wrapper object
        const val DEPENDENCIES_PB_LOCATION =
            "BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb"

        fun from(file: File): BundleInfo {
            return FileInputStream(file).use { from(file.path, it) }
        }

        fun from(path: String, inputStream: InputStream): BundleInfo {
            val dexInfo = mutableListOf<DexInfo>()
            val dotVersionFiles = mutableMapOf<String, String>()
            var mappingFileInfo: MappingFileInfo? = null
            var r8MetadataFileInfo: R8JsonFileInfo? = null
            var appDependencies: AppDependencies? = null
            var profileInfo: ProfileInfo? = null
            var appMetadataPropsInfoMetaInf: AppMetadataPropsInfo? = null
            var appMetadataPropsInfoBundleMetadata: AppMetadataPropsInfo? = null
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                while (entry != null) {
                    if (VERBOSE && !entry.name.contains("/res/")) {
                        println(entry.name) // just for debugging
                    }
                    when {
                        entry.name.contains("/dex/classes") && entry.name.endsWith(".dex") -> {
                            dexInfo.add(DexInfo.from(entry.name, zis))
                        }

                        entry.name == ProfileInfo.BUNDLE_LOCATION -> {
                            profileInfo = ProfileInfo.readFromProfile(zis)
                        }

                        entry.name.endsWith(".version") && entry.name.contains("/META-INF/") -> {
                            dotVersionFiles[entry.name] = zis.bufferedReader().readText().trim()
                        }

                        entry.name == R8JsonFileInfo.BUNDLE_LOCATION -> {
                            r8MetadataFileInfo = R8JsonFileInfo.fromJson(zis)
                        }

                        entry.name == DEPENDENCIES_PB_LOCATION -> {
                            appDependencies = AppDependencies.ADAPTER.decode(zis)
                        }

                        entry.name == MappingFileInfo.BUNDLE_LOCATION -> {
                            mappingFileInfo = MappingFileInfo()
                        }

                        entry.name == AppMetadataPropsInfo.BUNDLE_LOCATION_METADATA -> {
                            appMetadataPropsInfoBundleMetadata = AppMetadataPropsInfo.from(zis)
                        }

                        entry.name == AppMetadataPropsInfo.BUNDLE_LOCATION_META_INF -> {
                            appMetadataPropsInfoMetaInf = AppMetadataPropsInfo.from(zis)
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            if (VERBOSE) {
                appDependencies?.run {
                    // print all contained libraries
                    library.forEach {
                        it.maven_library?.run {
                            println("LIB: ${groupId}:${artifactId}:${version}")
                        }
                    }
                }
            }

            return BundleInfo(
                path = path,
                profileInfo = profileInfo,
                dexInfo = dexInfo,
                mappingFileInfo = mappingFileInfo,
                r8JsonFileInfo = r8MetadataFileInfo,
                dotVersionFiles = dotVersionFiles,
                appBundleDependencies = appDependencies,
                appMetadataPropsInfoMetaInf = appMetadataPropsInfoMetaInf,
                appMetadataPropsInfoBundleMetadata = appMetadataPropsInfoBundleMetadata,
            )
        }
    }
}
