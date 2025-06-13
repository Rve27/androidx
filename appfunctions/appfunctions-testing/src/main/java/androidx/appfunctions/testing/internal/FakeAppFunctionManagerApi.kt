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

import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.AppFunctionManagerApi

// TODO: b/418017070 - Implement
internal class FakeAppFunctionManagerApi : AppFunctionManagerApi {
    override suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse {
        TODO("Not yet implemented")
    }

    override suspend fun isAppFunctionEnabled(packageName: String, functionId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setAppFunctionEnabled(functionId: String, newEnabledState: Int) {
        TODO("Not yet implemented")
    }
}
