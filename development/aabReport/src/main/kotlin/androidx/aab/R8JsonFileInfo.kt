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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        fun fromJson(src: InputStream): R8JsonFileInfo {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val metadata = gson.fromJson<Map<String, Any>>(src.bufferedReader().readText(), mapType)

            val options = (metadata["options"] as Map<String, Any>?)!!

            return R8JsonFileInfo(
                dexShas =
                    (metadata["dexFiles"] as List<Map<String, Any>>)
                        .map { it["checksum"] as String }
                        .toSet(),
                optimizationEnabled = options["isObfuscationEnabled"] as Boolean,
                obfuscationEnabled = options["isObfuscationEnabled"] as Boolean,
                shrinkingEnabled = options["isShrinkingEnabled"] as Boolean,
            )
        }

        val CSV_TITLES =
            listOf(
                "r8json_metadata",
                "r8json_sortedDexChecksumsSha256",
                "r8json_optimizationEnabled",
                "r8json_obfuscationEnabled",
                "r8json_shrinkingEnabled",
            )

        fun R8JsonFileInfo?.csvEntries(): List<String> {
            return listOf(
                if (this == null) "false" else "true",
                this?.dexShas?.sorted()?.joinToString(separator = INTERNAL_CSV_SEPARATOR) ?: "null",
                this?.optimizationEnabled.toString(),
                this?.obfuscationEnabled.toString(),
                this?.shrinkingEnabled.toString(),
            )
        }
    }
}
