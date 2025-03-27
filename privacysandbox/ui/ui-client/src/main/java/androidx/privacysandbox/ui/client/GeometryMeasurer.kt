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

package androidx.privacysandbox.ui.client

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Class responsible for calculating geometry data for one or more [android.view.View]s.
 *
 * Clients of this class can register to receive geometry data on their view using [registerView].
 * This will return a [kotlinx.coroutines.flow.Flow] that emits a new [GeometryData] object every
 * time a measurement is performed. Clients should call [unregisterView] when there is no longer a
 * need to measure the [android.view.View].
 *
 * When an event occurs that means that the geometry of a client's view has changed, the client
 * should call [requestUpdatedSignals]. When at least one client requests updated signals, this
 * class will calculate updated signals for all clients. This measurement is throttled so that
 * geometry data is not emitted more than once per [MIN_SIGNAL_LATENCY_MS] milliseconds.
 */
// TODO(b/406270265): Improve debuggability of these signals.
internal class GeometryMeasurer(val clock: Clock = Clock { SystemClock.uptimeMillis() }) {

    companion object {
        @Volatile private var instance: GeometryMeasurer? = null

        private const val MIN_SIGNAL_LATENCY_MS = 200

        fun getInstance(): GeometryMeasurer {
            return instance ?: synchronized(this) { GeometryMeasurer().also { instance = it } }
        }
    }

    private var targetViews: MutableMap<View, ViewGeometryCalculator> = mutableMapOf()
    private var scheduledTask: Job? = null
    private var lastTimeSentSignals: Long? = null
    private var scope = CoroutineScope(Dispatchers.Main)
    private val mutex = Mutex()

    data class GeometryData(
        val widthPx: Int,
        val heightPx: Int,
        val onScreenGeometry: Rect,
        val opacityHint: Float
    )

    private class ViewGeometryCalculator(val view: View) {
        var flow = MutableSharedFlow<GeometryData>()
        private var containerHeightPx = 0
        private var containerWidthPx = 0
        private var onScreenGeometry = Rect()
        private var windowLocation = IntArray(2)
        private var opacityHint = 1f

        suspend fun calculateAndEmitGeometry() {
            if (view.windowVisibility == View.VISIBLE) {
                val isVisible = view.getGlobalVisibleRect(onScreenGeometry)
                if (!isVisible) {
                    onScreenGeometry.set(-1, -1, -1, -1)
                } else {
                    view.getLocationOnScreen(windowLocation)
                    onScreenGeometry.offset(-windowLocation[0], -windowLocation[1])
                    onScreenGeometry.intersect(0, 0, view.width, view.height)
                }
            } else {
                onScreenGeometry.set(-1, -1, -1, -1)
            }
            containerHeightPx = view.height
            containerWidthPx = view.width
            opacityHint = view.alpha
            flow.emit(
                GeometryData(containerWidthPx, containerHeightPx, onScreenGeometry, opacityHint)
            )
        }
    }

    fun requestUpdatedSignals() {
        if (scheduledTask != null) {
            return
        }
        var delayToNextSend = 0L
        lastTimeSentSignals?.let { time ->
            if ((clock.uptimeMillis() - time) < MIN_SIGNAL_LATENCY_MS) {
                delayToNextSend = MIN_SIGNAL_LATENCY_MS - (clock.uptimeMillis() - time)
            }
        }

        scheduledTask =
            scope.launch {
                delay(delayToNextSend)
                measureAndReportGeometryData()
                lastTimeSentSignals = SystemClock.uptimeMillis()
                scheduledTask = null
            }
    }

    suspend fun registerView(view: View): Flow<GeometryData> {
        mutex.withLock {
            val geometryCalculator = targetViews.getOrPut(view) { ViewGeometryCalculator(view) }
            return geometryCalculator.flow
        }
    }

    suspend fun unregisterView(view: View) {
        mutex.withLock {
            targetViews.remove(view)
            if (targetViews.isEmpty()) {
                scheduledTask = null
                lastTimeSentSignals = null
            }
        }
    }

    private suspend fun measureAndReportGeometryData() {
        mutex.withLock {
            targetViews.forEach { (_, geometryCalculator) ->
                geometryCalculator.calculateAndEmitGeometry()
            }
        }
    }

    fun interface Clock {
        fun uptimeMillis(): Long
    }
}
