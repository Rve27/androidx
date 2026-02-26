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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleBufferTest {

    @Test
    fun basicAddAndGet() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style1", 0, 10)
        buffer.addStyle("style2", 5, 15)

        assertThat(buffer.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("style1", 0, 10),
                AnnotatedString.Range("style2", 5, 15),
            )
            .inOrder()
    }

    @Test
    fun removeStyle() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", 0, 10)
        buffer.removeStyle("a", 0, 10)
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun orderIsPreserved() {
        val buffer = TextStyleBuffer<Int>()
        buffer.addStyle(1, 10, 20)
        buffer.addStyle(2, 0, 30)
        buffer.addStyle(3, 5, 15)

        // Verifies that styles are returned in insertion order, regardless of their ranges.
        assertThat(buffer.getStyles(0, 30).map { it.item }).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun equalsAndHashCode() {
        val buffer1 =
            TextStyleBuffer<String>().apply {
                addStyle("a", 0, 10)
                addStyle("b", 10, 20)
            }
        val buffer2 =
            TextStyleBuffer<String>().apply {
                addStyle("a", 0, 10)
                addStyle("b", 10, 20)
            }
        val buffer3 =
            TextStyleBuffer<String>().apply {
                addStyle("b", 10, 20)
                addStyle("a", 0, 10)
            }

        assertThat(buffer1).isEqualTo(buffer2)
        assertThat(buffer1.hashCode()).isEqualTo(buffer2.hashCode())

        // Order matters for equality because it affects the rendered result.
        assertThat(buffer1).isNotEqualTo(buffer3)
    }

    @Test
    fun copy_isEqualAndIndependent() {
        val original = TextStyleBuffer<String>().apply { addStyle("a", 0, 10) }

        val copy = TextStyleBuffer(original)
        assertThat(copy).isEqualTo(original)

        copy.addStyle("b", 10, 20)
        assertThat(copy).isNotEqualTo(original)
        assertThat(original.getAllStyles()).hasSize(1)
        assertThat(copy.getAllStyles()).hasSize(2)
    }

    @Test
    fun clear() {
        val buffer = TextStyleBuffer<String>().apply { addStyle("a", 0, 10) }
        buffer.clear()
        assertThat(buffer.getAllStyles()).isEmpty()
    }
}
