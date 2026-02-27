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

package androidx.compose.remote.creation.compose.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteMatrix3x3Test {

    @Test
    fun remoteMatrix_cacheKey() {
        val m1 = RemoteMatrix3x3.createIdentity()
        val m2 = RemoteMatrix3x3.createIdentity()
        assertThat(m1.cacheKey).isNotNull()
        assertThat(m1.cacheKey).isEqualTo(m2.cacheKey)

        val rotate1 = RemoteMatrix3x3.createRotate(45f.rf)
        val rotate2 = RemoteMatrix3x3.createRotate(45f.rf)
        assertThat(rotate1.cacheKey).isNotNull()
        assertThat(rotate1.cacheKey).isEqualTo(rotate2.cacheKey)
        assertThat(rotate1.cacheKey).isNotEqualTo(m1.cacheKey)

        val tx1 = RemoteMatrix3x3.createTranslateX(10f.rf)
        assertThat(tx1.cacheKey).isNotNull()
        assertThat(tx1.cacheKey).isNotEqualTo(rotate1.cacheKey)

        val combined = m1 * rotate1
        assertThat(combined.cacheKey).isNotNull()
        assertThat(combined.cacheKey).isNotEqualTo(rotate1.cacheKey)
    }
}
