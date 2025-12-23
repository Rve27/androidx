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

package androidx.pdf.annotation

import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.pdf.annotation.highlights.InProgressHighlightsView
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.view.FakePdfDocument
import androidx.pdf.view.PdfViewTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnnotationsViewTest {

    private lateinit var annotationsView: AnnotationsView
    private lateinit var fakePdfDocument: FakePdfDocument
    private lateinit var testHighlightListener: FakeInProgressTextHighlightsListener

    private val startIdlingResource = CountingIdlingResource(HIGHLIGHT_START_RESOURCE_NAME)
    private val finishIdlingResource = CountingIdlingResource(HIGHLIGHT_FINISH_RESOURCE_NAME)

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(startIdlingResource, finishIdlingResource)

        val pageText =
            PdfPageTextContent(bounds = listOf(RectF(0f, 0f, 100f, 100f)), text = "Test Content")
        fakePdfDocument =
            FakePdfDocument(pages = listOf(Point(100, 100)), textContents = listOf(pageText))

        testHighlightListener =
            FakeInProgressTextHighlightsListener(startIdlingResource, finishIdlingResource)

        setupActivity()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(startIdlingResource, finishIdlingResource)
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun highlighterConfig_controlsInternalViewVisibility() {
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            scenario.onActivity {
                // 1. Initially GONE
                val internalView = getHighlightView(annotationsView)
                assertThat(internalView.visibility).isEqualTo(View.GONE)

                // 2. Set Config -> VISIBLE
                val config =
                    AnnotationsView.HighlighterConfig(
                        color = Color.YELLOW,
                        pdfDocument = fakePdfDocument,
                    )
                annotationsView.setHighlighter(config)
                assertThat(internalView.visibility).isEqualTo(View.VISIBLE)

                // 3. Set Null -> GONE
                annotationsView.setHighlighter(null)
                assertThat(internalView.visibility).isEqualTo(View.GONE)
            }
        }
    }

    @Test
    fun onTouchEvent_whenHighlighterEnabled_consumesAndDispatchesEvents() {
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            startIdlingResource.increment()

            scenario.onActivity {
                val config =
                    AnnotationsView.HighlighterConfig(
                        color = Color.YELLOW,
                        pdfDocument = fakePdfDocument,
                    )
                annotationsView.setHighlighter(config)
                annotationsView.addInProgressTextHighlightsListener(testHighlightListener)

                // Touch down on valid text
                val event = obtainMotionEvent(10f, 10f, MotionEvent.ACTION_DOWN)
                val consumed = annotationsView.onTouchEvent(event)
                event.recycle()

                assertThat(consumed).isTrue()
            }

            Espresso.onIdle()

            assertThat(testHighlightListener.isStarted).isTrue()
        }
    }

    @Test
    fun onTouchEvent_whenHighlighterDisabled_ignoresEvents() {
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            scenario.onActivity {
                annotationsView.setHighlighter(null)
                annotationsView.addInProgressTextHighlightsListener(testHighlightListener)

                val event = obtainMotionEvent(10f, 10f, MotionEvent.ACTION_DOWN)
                val consumed = annotationsView.onTouchEvent(event)
                event.recycle()

                assertThat(consumed).isFalse()
            }

            Espresso.onIdle()

            assertThat(testHighlightListener.isStarted).isFalse()
        }
    }

    @Test
    fun addInProgressTextHighlightsListener_multipleListeners_allReceiveEvents() {
        val listenerA =
            FakeInProgressTextHighlightsListener(startIdlingResource, finishIdlingResource)
        val listenerB =
            FakeInProgressTextHighlightsListener(startIdlingResource, finishIdlingResource)

        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            repeat(2) { startIdlingResource.increment() }

            scenario.onActivity {
                val config = AnnotationsView.HighlighterConfig(Color.YELLOW, fakePdfDocument)
                annotationsView.setHighlighter(config)

                annotationsView.addInProgressTextHighlightsListener(listenerA)
                annotationsView.addInProgressTextHighlightsListener(listenerB)

                val event = obtainMotionEvent(10f, 10f, MotionEvent.ACTION_DOWN)
                annotationsView.onTouchEvent(event)
                event.recycle()
            }

            Espresso.onIdle()

            assertThat(listenerA.isStarted).isTrue()
            assertThat(listenerB.isStarted).isTrue()
        }
    }

    private fun setupActivity() {
        PdfViewTestActivity.onCreateCallback = { activity ->
            annotationsView =
                AnnotationsView(activity).apply {
                    layoutParams =
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    pageInfoProvider = FakePageInfoProvider()
                }
            activity.container.addView(annotationsView)
        }
    }

    private fun getHighlightView(parent: AnnotationsView): View {
        val view =
            (0 until parent.childCount)
                .map { parent.getChildAt(it) }
                .find { it is InProgressHighlightsView }

        assertWithMessage("InProgressHighlightsView not found as child of AnnotationsView")
            .that(view)
            .isNotNull()
        return view!!
    }

    private fun obtainMotionEvent(x: Float, y: Float, action: Int): MotionEvent {
        val now = SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, action, x, y, 0)
    }

    companion object {
        private val HIGHLIGHT_START_RESOURCE_NAME = "TextHighlightStart-${UUID.randomUUID()}"
        private val HIGHLIGHT_FINISH_RESOURCE_NAME = "TextHighlightFinish-${UUID.randomUUID()}"
    }
}
