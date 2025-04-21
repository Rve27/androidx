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

package androidx.camera.core.featurecombination

import androidx.camera.core.DynamicRange
import androidx.camera.core.featurecombination.Feature.Companion.FPS_60
import androidx.camera.core.featurecombination.Feature.Companion.HDR_HLG10
import androidx.camera.core.featurecombination.Feature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featurecombination.Feature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featurecombination.impl.feature.FeatureTypeInternal
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class FeatureCombinationTest {
    @Test
    fun constructor_conflictingRequiredFeatures_throwsIllegalArgumentExceptionWithCorrectMessage() {
        // Arrange
        val requiredFeatures =
            setOf(
                FPS_60,
                FakeDynamicRangeFeature(DynamicRange.SDR),
                FakeDynamicRangeFeature(DynamicRange.HLG_10_BIT)
            )

        // Act & assert
        val exception =
            try {
                FeatureCombination(requiredFeatures = requiredFeatures)
                null
            } catch (e: Exception) {
                e
            }

        // Assert
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception).hasMessageThat().contains("{encoding=SDR, bitDepth=8}")
        assertThat(exception).hasMessageThat().contains("{encoding=HLG, bitDepth=10}")
    }

    @Test
    fun constructor_noConflictingRequiredFeatures_noExceptionThrown() {
        // Arrange
        val requiredFeatures = setOf(FPS_60, IMAGE_ULTRA_HDR, PREVIEW_STABILIZATION)

        // Act
        val exception =
            try {
                FeatureCombination(requiredFeatures = requiredFeatures)
                null
            } catch (e: Exception) {
                e
            }

        // Assert
        assertThat(exception).isNull()
    }

    @Test
    fun constructor_sameFeatureTwiceInPreferredFeatures_exceptionThrown() {
        // Arrange
        val features = listOf(FPS_60, IMAGE_ULTRA_HDR, PREVIEW_STABILIZATION, IMAGE_ULTRA_HDR)

        // Act & assert
        assertThrows<IllegalArgumentException> {
            FeatureCombination(orderedPreferredFeatures = features)
        }
    }

    @Test
    fun builder_setRequiredFeatures_featuresAreAdded() {
        // Arrange
        val builder = FeatureCombination.Builder()
        val featuresToAdd = listOf(HDR_HLG10, FPS_60)

        // Act
        builder.setRequiredFeatures(*featuresToAdd.toTypedArray())
        val featureCombination = builder.build()

        // Assert
        assertThat(featureCombination.requiredFeatures).containsExactlyElementsIn(featuresToAdd)
    }

    @Test
    fun builder_setOrderedPreferredFeatures_featuresAreSet() {
        // Arrange
        val builder = FeatureCombination.Builder()
        val featuresToSet = listOf(HDR_HLG10, FPS_60)

        // Act
        builder.setOrderedPreferredFeatures(*featuresToSet.toTypedArray())
        val featureCombination = builder.build()

        // Assert
        assertThat(featureCombination.orderedPreferredFeatures)
            .containsExactlyElementsIn(featuresToSet)
            .inOrder()
    }

    @Test
    fun builder_modifyAfterBuild_builtInstanceDoesNotChange() {
        // Arrange
        val builder = FeatureCombination.Builder()

        // Act
        builder.setRequiredFeatures(HDR_HLG10)
        builder.setOrderedPreferredFeatures(FPS_60)
        val featureCombination = builder.build()

        builder.setRequiredFeatures(FPS_60)
        builder.setOrderedPreferredFeatures(HDR_HLG10)

        // Assert
        assertThat(featureCombination.requiredFeatures).containsExactly(HDR_HLG10)
        assertThat(featureCombination.orderedPreferredFeatures).containsExactly(FPS_60)
    }

    data class FakeDynamicRangeFeature(private val dynamicRange: DynamicRange) : Feature() {
        override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.DYNAMIC_RANGE
    }
}
