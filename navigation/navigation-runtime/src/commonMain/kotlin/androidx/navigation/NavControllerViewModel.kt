/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.navigation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.get
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * NavControllerViewModel is the always up to date view of the NavController's non configuration
 * state
 */
// TODO(mgalhardo): Delete `NavControllerViewModel` and use `ViewModelStoreProvider` directly.
internal class NavControllerViewModel
private constructor(private val provider: ViewModelStoreProvider) :
    ViewModel(), NavViewModelStoreProvider {

    fun clear(backStackEntryId: String) {
        provider.clearKey(backStackEntryId)
    }

    override fun onCleared() {
        provider.clearAllKeys()
    }

    override fun getViewModelStore(backStackEntryId: String): ViewModelStore {
        return provider.getOrCreate(backStackEntryId)
    }

    override fun toString(): String = "NavControllerViewModel(provider=$provider)"

    companion object {

        @VisibleForTesting
        fun create(): NavControllerViewModel {
            val provider =
                ViewModelStoreProvider(
                    parentStore = null,
                    parentKey = "androidx.navigation.NavControllerViewModel",
                )
            return NavControllerViewModel(provider)
        }

        fun getInstance(viewModelStore: ViewModelStore): NavControllerViewModel {
            val factory = viewModelFactory {
                initializer {
                    val provider =
                        ViewModelStoreProvider(
                            parentStore = viewModelStore,
                            parentKey = "androidx.navigation.NavControllerViewModel",
                        )
                    NavControllerViewModel(provider)
                }
            }
            val viewModelProvider = ViewModelProvider.create(viewModelStore, factory)
            return viewModelProvider.get()
        }
    }
}
