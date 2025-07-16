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

package androidx.appfunctions.compiler.core

import androidx.annotation.VisibleForTesting
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

private const val PARAM_TAG_REGEX_PATTERN = """^@param\s+(\w+)\s*(.*)"""
private const val ANY_TAG_REGEX_PATTERN = """^@\w+.*"""
private val PARAM_TAG_REGEX = Regex(PARAM_TAG_REGEX_PATTERN)
private val ANY_TAG_REGEX = Regex(ANY_TAG_REGEX_PATTERN)

/**
 * Returns a mapping of parameter name to parameter description, where the parameter's description
 * is extracted from `@param` declarations in the KDoc.
 */
fun KSFunctionDeclaration.getParamDescriptionsFromKDoc(): Map<String, String> {
    return if (docString != null) {
        getParamDescriptionsFromKDoc(checkNotNull(docString))
    } else {
        mapOf()
    }
}

/** The input docString is expected to be stripped from any "/**", "*/" or "*". */
@VisibleForTesting
internal fun getParamDescriptionsFromKDoc(docString: String): Map<String, String> {
    val descriptionMap = mutableMapOf<String, String>()

    val currentDescriptionBuilder = StringBuilder()
    var currentParamName: String? = null

    for (line in docString.lines()) {
        val trimmedLine = line.trim()
        val paramMatch = PARAM_TAG_REGEX.find(trimmedLine)

        when {
            paramMatch != null -> {
                if (currentParamName != null) {
                    descriptionMap[currentParamName] = currentDescriptionBuilder.toString().trim()
                    currentDescriptionBuilder.clear()
                }
                currentParamName = checkNotNull(paramMatch.groupValues[1])
                currentDescriptionBuilder.append(checkNotNull(paramMatch.groupValues[2]))
            }

            ANY_TAG_REGEX.matches(trimmedLine) -> {
                if (currentParamName != null) {
                    descriptionMap[currentParamName] = currentDescriptionBuilder.toString().trim()
                    currentDescriptionBuilder.clear()
                    currentParamName = null
                }
            }

            currentParamName != null -> {
                if (trimmedLine.isNotBlank()) {
                    if (currentDescriptionBuilder.isNotEmpty()) {
                        currentDescriptionBuilder.append(" ")
                    }
                    currentDescriptionBuilder.append(trimmedLine)
                }
            }
        }
    }

    if (currentParamName != null) {
        descriptionMap[currentParamName] = currentDescriptionBuilder.toString().trim()
    }
    return descriptionMap
}

/** Returns the function's docstring with all of the tags stripped out. */
// TODO(b/431765277): remove @return tags too when they are extracted to
//  AppFunctionResponseMetadata.
fun KSFunctionDeclaration.sanitizeKdoc(): String {
    return if (docString != null) {
        sanitizeKdoc(checkNotNull(docString))
    } else {
        ""
    }
}

/** The input docString is expected to be stripped from any "/**", "*/" or "*". */
@VisibleForTesting
internal fun sanitizeKdoc(docString: String): String {
    val resultLines = mutableListOf<String>()

    for (line in docString.lines()) {
        val trimmedLine = line.trim()

        if (ANY_TAG_REGEX.matches(trimmedLine)) {
            break
        } else {
            resultLines.add(line)
        }
    }

    return resultLines.joinToString("\n").trim()
}
