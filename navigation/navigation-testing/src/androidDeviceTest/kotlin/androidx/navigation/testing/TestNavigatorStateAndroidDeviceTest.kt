/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation.testing

import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.navigation.testing.TestNavigatorStateTest.FloatingWindowTestNavigator
import androidx.navigation.testing.TestNavigatorStateTest.TestNavigator
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi

class TestNavigatorStateAndroidDeviceTest {

    @OptIn(ExperimentalCoroutinesApi::class) private val state = TestNavigatorState()

    @Test
    fun testLifecycle() {
        val navigator = TestNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testFloatingWindowLifecycle() {
        val navigator = FloatingWindowTestNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }
}
