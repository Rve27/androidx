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

package androidx.camera.core

import android.os.Build
import android.util.Range
import android.util.Rational
import android.view.Surface
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalSessionConfig::class)
class SessionConfigTest {
    val useCases = listOf(Preview.Builder().build(), ImageCapture.Builder().build())
    val viewPort = ViewPort.Builder(Rational(4, 3), Surface.ROTATION_0).build()
    val effects =
        listOf(FakeSurfaceEffect(directExecutor(), FakeSurfaceProcessor(directExecutor())))

    @Test
    fun sessionConfig_constructorInitializesFields() {
        val sessionConfig = SessionConfig(useCases, viewPort, effects)

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).isEqualTo(effects)
        assertThat(sessionConfig.targetHighSpeedFrameRate).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(sessionConfig.isMultipleBindingAllowed).isFalse()
    }

    @Test
    fun sessionConfig_defaultConstructorInitializesWithDefaults() {
        val sessionConfig = SessionConfig(useCases)

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isNull()
        assertThat(sessionConfig.effects).isEmpty()
        assertThat(sessionConfig.targetHighSpeedFrameRate).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(sessionConfig.isMultipleBindingAllowed).isFalse()
    }

    @Test
    fun sessionConfig_builderSetsViewPort() {
        val sessionConfig = SessionConfig.Builder(useCases).setViewPort(viewPort).build()

        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
    }

    @Test
    fun sessionConfig_builderAddsEffect() {
        val effect1 = mock(CameraEffect::class.java)
        val effect2 = mock(CameraEffect::class.java)
        val sessionConfig =
            SessionConfig.Builder(useCases).addEffect(effect1).addEffect(effect2).build()

        assertThat(sessionConfig.effects).containsExactly(effect1, effect2)
    }

    @Test
    fun sessionConfig_builderBuildsCorrectSessionConfig() {
        val effect = mock(CameraEffect::class.java)
        val sessionConfig =
            SessionConfig.Builder(useCases).setViewPort(viewPort).addEffect(effect).build()

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).containsExactly(effect)
        assertThat(sessionConfig.targetHighSpeedFrameRate).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(sessionConfig.isMultipleBindingAllowed).isFalse()
    }

    @Test
    fun sessionConfig_builderBuildsCorrectSessionConfigWithVarargUseCase() {
        val effect = mock(CameraEffect::class.java)
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val sessionConfig =
            SessionConfig.Builder(preview, imageCapture, imageAnalysis)
                .setViewPort(viewPort)
                .addEffect(effect)
                .build()

        assertThat(sessionConfig.useCases).containsExactly(preview, imageCapture, imageAnalysis)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).containsExactly(effect)
        assertThat(sessionConfig.targetHighSpeedFrameRate).isEqualTo(FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(sessionConfig.isMultipleBindingAllowed).isFalse()
    }

    @Test
    fun sessionConfig_notAffectedByBuilderAfterBuilt() {
        val mutableUseCasesList = useCases.toMutableList()
        val effect1 = mock(CameraEffect::class.java)
        val effect2 = mock(CameraEffect::class.java)
        val viewPort2 = ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()

        val builder =
            SessionConfig.Builder(mutableUseCasesList).setViewPort(viewPort).addEffect(effect1)

        val sessionConfig = builder.build()
        builder.addEffect(effect2)
        builder.setViewPort(viewPort2)
        mutableUseCasesList.add(ImageAnalysis.Builder().build())

        assertThat(sessionConfig.useCases).isEqualTo(useCases)
        assertThat(sessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(sessionConfig.effects).containsExactly(effect1)
    }

    @Test
    fun sessionConfig_useCasesRemovedDuplicatesAndKeepOrder() {
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val useCases1 = listOf(imageCapture, preview, preview, imageCapture)
        val useCases2 = listOf(imageCapture, preview, preview, imageCapture)

        val sessionConfigViaBuilder = SessionConfig.Builder(useCases1).build()
        val sessionConfigViaConstructor = SessionConfig(useCases2)
        assertThat(sessionConfigViaBuilder.useCases)
            .containsExactly(imageCapture, preview)
            .inOrder()
        assertThat(sessionConfigViaConstructor.useCases)
            .containsExactly(imageCapture, preview)
            .inOrder()
    }

    @Test
    fun legacySessionConfig_constructorInitializesFields() {
        val frameRateRange = Range(30, 60)

        val legacySessionConfig = LegacySessionConfig(useCases, viewPort, effects, frameRateRange)

        assertThat(legacySessionConfig.useCases).isEqualTo(useCases)
        assertThat(legacySessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(legacySessionConfig.effects).isEqualTo(effects)
        assertThat(legacySessionConfig.targetHighSpeedFrameRate).isEqualTo(frameRateRange)
        assertThat(legacySessionConfig.isMultipleBindingAllowed).isTrue()
    }

    @Test
    fun legacySessionConfig_defaultConstructorInitializesWithDefaults() {
        val legacySessionConfig = LegacySessionConfig(useCases)

        assertThat(legacySessionConfig.useCases).isEqualTo(useCases)
        assertThat(legacySessionConfig.viewPort).isNull()
        assertThat(legacySessionConfig.effects).isEmpty()
        assertThat(legacySessionConfig.targetHighSpeedFrameRate)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(legacySessionConfig.isMultipleBindingAllowed).isTrue()
    }

    @Test
    fun legacySessionConfig_secondaryConstructorInitializesFromUseCaseGroup() {
        val useCaseGroup =
            UseCaseGroup.Builder()
                .apply {
                    useCases.forEach { addUseCase(it) }
                    setViewPort(viewPort)
                    effects.forEach { addEffect(it) }
                }
                .build()

        val legacySessionConfig = LegacySessionConfig(useCaseGroup)

        assertThat(legacySessionConfig.useCases).isEqualTo(useCases)
        assertThat(legacySessionConfig.viewPort).isEqualTo(viewPort)
        assertThat(legacySessionConfig.effects).isEqualTo(effects)
        assertThat(legacySessionConfig.targetHighSpeedFrameRate)
            .isEqualTo(useCaseGroup.targetHighSpeedFrameRate)
        assertThat(legacySessionConfig.isMultipleBindingAllowed).isTrue()
    }
}
