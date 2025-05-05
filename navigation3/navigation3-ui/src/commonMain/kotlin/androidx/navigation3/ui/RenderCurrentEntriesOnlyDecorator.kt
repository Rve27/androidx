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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator

/**
 * A [NavEntryDecorator] that wraps each entry in a shared element that is controlled by the
 * [Scene].
 */
internal object RenderCurrentEntriesOnlyDecorator : NavEntryDecorator {
    @Composable
    override fun <T : Any> DecorateEntry(entry: NavEntry<T>) {
        if (LocalEntriesToRenderInCurrentScene.current.contains(entry.key)) {
            entry.content(entry.key)
        }
    }
}

/**
 * The entry keys to render in the current [Scene], in the sense of the target of the animation for
 * an [AnimatedContent] that is transitioning between different scenes.
 */
public val LocalEntriesToRenderInCurrentScene: ProvidableCompositionLocal<Set<Any>> =
    compositionLocalOf<Set<Any>> {
        throw IllegalStateException(
            "Unexpected access to LocalEntriesToRenderInCurrentScene. You should only " +
                "access LocalEntriesToRenderInCurrentScene inside a NavEntry passed " +
                "to SceneNavDisplay."
        )
    }
