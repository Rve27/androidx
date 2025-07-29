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

package androidx.navigationevent

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.navigationevent.testing.TestNavigationEvent
import androidx.navigationevent.testing.TestNavigationEventCallback
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class OnBackInvokedInputHandlerTest {

    @Test
    fun testSimpleInvoker() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        OnBackInvokedInputHandler(dispatcher, invoker)

        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        callback.remove()

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerEnableDisable() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        OnBackInvokedInputHandler(dispatcher, invoker)

        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)

        callback.isEnabled = true

        assertThat(registerCount).isEqualTo(2)
    }

    @Test
    fun testCallbackEnabledDisabled() {
        val callback = TestNavigationEventCallback(isEnabled = false)
        assertThat(callback.isEnabled).isFalse()

        callback.isEnabled = true
        assertThat(callback.isEnabled).isTrue()

        callback.isEnabled = false
        assertThat(callback.isEnabled).isFalse()
    }

    @Test
    fun testInvokerAddDisabledCallback() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val callback = TestNavigationEventCallback(isEnabled = false)

        OnBackInvokedInputHandler(dispatcher, invoker)

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(0)

        callback.isEnabled = true

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testInvokerAddEnabledCallbackBeforeSet() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        OnBackInvokedInputHandler(dispatcher, invoker)

        assertThat(registerCount).isEqualTo(1)

        callback.isEnabled = false

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallback() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val inputHandler = OnBackInvokedInputHandler(dispatcher, invoker)

        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        inputHandler.sendOnStarted(TestNavigationEvent())
        assertThat(callback.startedInvocations).isEqualTo(1)

        inputHandler.sendOnProgressed(TestNavigationEvent())
        assertThat(callback.progressedInvocations).isEqualTo(1)

        inputHandler.sendOnCancelled()
        assertThat(callback.cancelledInvocations).isEqualTo(1)

        callback.remove()

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallbackRemovedCancel() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val inputHandler = OnBackInvokedInputHandler(dispatcher, invoker)

        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        inputHandler.sendOnStarted(TestNavigationEvent())

        callback.remove()
        assertThat(callback.cancelledInvocations).isEqualTo(1)

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallbackRemovedCancelInHandleOnStarted() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val inputHandler = OnBackInvokedInputHandler(dispatcher, invoker)

        val callback = TestNavigationEventCallback(onEventStarted = { remove() })

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        inputHandler.sendOnStarted(TestNavigationEvent())

        assertThat(callback.cancelledInvocations).isEqualTo(1)

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun testSimpleAnimatedCallbackAddedContinue() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}

        val inputHandler = OnBackInvokedInputHandler(dispatcher, invoker)

        val callback = TestNavigationEventCallback()

        dispatcher.addCallback(callback)

        assertThat(registerCount).isEqualTo(1)

        inputHandler.sendOnStarted(TestNavigationEvent())

        dispatcher.addCallback(TestNavigationEventCallback())

        inputHandler.sendOnCompleted()

        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun testDoubleStartCallbackCausesCancel() {
        var registerCount = 0
        var unregisterCount = 0
        val invoker =
            object : OnBackInvokedDispatcher {
                override fun registerOnBackInvokedCallback(p0: Int, p1: OnBackInvokedCallback) {
                    registerCount++
                }

                override fun unregisterOnBackInvokedCallback(p0: OnBackInvokedCallback) {
                    unregisterCount++
                }
            }

        val dispatcher = NavigationEventDispatcher {}
        val inputHandler = OnBackInvokedInputHandler(dispatcher, invoker)

        val callback1 = TestNavigationEventCallback()

        dispatcher.addCallback(callback1)

        assertThat(registerCount).isEqualTo(1)

        inputHandler.sendOnStarted(TestNavigationEvent())

        val callback2 = TestNavigationEventCallback()

        dispatcher.addCallback(callback2)

        inputHandler.sendOnStarted(TestNavigationEvent())

        assertThat(registerCount).isEqualTo(1)

        assertThat(callback1.cancelledInvocations).isEqualTo(1)

        assertThat(callback2.startedInvocations).isEqualTo(1)
    }
}
