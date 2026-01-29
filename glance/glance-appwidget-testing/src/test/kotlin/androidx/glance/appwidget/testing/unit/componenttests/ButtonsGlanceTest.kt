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

package androidx.glance.appwidget.testing.unit.componenttests

import androidx.glance.Button
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.layout.Column
import androidx.glance.testing.unit.hasText
import org.junit.Test

class ButtonsGlanceTest {

    @Test
    fun translateButton_hasCorrectText() = runGlanceAppWidgetUnitTest {
        // Set the composable to test
        val buttonText = "Button Text"
        val m3ButtonText = "M3 Btn Text"
        provideComposable {
            Column() {
                FilledButton(text = m3ButtonText, onClick = {})
                Button(buttonText, onClick = {})
            }
        }

        // Perform assertions
        onNode(hasText(m3ButtonText)).assertExists()
        onNode(hasText(buttonText)).assertExists()
    }
}
