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

package androidx.appfunctions.metadata

/**
 * Represents metadata about a package providing app functions.
 *
 * @property packageName name of the package.
 * @property appFunctions list of [AppFunctionMetadata] for each app function provided by the app.
 */
public class AppFunctionPackageMetadata(
    public val packageName: String,
    public val appFunctions: List<AppFunctionMetadata>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionPackageMetadata

        if (packageName != other.packageName) return false
        if (appFunctions != other.appFunctions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + appFunctions.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionPackageMetadata(packageName='$packageName', appFunctions=$appFunctions)"
    }
}
