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

package androidx.appfunctions.testing.internal

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.internal.AppFunctionReader
import androidx.appfunctions.internal.findImpl
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.service.internal.AggregatedAppFunctionInventory
import androidx.appfunctions.service.internal.AppFunctionInventory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@RequiresApi(Build.VERSION_CODES.S)
internal class FakeAppFunctionReader(context: Context) : AppFunctionReader {
    private val packageToFunctionMetadataMap:
        MutableMap<String, MutableMap<String, AppFunctionStaticAndRuntimeMetadata>> =
        mutableMapOf()

    init {
        val compiledInventories: List<AppFunctionInventory>? =
            try {
                AggregatedAppFunctionInventory::class
                    .java
                    .findImpl(prefix = "$", suffix = "_Impl")
                    .inventories
            } catch (e: Exception) {
                Log.d("AppFunctionsTesting", "No aggregated inventory found.", e)
                null
            }
        for (inventory in compiledInventories ?: listOf()) {
            packageToFunctionMetadataMap.putIfAbsent(context.packageName, mutableMapOf())
            for ((id, staticMetadata) in inventory.functionIdToMetadataMap) {
                packageToFunctionMetadataMap
                    .getValue(context.packageName)
                    .put(
                        id,
                        AppFunctionStaticAndRuntimeMetadata(
                            staticMetadata,
                            AppFunctionRuntimeMetadata(
                                AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT
                            ),
                        ),
                    )
            }
        }
    }

    override fun searchAppFunctions(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionMetadata>> =
        // TODO: b/418017070 - Handle updates
        flowOf(
            packageToFunctionMetadataMap.entries.flatMap { (packageName, metadataMap) ->
                metadataMap.values.map {
                    AppFunctionMetadata(
                        id = it.staticMetadata.id,
                        packageName = packageName,
                        isEnabled =
                            computeEffectivelyEnabled(it.staticMetadata, it.runtimeMetadata),
                        schema = it.staticMetadata.schema,
                        parameters = it.staticMetadata.parameters,
                        response = it.staticMetadata.response,
                        components = it.staticMetadata.components,
                    )
                }
            }
        )

    private fun computeEffectivelyEnabled(
        staticMetadata: CompileTimeAppFunctionMetadata,
        runtimeMetadata: AppFunctionRuntimeMetadata,
    ): Boolean =
        when (runtimeMetadata.enabled) {
            AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_ENABLED -> true
            AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DISABLED -> false
            AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DEFAULT ->
                staticMetadata.isEnabledByDefault
            else ->
                throw IllegalStateException(
                    "Unknown AppFunction state: ${runtimeMetadata.enabled}."
                )
        }

    override suspend fun getAppFunctionSchemaMetadata(
        functionId: String,
        packageName: String,
    ): AppFunctionSchemaMetadata? {
        TODO("Not yet implemented")
    }
}

private data class AppFunctionRuntimeMetadata(
    @AppFunctionManagerCompat.EnabledState val enabled: Int
)

private data class AppFunctionStaticAndRuntimeMetadata(
    val staticMetadata: CompileTimeAppFunctionMetadata,
    val runtimeMetadata: AppFunctionRuntimeMetadata,
)
