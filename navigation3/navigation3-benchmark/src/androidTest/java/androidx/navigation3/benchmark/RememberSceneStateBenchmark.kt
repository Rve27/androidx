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

package androidx.navigation3.benchmark

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import kotlin.test.Test
import org.junit.Rule

internal class RememberSceneStateBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactory = {
        object : LayeredComposeTestCase(), ToggleableTestCase {
            var keyCount: Int = 0
            val backStack = mutableStateListOf(keyCount++)

            @Composable
            override fun MeasuredContent() {
                rememberSceneState(
                    entries =
                        rememberDecoratedNavEntries(backStack = backStack) { key ->
                            NavEntry(key) { BasicText("Key = $key") }
                        },
                    sceneStrategy = SinglePaneSceneStrategy(),
                    onBack = { /* no-op */ },
                )
            }

            override fun toggleState() {
                backStack.add(keyCount++)
            }
        }
    }

    /**
     * Measure the time taken to compose the scene from scratch. This is the time taken to call the
     * [rememberSceneState] composable function and its content.
     */
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(caseFactory)
    }

    /**
     * Measure the time taken by the first time measure the scene. This is mainly the time used to
     * measure all the [Measurable]s in the scene.
     */
    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(caseFactory)
    }

    /**
     * Measure the time taken by the first time layout the scene. This is mainly the time used to
     * place [Placeable]s in the scene.
     */
    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(caseFactory)
    }

    /** Measure the time taken by first time draw the scene. */
    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(caseFactory)
    }

    /**
     * Measure the time taken by layout the scene after the layout constraints changed. This is
     * mainly the time used to re-measure and re-layout the composable.
     */
    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(caseFactory)
    }

    /** Measure the time taken to recompose the scene when the back stack state gets toggled. */
    @Test
    fun toggleState_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(caseFactory)
    }

    /** Measure the time taken to measure the scene when the back stack state gets toggled. */
    @Test
    fun toggleState_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(caseFactory)
    }

    /** Measure the time taken to layout the scene when the back stack state gets toggled. */
    @Test
    fun toggleState_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(caseFactory)
    }

    /** Measure the time taken to draw the scene when the back stack state gets toggled. */
    @Test
    fun toggleState_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(caseFactory)
    }
}
