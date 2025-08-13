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

package androidx.xr.glimmer.demos

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION

/** Main [Activity] containing all Glimmer related demos. */
class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            SystemBarStyle.dark(Color.TRANSPARENT),
            SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        ComposeView(this)
            .also { setContentView(it) }
            .setContent {
                val navigator =
                    rememberSaveable(saver = Navigator.Saver(Demos, onBackPressedDispatcher)) {
                        Navigator(Demos, onBackPressedDispatcher)
                    }

                DemoApp(
                    currentDemo = navigator.currentDemo,
                    onNavigateToDemo = { demo -> navigator.navigateTo(demo) },
                )
            }
    }

    private var secondaryPointerUpEventTime: Long? = null

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            if (handleTwoPointerBackTapNavigation(ev)) return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    /**
     * Handles a two-pointer tap to invoke back navigation.
     *
     * @return true if back navigation was invoked, false otherwise
     */
    private fun handleTwoPointerBackTapNavigation(motionEvent: MotionEvent): Boolean {
        if (motionEvent.isFromSource(SOURCE_TOUCH_NAVIGATION)) {
            if (
                motionEvent.actionMasked == MotionEvent.ACTION_POINTER_UP &&
                    motionEvent.pointerCount == 2
            ) {
                // The secondary pointer was released (one is still touching), track event time
                secondaryPointerUpEventTime = motionEvent.eventTime
            }
            if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                if (secondaryPointerUpEventTime != null) {
                    // Last pointer was released. If we previously released a secondary pointer,
                    // and this pointer was released within a short time of that, invoke back
                    val timeBetweenUpEvents = motionEvent.eventTime - secondaryPointerUpEventTime!!
                    secondaryPointerUpEventTime = null
                    if (timeBetweenUpEvents < 500) {
                        onBackPressedDispatcher.onBackPressed()
                        return true
                    }
                }
            }
        }
        return false
    }
}

private class Navigator
private constructor(
    private val backDispatcher: OnBackPressedDispatcher,
    initialDemo: Demo,
    private val backStack: MutableList<Demo>,
) {
    constructor(
        rootDemo: Demo,
        backDispatcher: OnBackPressedDispatcher,
    ) : this(backDispatcher, rootDemo, mutableListOf<Demo>())

    private val onBackPressed =
        object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    popBackStack()
                }
            }
            .apply {
                isEnabled = !isRoot
                backDispatcher.addCallback(this)
            }

    private var _currentDemo by mutableStateOf(initialDemo)
    var currentDemo: Demo
        get() = _currentDemo
        private set(value) {
            _currentDemo = value
            onBackPressed.isEnabled = !isRoot
        }

    val isRoot: Boolean
        get() = backStack.isEmpty()

    fun navigateTo(demo: Demo) {
        backStack.add(currentDemo)
        currentDemo = demo
    }

    private fun popBackStack() {
        currentDemo = backStack.removeAt(backStack.lastIndex)
    }

    companion object {
        fun Saver(rootDemo: Demo, backDispatcher: OnBackPressedDispatcher): Saver<Navigator, *> =
            listSaver(
                save = { navigator ->
                    (navigator.backStack + navigator.currentDemo).map { it.title }
                },
                restore = { restored ->
                    require(restored.isNotEmpty()) { "no restored items" }
                    val backStack =
                        restored.mapTo(mutableListOf()) {
                            requireNotNull(findDemo(rootDemo, it)) { "could not find demo" }
                        }
                    val initial = backStack.removeAt(backStack.lastIndex)
                    Navigator(backDispatcher, initial, backStack)
                },
            )

        fun findDemo(demo: Demo, title: String): Demo? {
            if (demo.title == title) return demo
            if (demo is DemoCategory) {
                demo.demos.forEach { child ->
                    findDemo(child, title)?.let {
                        return it
                    }
                }
            }
            return null
        }

        private fun MutableList<Demo>.addDemos(demo: Demo, title: String, exact: Boolean = false) {
            if ((exact && demo.title == title) || (!exact && demo.title.contains(title))) {
                add(demo)
            }
            if (demo is DemoCategory) {
                demo.demos.forEach { addDemos(it, title, exact) }
            }
        }
    }
}
