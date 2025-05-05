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
import androidx.navigation3.runtime.NavEntry

/**
 * A strategy that tries to calculate a [Scene] given a list of [NavEntry].
 *
 * If the list of [NavEntry] does not result in a [Scene] for this strategy, `null` will be returned
 * instead to delegate to another strategy.
 */
public fun interface SceneStrategy<T : Any> {
    @Composable
    public fun calculateScene(
        entries: List<NavEntry<T>>,
    ): Scene<T>?

    /**
     * Chains this [SceneStrategy] with another [sceneStrategy] to return a combined
     * [SceneStrategy].
     */
    public infix fun then(sceneStrategy: SceneStrategy<T>): SceneStrategy<T> =
        SceneStrategy { entries ->
            calculateScene(entries) ?: sceneStrategy.calculateScene(entries)
        }
}
