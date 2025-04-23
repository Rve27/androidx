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

package androidx.camera.core.featurecombination.impl

import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.featurecombination.ExperimentalFeatureCombination
import androidx.camera.core.featurecombination.Feature
import androidx.camera.core.featurecombination.Feature.Companion.HDR_HLG10
import androidx.camera.core.featurecombination.Feature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featurecombination.Feature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featurecombination.impl.ResolvedFeatureCombination.Companion.findResolvedFeatureCombination
import androidx.camera.core.featurecombination.impl.UseCaseType.IMAGE_CAPTURE
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Supported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Unsupported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UnsupportedUseCase
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UseCaseMissing
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolver
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalFeatureCombination::class)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class ResolvedFeatureCombinationTest {
    private val fakeCameraInfo = FakeCameraInfoInternal()

    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()

    @Test
    fun findResolvedFeatureCombination_resultIsSupported_returnsResolvedFeatureCombination() {
        // Arrange: Create a resolver returning a ResolvedFeatureCombination without all the
        // preferred features
        val useCases = setOf(preview, imageCapture)
        val orderedPreferredFeatures = listOf(HDR_HLG10, IMAGE_ULTRA_HDR)
        val expectedResolvedFeatureCombination =
            ResolvedFeatureCombination(useCases, setOf(HDR_HLG10))
        val resolver =
            object : FeatureCombinationResolver {
                override fun resolveFeatureCombination(
                    useCases: Set<UseCase>,
                    requiredFeatures: Set<Feature>,
                    orderedPreferredFeatures: List<Feature>
                ): FeatureCombinationResolutionResult {
                    return Supported(expectedResolvedFeatureCombination)
                }
            }

        // Act
        val resolvedCombination =
            findResolvedFeatureCombination(
                fakeCameraInfo,
                useCases,
                orderedPreferredFeatures = orderedPreferredFeatures,
                resolver = resolver
            )

        // Assert
        assertThat(resolvedCombination).isEqualTo(expectedResolvedFeatureCombination)
    }

    @Test
    fun findResolvedFeatureCombination_resultIsUnsupported_throwsException() {
        // Arrange: Create a resolver returning Unsupported result
        val useCases = setOf(preview, imageCapture)
        val orderedPreferredFeatures = listOf(HDR_HLG10, IMAGE_ULTRA_HDR)
        val resolver =
            object : FeatureCombinationResolver {
                override fun resolveFeatureCombination(
                    useCases: Set<UseCase>,
                    requiredFeatures: Set<Feature>,
                    orderedPreferredFeatures: List<Feature>
                ): FeatureCombinationResolutionResult {
                    return Unsupported
                }
            }

        // Act & assert
        assertThrows<IllegalArgumentException> {
            findResolvedFeatureCombination(
                fakeCameraInfo,
                useCases,
                orderedPreferredFeatures = orderedPreferredFeatures,
                resolver = resolver
            )
        }
    }

    @Test
    fun findResolvedFeatureCombination_useCaseMissingForSomeFeature_throwsException() {
        // Arrange: Create a resolver returning UseCaseMissing result
        val useCases = setOf(preview)
        val requiredFeatures = setOf(IMAGE_ULTRA_HDR) // but no ImageCapture
        val orderedPreferredFeatures = listOf(HDR_HLG10, PREVIEW_STABILIZATION)
        val resolver =
            object : FeatureCombinationResolver {
                override fun resolveFeatureCombination(
                    useCases: Set<UseCase>,
                    requiredFeatures: Set<Feature>,
                    orderedPreferredFeatures: List<Feature>
                ): FeatureCombinationResolutionResult {
                    return UseCaseMissing(
                        requiredUseCases = IMAGE_CAPTURE.toString(),
                        featureRequiring = IMAGE_ULTRA_HDR
                    )
                }
            }

        // Act & assert
        assertThrows<IllegalArgumentException> {
            findResolvedFeatureCombination(
                fakeCameraInfo,
                useCases,
                requiredFeatures = requiredFeatures,
                orderedPreferredFeatures = orderedPreferredFeatures,
                resolver = resolver
            )
        }
    }

    @Test
    fun findResolvedFeatureCombination_useCaseUnsupported_throwsException() {
        // Arrange: Create a resolver returning UnsupportedUseCase result
        val unsupportedUseCase = FakeUseCase()
        val requiredFeatures = setOf(IMAGE_ULTRA_HDR) // but no ImageCapture
        val orderedPreferredFeatures = listOf(HDR_HLG10, PREVIEW_STABILIZATION)
        val resolver =
            object : FeatureCombinationResolver {
                override fun resolveFeatureCombination(
                    useCases: Set<UseCase>,
                    requiredFeatures: Set<Feature>,
                    orderedPreferredFeatures: List<Feature>
                ): FeatureCombinationResolutionResult {
                    return UnsupportedUseCase(unsupportedUseCase)
                }
            }

        // Act & assert
        assertThrows<IllegalArgumentException> {
            findResolvedFeatureCombination(
                fakeCameraInfo,
                setOf(unsupportedUseCase),
                requiredFeatures = requiredFeatures,
                orderedPreferredFeatures = orderedPreferredFeatures,
                resolver = resolver
            )
        }
    }
}
