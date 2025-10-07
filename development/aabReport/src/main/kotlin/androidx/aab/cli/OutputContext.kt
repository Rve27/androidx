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

package androidx.aab.cli

import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class OutputContext(outputPath: String?, val verbose: Boolean, csv: Boolean) {

    val outputDir =
        if (outputPath != null) {
            Path(outputPath)
                .also {
                    println("Outputting to: $outputPath")
                    it.createDirectories()
                }
                .toFile()
        } else null

    val csvFile: File? =
        if (csv) {
            if (outputDir == null) {
                println("--csv requires passing --out=<path_to_output_dir>")
                usageAndDie()
            }
            File(outputDir.toString(), "aabReport.csv").also { it.createNewFile() }
        } else null

    fun outputDirForApp(name: String): File? {
        if (outputDir == null) {
            println("no output dir!!!")
            return null
        }

        return File(outputDir, name).also { Files.createDirectory(it.toPath()) }
    }
}
