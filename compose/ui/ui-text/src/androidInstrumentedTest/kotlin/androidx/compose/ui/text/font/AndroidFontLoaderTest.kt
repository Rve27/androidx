/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidFontResourceLoaderTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun sampleFont_loadsFont() {
        val loader = AndroidFontLoader(context)
        val typeface =
            loader.loadBlocking(
                Font(
                    resId = R.font.sample_font,
                    weight = FontWeight.Normal,
                    style = FontStyle.Normal
                )
            )
        assertThat(typeface).isNotNull()
        assertThat(typeface).isNotSameInstanceAs(Typeface.DEFAULT)
    }

    @Test
    fun whenBadRes_loadFonts_returnsDefault() {
        val loader = AndroidFontLoader(context)
        val typeface =
            loader.loadBlocking(
                Font(resId = 0, weight = FontWeight.Normal, style = FontStyle.Normal)
            )
        assertThat(typeface).isSameInstanceAs(Typeface.DEFAULT)
    }
}
