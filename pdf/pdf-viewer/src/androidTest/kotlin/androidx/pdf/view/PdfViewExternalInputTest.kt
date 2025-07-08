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

package androidx.pdf.view

import android.graphics.Point
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.pdf.util.ZoomUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.EspressoKey
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewExternalInputTest {

    @Before
    fun setUp() {
        val fakePdfDocument = FakePdfDocument(List(10) { Point(2000, 4000) })
        PdfViewTestActivity.onCreateCallback = { activity ->
            val container = FrameLayout(activity)
            container.addView(
                PdfView(activity).apply {
                    pdfDocument = fakePdfDocument
                    id = PDF_VIEW_ID
                    isFocusable = true
                    isFocusableInTouchMode = true
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            activity.setContentView(container)
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testDpadUp_scrollsUp() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0
        val initialScrollY = 500

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Request focus to receive key events.
                    pdfView.post { pdfView.requestFocus() }
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Scroll down initially so there is space to scroll up.
                    pdfView.scrollTo(pdfView.scrollX, initialScrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_UP))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, KEYBOARD_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        Truth.assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(-expectedScrollDelta)

        // Verify that the view did not scroll horizontally.
        Truth.assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testDpadDown_scrollsDown() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Request focus to receive key events.
                    pdfView.post { pdfView.requestFocus() }
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Ensure we are at the top to have space to scroll down.
                    pdfView.scrollTo(pdfView.scrollX, 0)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_DOWN))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, KEYBOARD_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        Truth.assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(expectedScrollDelta)

        // Verify that the view did not scroll horizontally.
        Truth.assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testDpadLeft_scrollsLeft() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportWidth = 0
        val initialScrollX = 500

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Request focus to receive key events.
                    pdfView.post { pdfView.requestFocus() }
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Scroll right initially so there is space to scroll left.
                    pdfView.scrollTo(initialScrollX, pdfView.scrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportWidth = pdfView.viewportWidth
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_LEFT))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportWidth, KEYBOARD_HORIZONTAL_SCROLL_FACTOR)

        // Verify that the view scrolled horizontally by the correct amount.
        Truth.assertThat(scrollAfter.x - scrollBefore.x).isEqualTo(-expectedScrollDelta)

        // Verify that the view did not scroll vertically.
        Truth.assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testDpadRight_scrollsRight() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportWidth = 0

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Request focus to receive key events.
                    pdfView.post { pdfView.requestFocus() }
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Ensure we are at the far left to have space to scroll right.
                    pdfView.scrollTo(0, pdfView.scrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportWidth = pdfView.viewportWidth
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportWidth, KEYBOARD_HORIZONTAL_SCROLL_FACTOR)

        // Verify that the view scrolled horizontally by the correct amount.
        Truth.assertThat(scrollAfter.x - scrollBefore.x).isEqualTo(expectedScrollDelta)

        // Verify that the view did not scroll vertically.
        Truth.assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testMouseScrollUp_scrollsUp() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0
        val initialScrollY = 500

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Scroll down initially so there is space to scroll up.
                    pdfView.scrollTo(pdfView.scrollX, initialScrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                // Positive vscroll for scroll up
                .perform(scrollMouseWheel(1.0f, 0f))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, MOUSE_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        Truth.assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(-expectedScrollDelta)
        // Verify that the view did not scroll horizontally.
        Truth.assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testMouseScrollDown_scrollsDown() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Ensure we are at the top to have space to scroll down.
                    pdfView.scrollTo(pdfView.scrollX, 0)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                // Negative vscroll for scroll down
                .perform(scrollMouseWheel(-1.0f, 0f))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, MOUSE_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        Truth.assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(expectedScrollDelta)
        // Verify that the view did not scroll horizontally.
        Truth.assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testMouseScrollRight_scrollsRight() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportWidth = 0

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Ensure we are at the far left to have space to scroll right.
                    pdfView.scrollTo(0, pdfView.scrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportWidth = pdfView.viewportWidth
                }
                // Positive hscroll for scroll right
                .perform(scrollMouseWheel(0f, 1.0f))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportWidth, MOUSE_HORIZONTAL_SCROLL_FACTOR)

        // Verify that the view scrolled horizontally by the correct amount.
        Truth.assertThat(scrollAfter.x - scrollBefore.x).isEqualTo(expectedScrollDelta)

        // Verify that the view did not scroll vertically.
        Truth.assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testMouseScrollLeft_scrollsLeft() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportWidth = 0
        val initialScrollX = 500

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Scroll right initially so there is space to scroll left.
                    pdfView.scrollTo(initialScrollX, pdfView.scrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportWidth = pdfView.viewportWidth
                }
                // Negative hscroll for scroll left
                .perform(scrollMouseWheel(0f, -1.0f))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportWidth, MOUSE_HORIZONTAL_SCROLL_FACTOR)

        // Verify that the view scrolled horizontally by the correct amount.
        Truth.assertThat(scrollAfter.x - scrollBefore.x).isEqualTo(-expectedScrollDelta)

        // Verify that the view did not scroll vertically.
        Truth.assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testCtrlEquals_zoomsIn() {
        var zoomAfter = 0f
        var expectedZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    val baselineZoom =
                        ZoomUtils.calculateZoomToFit(
                            pdfView.viewportWidth.toFloat(),
                            pdfView.viewportHeight.toFloat(),
                            pdfView.contentWidth,
                            1f,
                        )
                    val currentZoomLevel = 1.0f
                    pdfView.zoom = baselineZoom * currentZoomLevel

                    val nextZoomLevel = 1.10f
                    expectedZoom = baselineZoom * nextZoomLevel
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_EQUALS)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView
                    zoomAfter = pdfView.zoom
                }
            close()
        }

        Truth.assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(expectedZoom)
    }

    @Test
    fun testCtrlPlus_zoomsIn() {
        var zoomAfter = 0f
        var expectedZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    val baselineZoom =
                        ZoomUtils.calculateZoomToFit(
                            pdfView.viewportWidth.toFloat(),
                            pdfView.viewportHeight.toFloat(),
                            pdfView.contentWidth,
                            1f,
                        )
                    val currentZoomLevel = 1.0f
                    pdfView.zoom = baselineZoom * currentZoomLevel

                    val nextZoomLevel = 1.10f
                    expectedZoom = baselineZoom * nextZoomLevel
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_PLUS)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView
                    zoomAfter = pdfView.zoom
                }
            close()
        }

        Truth.assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(expectedZoom)
    }

    @Test
    fun testCtrlMinus_zoomsOut() {
        var zoomAfter = 0f
        var expectedZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    val baselineZoom =
                        ZoomUtils.calculateZoomToFit(
                            pdfView.viewportWidth.toFloat(),
                            pdfView.viewportHeight.toFloat(),
                            pdfView.contentWidth,
                            1f,
                        )
                    val currentZoomLevel = 1.10f
                    pdfView.zoom = baselineZoom * currentZoomLevel

                    val previousZoomLevel = 1.0f
                    expectedZoom = baselineZoom * previousZoomLevel
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_MINUS)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView
                    zoomAfter = pdfView.zoom
                }
            close()
        }

        Truth.assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(expectedZoom)
    }

    @Test
    fun testCtrlPlusMouseScrollUp_zoomsOut() {
        var zoomAfter = 0f
        var expectedZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    val baselineZoom =
                        ZoomUtils.calculateZoomToFit(
                            pdfView.viewportWidth.toFloat(),
                            pdfView.viewportHeight.toFloat(),
                            pdfView.contentWidth,
                            1f,
                        )
                    val currentZoomLevel = 1.10f
                    pdfView.zoom = baselineZoom * currentZoomLevel

                    val previousZoomLevel = 1.0f
                    expectedZoom = baselineZoom * previousZoomLevel
                }
                .perform(scrollMouseWheel(1.0f, 0f, KeyEvent.META_CTRL_ON))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    zoomAfter = pdfView.zoom
                }
            close()
        }

        Truth.assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(expectedZoom)
    }

    @Test
    fun testCtrlPlusMouseScrollDown_zoomsIn() {
        var zoomAfter = 0f
        var expectedZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    val baselineZoom =
                        ZoomUtils.calculateZoomToFit(
                            pdfView.viewportWidth.toFloat(),
                            pdfView.viewportHeight.toFloat(),
                            pdfView.contentWidth,
                            1f,
                        )
                    val currentZoomLevel = 1.0f
                    pdfView.zoom = baselineZoom * currentZoomLevel

                    val nextZoomLevel = 1.10f
                    expectedZoom = baselineZoom * nextZoomLevel
                }
                .perform(scrollMouseWheel(-1.0f, 0f, KeyEvent.META_CTRL_ON))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    zoomAfter = pdfView.zoom
                }
            close()
        }

        Truth.assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(expectedZoom)
    }

    private fun scrollMouseWheel(vscroll: Float, hscroll: Float, metaState: Int = 0): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(PdfView::class.java)
            }

            override fun getDescription(): String {
                return "dispatch generic motion event"
            }

            override fun perform(uiController: UiController, view: View) {
                val downTime = SystemClock.uptimeMillis()
                val eventTime = SystemClock.uptimeMillis()
                val pointerProperties = arrayOf(MotionEvent.PointerProperties().apply { id = 0 })
                val pointerCoords =
                    arrayOf(
                        MotionEvent.PointerCoords().apply {
                            this.x = GeneralLocation.CENTER.calculateCoordinates(view)[0]
                            this.y = GeneralLocation.CENTER.calculateCoordinates(view)[1]
                            setAxisValue(MotionEvent.AXIS_VSCROLL, vscroll)
                            setAxisValue(MotionEvent.AXIS_HSCROLL, hscroll)
                        }
                    )
                val scrollEvent =
                    MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_SCROLL,
                        1, // pointerCount
                        pointerProperties,
                        pointerCoords,
                        metaState,
                        0, // buttonState
                        1f, // xPrecision
                        1f, // yPrecision
                        0, // deviceId
                        0, // edgeFlags
                        InputDevice.SOURCE_MOUSE,
                        0, // flags
                    )
                uiController.injectMotionEvent(scrollEvent)
            }
        }
    }

    private companion object {
        /** Arbitrary fixed ID for PdfView */
        private const val PDF_VIEW_ID = 123456789
        private const val ZOOM_DIFFERENCE_TOLERANCE = 0.001f
        const val KEYBOARD_VERTICAL_SCROLL_FACTOR = 20
        const val KEYBOARD_HORIZONTAL_SCROLL_FACTOR = 20
        const val MOUSE_VERTICAL_SCROLL_FACTOR = 14
        const val MOUSE_HORIZONTAL_SCROLL_FACTOR = 14
    }
}
