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

package androidx.bundle

import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

const val VERBOSE = false

@OptIn(ExperimentalCoroutinesApi::class)
fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        println("Expected one or more android app bundle files to be passed.")
        println("Usage:")
        println("     ./<path-to-script> <path-to-aab> [<path-to-aab2>...]")
        exitProcess(1)
    }

    val files =
        args.flatMap { path ->
            val f = File(path)
            if (f.isDirectory) {
                (f.listFiles()?.toList()) ?: emptyList()
            } else {
                listOf(f)
            }
        }

    println("Analyzing ${files.size} bundles")

    val output = File("output.csv")
    output.writeText(BundleInfo.CSV_HEADER + "\n")
    println(BundleInfo.CSV_HEADER)
    files
        .asFlow()
        .flatMapMerge { bundleFile ->
            flow { emit(BundleInfo.from(bundleFile).toCsvLine()) }.flowOn(Dispatchers.IO)
        }
        .toList()
        .forEach {
            output.appendText(it + "\n")
            println(it)
        }
    println("Analysis complete, printed to ${output.absolutePath}")
}
