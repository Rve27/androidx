/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.savedState

/**
 * Wraps the content of a [NavEntry] with a [SaveableStateHolder.SaveableStateProvider] to ensure
 * that calls to [rememberSaveable] within the content work properly and that state can be saved and
 * provides the content of a [NavEntry] with a [SavedStateRegistryOwner] and provides that
 * [SavedStateRegistryOwner] as a [LocalSavedStateRegistryOwner] so that it is available within the
 * content.
 *
 * This [NavEntryDecorator] is the only one that is **required** as saving state is considered a
 * non-optional feature.
 */
public object SavedStateNavEntryDecorator : NavEntryDecorator {

    @Composable
    override fun DecorateBackStack(backStack: List<Any>, content: @Composable () -> Unit) {
        val localInfo = remember { SavedStateNavLocalInfo() }
        DisposableEffect(key1 = backStack) { onDispose { localInfo.refCount.clear() } }

        localInfo.savedStateHolder = rememberSaveableStateHolder()
        backStack.forEachIndexed { index, key ->
            // We update here as part of composition to ensure the value is available to
            // DecorateEntry
            localInfo.refCount.getOrPut(key) { LinkedHashSet<Int>() }.add(getIdForKey(key, index))
            DisposableEffect(key1 = key) {
                // We update here at the end of composition in case the backstack changed and
                // everything was cleared.
                localInfo.refCount
                    .getOrPut(key) { LinkedHashSet<Int>() }
                    .add(getIdForKey(key, index))
                onDispose {
                    // If the backStack count is less than the refCount for the key, remove the
                    // state since that means we removed a key from the backstack, and set the
                    // refCount to the backstack count.
                    val backstackCount = backStack.count { it == key }
                    val lastKeyCount = localInfo.refCount[key]?.size ?: 0
                    if (backstackCount < lastKeyCount) {
                        // The set of the ids associated with this key
                        @Suppress("PrimitiveInCollection") // The order of the element matters
                        val idsSet = localInfo.refCount[key]!!
                        val id = idsSet.last()
                        idsSet.remove(id)
                        if (!localInfo.idsInComposition.contains(id)) {
                            localInfo.savedStateHolder!!.removeState(id)
                        }
                    }
                    // If the refCount is 0, remove the key from the refCount.
                    if (localInfo.refCount[key]?.isEmpty() == true) {
                        localInfo.refCount.remove(key)
                    }
                }
            }
        }

        CompositionLocalProvider(LocalSavedStateNavLocalInfo provides localInfo) {
            content.invoke()
        }
    }

    @Composable
    override fun <T : Any> DecorateEntry(entry: NavEntry<T>) {
        val localInfo = LocalSavedStateNavLocalInfo.current
        val key = entry.key

        // Tracks whether the key is changed
        var keyChanged = false
        var id: Int =
            rememberSaveable(key) {
                keyChanged = true
                localInfo.refCount[key]!!.last()
            }
        id =
            rememberSaveable(localInfo.refCount[key]?.size) {
                // if the key changed, use the current id
                // If the key was not changed, and the current id is not in composition or on the
                // back stack then update the id with the last item from the backstack with the
                // associated key. This ensures that we can handle duplicates, both consecutive and
                // non-consecutive
                if (
                    !keyChanged &&
                        (!localInfo.idsInComposition.contains(id) ||
                            localInfo.refCount[key]?.contains(id) == true)
                ) {
                    localInfo.refCount[key]!!.last()
                } else {
                    id
                }
            }

        val childRegistry by
            rememberSaveable(
                key,
                stateSaver =
                    Saver(
                        save = { it.savedState },
                        restore = { EntrySavedStateRegistry().apply { savedState = it } }
                    )
            ) {
                mutableStateOf(EntrySavedStateRegistry())
            }

        DisposableEffect(key1 = key) {
            localInfo.idsInComposition.add(id)
            childRegistry.lifecycle.currentState = Lifecycle.State.RESUMED
            onDispose {
                if (localInfo.idsInComposition.remove(id) && !localInfo.refCount.contains(key)) {
                    localInfo.savedStateHolder!!.removeState(id)
                    // If the refCount is 0, remove the key from the refCount.
                    if (localInfo.refCount[key]?.isEmpty() == true) {
                        localInfo.refCount.remove(key)
                    }
                }
                val savedState = savedState()
                childRegistry.savedStateRegistryController.performSave(savedState)
                childRegistry.savedState = savedState
                childRegistry.lifecycle.currentState = Lifecycle.State.DESTROYED
            }
        }

        localInfo.savedStateHolder?.SaveableStateProvider(id) {
            CompositionLocalProvider(LocalSavedStateRegistryOwner provides childRegistry) {
                entry.content.invoke(key)
            }
        }
    }
}

internal val LocalSavedStateNavLocalInfo =
    staticCompositionLocalOf<SavedStateNavLocalInfo> {
        error(
            "CompositionLocal LocalSavedStateNavLocalInfo not present. You must call " +
                "DecorateBackStack before calling DecorateEntry."
        )
    }

internal class SavedStateNavLocalInfo {
    internal var savedStateHolder: SaveableStateHolder? = null
    internal val refCount: MutableMap<Any, LinkedHashSet<Int>> = mutableMapOf()
    @Suppress("PrimitiveInCollection") // The order of the element matters
    internal val idsInComposition: LinkedHashSet<Int> = LinkedHashSet<Int>()
}

internal fun getIdForKey(key: Any, count: Int): Int = 31 * key.hashCode() + count

private class EntrySavedStateRegistry : SavedStateRegistryOwner {
    override val lifecycle: LifecycleRegistry = LifecycleRegistry(this)
    val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    var savedState: SavedState? = null

    init {
        savedStateRegistryController.performRestore(savedState)
    }
}
