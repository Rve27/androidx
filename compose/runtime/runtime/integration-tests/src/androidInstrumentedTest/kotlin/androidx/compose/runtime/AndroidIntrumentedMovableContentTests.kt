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

package androidx.compose.runtime

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidIntrumentedMovableContentTests {
    @get:Rule val rule = createComposeRule()

    @Test
    fun boxWithConstraintsAndIndirectContent() {
        var extractContent by mutableStateOf(false)

        rule.setContent {
            val innerState by remember { mutableStateOf(false) }
            var content: (@Composable () -> Unit)? by remember { mutableStateOf(null) }
            val movableContent = remember { movableContentOf { content?.invoke() } }
            BoxWithConstraints {
                if (!extractContent) {
                    content = { BasicText("Movable text: innerState=$innerState, $maxWidth") }
                    movableContent()
                }
            }
            if (extractContent) {
                movableContent()
            }
        }

        rule.runOnIdle { extractContent = true }

        rule.runOnIdle { extractContent = false }

        rule.runOnIdle { extractContent = true }
    }
}
