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

import androidx.camera.core.CameraInfo
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.UseCase

// TODO: Refactor the documentation to make it simpler once a binding API is added
/**
 * Represents a combination of [Feature]s to configure the camera with.
 *
 * During camera configuration, the [FeatureCombination] will be used to determine which features
 * should finally be configured to the camera based on the device capabilities and user priorities.
 *
 * In case a `Feature` value can be set through multiple APIs (e.g. through this class and also
 * through use case APIs like [Preview.Builder.setTargetFrameRate]), the [FeatureCombination] API
 * should be used to set the feature value. If there are conflicting values set through this API and
 * the [UseCase] APIs, the feature combination values will take precedence.
 *
 * @property requiredFeatures A set of `Feature` that are mandatory for the camera configuration. An
 *   [IllegalStateException] is thrown during camera configuration if adding all of the required
 *   features is not supported on a device. The [CameraInfo.isFeatureCombinationSupported] API can
 *   be used to check if the required features are supported. Alternatively, the features can be
 *   provided through the `orderedPreferredFeatures` property so that some features can be discarded
 *   according to priority if all of them are not supported.
 * @property orderedPreferredFeatures A list of preferred [Feature] that should be ordered according
 *   to priority in descending order, i.e. a `Feature` with a lower index in the list will be
 *   considered to have a higher priority. These features will be selected on a best-effort basis
 *   according to the priority.
 * @throws IllegalArgumentException If there are conflicting values for the same type of feature in
 *   [requiredFeatures] which can never be supported together, or [orderedPreferredFeatures] have
 *   duplicate values.
 * @see CameraInfo.isFeatureCombinationSupported
 */
public class FeatureCombination
@JvmOverloads
constructor(
    public val requiredFeatures: Set<Feature> = emptySet(),
    public val orderedPreferredFeatures: List<Feature> = emptyList(),
) {
    // TODO: May need to extend UseCaseGroup and useCases param later, depends on session config
    //  design

    init {
        // Currently, there is only feature instance possible per type. But this can change in
        // future, e.g. a VIDEO_STABILIZATION feature object may need to be added in future.
        validateRequiredFeatures()
        require(orderedPreferredFeatures.distinct().size == orderedPreferredFeatures.size) {
            "$orderedPreferredFeatures has duplicate values"
        }
    }

    /**
     * Validates that there are no conflicting values for the same feature in [requiredFeatures].
     *
     * @throws IllegalArgumentException If there are conflicting values for the same feature.
     */
    private fun validateRequiredFeatures() {
        val requiredFeatureTypes = requiredFeatures.map { it.featureTypeInternal }.distinct()
        requiredFeatureTypes.forEach { featureType ->
            val distinctFeaturesPerType =
                requiredFeatures.filter { it.featureTypeInternal == featureType }.distinct()

            Logger.d(
                TAG,
                "validateRequiredFeatures: featureType=$featureType," +
                    " distinctFeaturesPerType=$distinctFeaturesPerType"
            )

            require(distinctFeaturesPerType.size <= 1) {
                "requiredFeatures has conflicting feature values: $distinctFeaturesPerType"
            }
        }
    }

    override fun toString(): String {
        return "FeatureCombination(requiredFeatures=$requiredFeatures" +
            ",preferredFeatures=$orderedPreferredFeatures"
    }

    /** The `Builder` class to construct a [FeatureCombination] instance. */
    public class Builder {
        private val requiredFeatures = mutableListOf<Feature>()
        private val orderedPreferredFeatures = mutableListOf<Feature>()

        /**
         * Sets a list of features that are mandatory for the camera configuration.
         *
         * If all the features are not supported, an [IllegalStateException] will be thrown during
         * camera configuration.
         *
         * @param features The vararg of `Feature` objects to add to the required features.
         * @return The [Builder] instance, allowing for method chaining.
         * @see FeatureCombination.requiredFeatures
         */
        public fun setRequiredFeatures(vararg features: Feature): Builder {
            requiredFeatures.clear()
            requiredFeatures.addAll(features)
            return this
        }

        /**
         * Sets the ordered list of preferred features.
         *
         * These features will be selected on a best-effort basis according to the priority. The
         * feature that is ordered first in the list (i.e. has a lower index) will be prioritized
         * higher than a feature ordered later in the list.
         *
         * @param features The list of preferred features, ordered by preference.
         * @return The [Builder] instance, allowing for method chaining.
         */
        public fun setOrderedPreferredFeatures(vararg features: Feature): Builder {
            orderedPreferredFeatures.clear()
            orderedPreferredFeatures.addAll(features)
            return this
        }

        /** Builds a [FeatureCombination] instance with the current builder configuration. */
        public fun build(): FeatureCombination {
            return FeatureCombination(
                requiredFeatures = requiredFeatures.toSet(),
                orderedPreferredFeatures = orderedPreferredFeatures.toList(),
            )
        }
    }

    private companion object {
        private const val TAG = "FeatureCombination"
    }
}
