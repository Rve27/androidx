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
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlin.test.Test
import org.junit.Rule

class NavDisplayBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun simpleNavDisplay() {
        benchmarkRule.benchmarkToFirstPixel { NavigationTestCase() }
    }
}

private class NavigationTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val backStack = remember { mutableStateListOf<Any>("Test1") }

        NavDisplay(
            backStack,
            entryProvider = entryProvider { entry(key = "Test1") { BasicText("Text") } },
        )
    }
}
