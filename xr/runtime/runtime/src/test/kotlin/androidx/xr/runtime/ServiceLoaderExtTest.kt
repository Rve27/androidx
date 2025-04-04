/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime

import androidx.xr.runtime.internal.JxrPlatformAdapterFactory
import androidx.xr.runtime.internal.RuntimeFactory
import androidx.xr.runtime.testing.AnotherFakeStateExtender
import androidx.xr.runtime.testing.FakeJxrPlatformAdapterFactory
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.runtime.testing.FakeStateExtender
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ServiceLoaderExtTest {

    @Test
    fun fastServiceLoad_loadsServices() {
        assertThat(
                fastServiceLoad(
                        RuntimeFactory::class.java,
                        listOf(FakeRuntimeFactory::class.java.name)
                    )
                    .single()
            )
            .isInstanceOf(FakeRuntimeFactory::class.java)
        assertThat(
                fastServiceLoad(
                        JxrPlatformAdapterFactory::class.java,
                        listOf(FakeJxrPlatformAdapterFactory::class.java.name),
                    )
                    .single()
            )
            .isInstanceOf(FakeJxrPlatformAdapterFactory::class.java)
        assertThat(
                fastServiceLoad(
                        StateExtender::class.java,
                        listOf(FakeStateExtender::class.java.name)
                    )
                    .iterator()
                    .next()
            )
            .isInstanceOf(FakeStateExtender::class.java)
    }

    @Test
    fun fastServiceLoad_combinesFastAndLoaderServices() {
        val stateExtenders =
            fastServiceLoad(StateExtender::class.java, listOf(FakeStateExtender::class.java.name))
        assertThat(stateExtenders.size).isEqualTo(1)

        for (stateExtender in stateExtenders) {
            assert(stateExtender is FakeStateExtender || stateExtender is AnotherFakeStateExtender)
        }
    }
}
