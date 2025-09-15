/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.Test

internal class NavigationEventHistoryTest {

    @Test
    fun primaryConstructor_withValidData_createsState() {
        val history = NavigationEventHistory(mergedHistory = listOf(infoA, infoB), currentIndex = 1)
        assertThat(history.mergedHistory).containsExactly(infoA, infoB).inOrder()
        assertThat(history.currentIndex).isEqualTo(1)
    }

    @Test
    fun emptyConstructor_createsEmptyState() {
        val history = NavigationEventHistory()
        assertThat(history.mergedHistory).isEmpty()
        assertThat(history.currentIndex).isEqualTo(-1)
    }

    @Test
    fun partitionConstructor_currentOnly_createsCorrectState() {
        val history = NavigationEventHistory(currentInfo = infoA)
        // Stack should be [A], index points to A (index 0)
        assertThat(history.mergedHistory).containsExactly(infoA)
        assertThat(history.currentIndex).isEqualTo(0) // backInfo.size
    }

    @Test
    fun partitionConstructor_currentAndBack_createsCorrectState() {
        val history = NavigationEventHistory(currentInfo = infoB, backInfo = listOf(infoA))
        // Stack should be [A, B], index points to B (index 1)
        assertThat(history.mergedHistory).containsExactly(infoA, infoB).inOrder()
        assertThat(history.currentIndex).isEqualTo(1) // backInfo.size
    }

    @Test
    fun partitionConstructor_currentAndForward_createsCorrectState() {
        val history =
            NavigationEventHistory(currentInfo = infoA, forwardInfo = listOf(infoB, infoC))
        // Stack should be [A, B, C], index points to A (index 0)
        assertThat(history.mergedHistory).containsExactly(infoA, infoB, infoC).inOrder()
        assertThat(history.currentIndex).isEqualTo(0) // backInfo.size (which is 0)
    }

    @Test
    fun partitionConstructor_fullStack_createsCorrectState() {
        val history =
            NavigationEventHistory(
                currentInfo = infoB,
                backInfo = listOf(infoA),
                forwardInfo = listOf(infoC),
            )
        // Stack should be [A, B, C], index points to B (index 1)
        assertThat(history.mergedHistory).containsExactly(infoA, infoB, infoC).inOrder()
        assertThat(history.currentIndex).isEqualTo(1) // backInfo.size
    }

    @Test
    fun init_emptyHistory_withInvalidIndex_throwsException() {
        val e =
            assertThrows<IllegalArgumentException> {
                // Invalid: empty list must have index -1
                NavigationEventHistory(mergedHistory = emptyList(), currentIndex = 0)
            }
        e.hasMessageThat().contains("Invalid 'NavigationEventHistory' state")
        e.hasMessageThat().contains("Received: currentIndex = '0'")
        e.hasMessageThat().contains("bounds = '0..-1'")
    }

    @Test
    fun init_populatedHistory_withIndexNegativeOne_throwsException() {
        val e =
            assertThrows<IllegalArgumentException> {
                // Invalid: populated list must have an index >= 0
                NavigationEventHistory(mergedHistory = listOf(infoA), currentIndex = -1)
            }
        e.hasMessageThat().contains("Invalid 'NavigationEventHistory' state")
        e.hasMessageThat().contains("Received: currentIndex = '-1'")
        e.hasMessageThat().contains("bounds = '0..0'")
    }

    @Test
    fun init_populatedHistory_withIndexOutOfBounds_throwsException() {
        val e =
            assertThrows<IllegalArgumentException> {
                // Invalid: Index 2 is out of bounds for a list of size 2 (valid indices are 0, 1)
                NavigationEventHistory(mergedHistory = listOf(infoA, infoB), currentIndex = 2)
            }

        e.hasMessageThat().contains("Invalid 'NavigationEventHistory' state")
        e.hasMessageThat().contains("Received: currentIndex = '2'")
        e.hasMessageThat().contains("bounds = '0..1'")
    }

    @Test
    fun equals_and_hashCode_contract() {
        val history1 = NavigationEventHistory(listOf(infoA, infoB), 1)
        val history2 = NavigationEventHistory(listOf(infoA, infoB), 1)
        val history3 = NavigationEventHistory(listOf(infoA, infoB), 0) // Diff index
        val history4 = NavigationEventHistory(listOf(infoA), 0) // Diff list

        // Equals
        assertThat(history1).isEqualTo(history2)
        assertThat(history1).isNotEqualTo(history3)
        assertThat(history1).isNotEqualTo(history4)

        // HashCode
        assertThat(history1.hashCode()).isEqualTo(history2.hashCode())
        assertThat(history1.hashCode()).isNotEqualTo(history3.hashCode())
        assertThat(history1.hashCode()).isNotEqualTo(history4.hashCode())
    }

    @Test
    fun toString_containsAllProperties() {
        val history =
            NavigationEventHistory(
                currentInfo = infoB,
                backInfo = listOf(infoA),
                forwardInfo = listOf(infoC),
            )
        val string = history.toString()
        assertThat(string).contains("currentIndex=1")
        assertThat(string)
            .contains("mergedHistory=[TestInfo(id=A), TestInfo(id=B), TestInfo(id=C)]")
    }

    /** A simple [NavigationEventInfo] for testing. */
    private data class TestInfo(val id: String) : NavigationEventInfo

    private companion object {
        private val infoA = TestInfo("A")
        private val infoB = TestInfo("B")
        private val infoC = TestInfo("C")
    }
}
