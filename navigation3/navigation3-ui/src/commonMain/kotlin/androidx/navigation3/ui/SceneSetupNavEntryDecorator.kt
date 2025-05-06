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

package androidx.navigation3.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator

/**
 * A [NavEntryDecorator] that wraps each entry in a [movableContentOf] to allow nav displays to
 * arbitrarily place entries in different places in the composable call hierarchy and ensures that
 * the same entry content is not composed multiple times in different places of the hierarchy.
 *
 * This should likely be the first [NavEntryDecorator] to ensure that other
 * [NavEntryDecorator.DecorateEntry] calls that are stateful are moved properly inside the
 * [movableContentOf].
 */
public object SceneSetupNavEntryDecorator : NavEntryDecorator {

    @Composable
    override fun DecorateBackStack(backStack: List<Any>, content: @Composable (() -> Unit)) {
        val backStackKeys = backStack.toSet()

        // This is an intricate dance to create a movableContentOf for each entry that is scoped
        // to the backstack, that calls the correct updated content.
        // First we associate each key in the backstack with a MutableState that will contain
        // the actual content of the entry, as updated in DecorateEntry.
        // The MutableState's remembered lifecycle precisely matches when its key is in the
        // backstack.
        val movableContentContentHolderMap: Map<Any, MutableState<@Composable () -> Unit>> =
            backStackKeys.associateWith { key ->
                key(key) {
                    remember {
                        mutableStateOf(
                            @Composable {
                                error(
                                    "Should not be called, this should always be updated in" +
                                        "DecorateEntry with the real content"
                                )
                            }
                        )
                    }
                }
            }

        // Second we create another map containing the movable contents themselves, again
        // by associating the backstack key with a remembered movableContentOf
        // The critical thing here is that the movableContentOf's remembered lifecycle precisely
        // matches when its key is in the backstack.
        val movableContentHolderMap: Map<Any, @Composable () -> Unit> =
            backStackKeys.associateWith { key ->
                key(key) {
                    remember {
                        movableContentOf {
                            // In case the key is removed from the backstack while this is still
                            // being rendered, we remember the MutableState directly to allow
                            // rendering it while we are animating out.
                            remember { movableContentContentHolderMap.getValue(key) }.value()
                        }
                    }
                }
            }
        CompositionLocalProvider(
            LocalSceneSetupNavLocalInfo provides
                SceneSetupNavLocalInfo(movableContentHolderMap, movableContentContentHolderMap),
            content = content,
        )
    }

    @Composable
    override fun <T : Any> DecorateEntry(entry: NavEntry<T>) {
        val movableContentNavLocalInfo = LocalSceneSetupNavLocalInfo.current
        if (LocalEntriesToRenderInCurrentScene.current.contains(entry.key)) {
            key(entry.key) {
                // In case the key is removed from the backstack while this is still
                // being rendered, we remember the MutableState directly to allow
                // updating it while we are animating out.
                val movableContentContentHolder = remember {
                    movableContentNavLocalInfo.movableContentContentHolderMap.getValue(entry.key)
                }
                // Update the state holder with the actual entry content
                movableContentContentHolder.value = { entry.content(entry.key) }
                // In case the key is removed from the backstack while this is still
                // being rendered, we remember the movableContent directly to allow
                // rendering it while we are animating out.
                val movableContentHolder = remember {
                    movableContentNavLocalInfo.movableContentHolderMap.getValue(entry.key)
                }
                // Finally, render the entry content via the movableContentOf
                movableContentHolder()
            }
        }
    }
}

internal val LocalSceneSetupNavLocalInfo =
    staticCompositionLocalOf<SceneSetupNavLocalInfo> {
        error(
            "CompositionLocal LocalMovableContentNavLocalInfo not present. You must call " +
                "DecorateBackStack before calling DecorateEntry."
        )
    }

/**
 * The entry keys to render in the current [Scene], in the sense of the target of the animation for
 * an [AnimatedContent] that is transitioning between different scenes.
 */
public val LocalEntriesToRenderInCurrentScene: ProvidableCompositionLocal<Set<Any>> =
    compositionLocalOf {
        throw IllegalStateException(
            "Unexpected access to LocalEntriesToRenderInCurrentScene. You should only " +
                "access LocalEntriesToRenderInCurrentScene inside a NavEntry passed " +
                "to NavDisplay."
        )
    }

@Immutable
internal class SceneSetupNavLocalInfo(
    val movableContentHolderMap: Map<Any, @Composable () -> Unit>,
    val movableContentContentHolderMap: Map<Any, MutableState<@Composable () -> Unit>>
)
