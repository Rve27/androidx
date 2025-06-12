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

import android.util.Range
import androidx.annotation.RestrictTo
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.impl.UseCaseType
import androidx.camera.core.featuregroup.impl.UseCaseType.Companion.getFeatureGroupUseCaseType
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/**
 * Represents a session configuration to start a camera session. When used with `camera-lifecycle`,
 * this SessionConfig is expected to be used for starting a camera session (e.g. by being bound to
 * the [androidx.lifecycle.LifecycleOwner] via
 * `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` API which allows the lifecycle
 * events to start and stop the camera session with this given configuration).
 *
 * It consists of a collection of [UseCase], session parameters to be applied on the camera session,
 * and common properties like the field-of-view defined by [ViewPort], the [CameraEffect], frame
 * rate, required or preferred [GroupableFeature] groups etc.
 *
 * @property useCases The list of [UseCase] to be attached to the camera and receive camera data.
 *   This can't be empty.
 * @property viewPort The [ViewPort] to be applied on the camera session. If not set, the default is
 *   no viewport.
 * @property effects The list of [CameraEffect] to be applied on the camera session. If not set, the
 *   default is no effects.
 * @property frameRateRange The desired frame rate range for the camera session. The value must be
 *   one of the supported frame rates queried by [CameraInfo.getSupportedFrameRateRanges] with a
 *   specific [SessionConfig], or an [IllegalArgumentException] will be thrown during
 *   `SessionConfig` binding (i.e. when calling
 *   `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` or
 *   `androidx.camera.lifecycle.LifecycleCameraProvider.bindToLifecycle`). When this value is set,
 *   any target frame rate set on individual [UseCase] will be ignored during `SessionConfig`
 *   binding. If this value is not set, the default is [FRAME_RATE_RANGE_UNSPECIFIED], which means
 *   no specific frame rate. The range defines the acceptable minimum and maximum frame rate for the
 *   camera session. A **dynamic range** (e.g., `[15, 30]`) allows the camera to adjust its frame
 *   rate within the bounds, which will benefit **previewing in low light** by enabling longer
 *   exposures for brighter, less noisy images; conversely, a **fixed range** (e.g., `[30, 30]`)
 *   ensures a stable frame rate crucial for **video recording**, though it can lead to darker,
 *   noisier video in low light due to shorter exposure times.
 * @throws IllegalArgumentException If the combination of config options are conflicting or
 *   unsupported, e.g.
 *     - if any of the required features is not supported on the device
 *     - if same feature is present multiple times in [preferredFeatures]
 *     - if same feature is present in both [requiredFeatures] and [preferredFeatures]
 *     - if [ImageAnalysis] use case is added with [requiredFeatures] or [preferredFeatures]
 *     - if a [CameraEffect] is set with [requiredFeatures] or [preferredFeatures]
 *     - if the frame rate is not supported with the [SessionConfig]
 *
 * @See androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle
 */
@ExperimentalSessionConfig
public open class SessionConfig
@JvmOverloads
constructor(
    useCases: List<UseCase>,
    public val viewPort: ViewPort? = null,
    public val effects: List<CameraEffect> = emptyList(),
    public val frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
) {
    public val useCases: List<UseCase> = useCases.distinct()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var requiredFeatureGroup: Set<GroupableFeature> = emptySet()
        private set

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var preferredFeatureGroup: List<GroupableFeature> = emptyList()
        private set

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public open val isLegacy: Boolean = false
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val sessionType: Int = SESSION_TYPE_REGULAR

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var featureSelectionListener: Consumer<Set<GroupableFeature>> =
        Consumer<Set<GroupableFeature>> {}
        private set

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var featureSelectionListenerExecutor: Executor = CameraXExecutors.mainThreadExecutor()
        private set

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        useCases: List<UseCase>,
        viewPort: ViewPort? = null,
        effects: List<CameraEffect> = emptyList(),
        frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
        requiredFeatureGroup: Set<GroupableFeature> = emptySet(),
        preferredFeatureGroup: List<GroupableFeature> = emptyList(),
    ) : this(
        useCases = useCases,
        viewPort = viewPort,
        effects = effects,
        frameRateRange = frameRateRange,
    ) {
        this.requiredFeatureGroup = requiredFeatureGroup
        this.preferredFeatureGroup = preferredFeatureGroup
        validateFeatureCombination()
    }

    /** Creates the SessionConfig from use cases only. */
    public constructor(vararg useCases: UseCase) : this(useCases.toList())

    private fun validateFeatureCombination() {
        if (requiredFeatureGroup.isEmpty() && preferredFeatureGroup.isEmpty()) {
            return
        }

        // Currently, there is only feature instance possible per type. But this can change in
        // future, e.g. a VIDEO_STABILIZATION feature object may need to be added in future.
        validateRequiredFeatures()

        require(preferredFeatureGroup.distinct().size == preferredFeatureGroup.size) {
            "Duplicate values in preferredFeatures($preferredFeatureGroup)"
        }

        val duplicateFeatures = requiredFeatureGroup.intersect(preferredFeatureGroup)
        require(duplicateFeatures.isEmpty()) {
            "requiredFeatures and preferredFeatures have duplicate values: $duplicateFeatures"
        }

        useCases.forEach {
            require(it.getFeatureGroupUseCaseType() != UseCaseType.UNDEFINED) {
                "$it is not supported with feature group"
            }
        }

        require(effects.isEmpty()) { "Effects aren't supported with feature group yet" }
    }

    /**
     * Validates that there are no conflicting values for the same feature in
     * [requiredFeatureGroup].
     *
     * @throws IllegalArgumentException If there are conflicting values for the same feature.
     */
    private fun validateRequiredFeatures() {
        val requiredFeatureTypes = requiredFeatureGroup.map { it.featureTypeInternal }.distinct()
        requiredFeatureTypes.forEach { featureType ->
            val distinctFeaturesPerType =
                requiredFeatureGroup.filter { it.featureTypeInternal == featureType }

            require(distinctFeaturesPerType.size <= 1) {
                "requiredFeatures has conflicting feature values: $distinctFeaturesPerType"
            }
        }
    }

    /**
     * Sets a listener to know which features are finally selected when a session config is bound,
     * based on the user-defined priorities/ordering for [preferredFeatureGroup] and device
     * capabilities.
     *
     * Both the required and the selected preferred features are notified to the listener. The
     * listener is invoked when this session config is bound to camera (e.g. when the
     * `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` API is invoked).
     *
     * Alternatively, the [CameraInfo.isFeatureGroupSupported] API can be used to query if a set of
     * features is supported before binding.
     *
     * @param executor The executor in which the listener will be invoked, main thread by default.
     * @param listener The consumer to accept the final set of features when they are selected.
     */
    // TODO: b/384404392 - Remove when feature combo impl. is ready. The feature combo params should
    //   be kept restricted until then.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmOverloads
    public fun setFeatureSelectionListener(
        executor: Executor = CameraXExecutors.mainThreadExecutor(),
        listener: Consumer<Set<GroupableFeature>>,
    ) {
        featureSelectionListener = listener
        featureSelectionListenerExecutor = executor
    }

    /** Builder for [SessionConfig] */
    @ExperimentalSessionConfig
    public class Builder(private val useCases: List<UseCase>) {
        private var viewPort: ViewPort? = null
        private var effects: MutableList<CameraEffect> = mutableListOf()
        private var frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED
        private val requiredFeatureGroup = mutableListOf<GroupableFeature>()
        private val preferredFeatureGroup = mutableListOf<GroupableFeature>()

        public constructor(vararg useCases: UseCase) : this(useCases.toList())

        /** Sets the [ViewPort] to be applied on the camera session. */
        public fun setViewPort(viewPort: ViewPort): Builder {
            this.viewPort = viewPort
            return this
        }

        /** Adds a [CameraEffect] to be applied on the camera session. */
        public fun addEffect(effect: CameraEffect): Builder {
            this.effects.add(effect)
            return this
        }

        /**
         * Sets the frame rate range for the camera session.
         *
         * See [SessionConfig.frameRateRange] for more details.
         *
         * @param frameRateRange The frame rate range to be applied on the camera session.
         */
        public fun setFrameRateRange(frameRateRange: Range<Int>): Builder {
            this.frameRateRange = frameRateRange
            return this
        }

        /**
         * Sets the list of [GroupableFeature] that are mandatory for the camera configuration.
         *
         * If all the features are not supported, an [IllegalStateException] will be thrown during
         * camera configuration.
         *
         * @param features The vararg of `GroupableFeature` objects to add to the required features.
         * @return The [Builder] instance, allowing for method chaining.
         * @see androidx.camera.core.SessionConfig.requiredFeatureGroup
         */
        // TODO: b/384404392 - Remove when feature combo impl. is ready.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setRequiredFeatureGroup(vararg features: GroupableFeature): Builder {
            requiredFeatureGroup.addAll(features)
            return this
        }

        /**
         * Sets the list of preferred [GroupableFeature] that is ordered according to priority in
         * descending order.
         *
         * These features will be selected on a best-effort basis according to the priority. The
         * feature that is ordered first in the list (i.e. has a lower index) will be prioritized
         * higher than a feature ordered later in the list.
         *
         * @param features The list of preferred features, ordered by preference.
         * @return The [Builder] instance, allowing for method chaining.
         * @see androidx.camera.core.SessionConfig.preferredFeatureGroup
         */
        // TODO: b/384404392 - Remove when feature combo impl. is ready.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setPreferredFeatureGroup(vararg features: GroupableFeature): Builder {
            preferredFeatureGroup.clear()
            preferredFeatureGroup.addAll(features)
            return this
        }

        /** Builds a [SessionConfig] from the current configuration. */
        public fun build(): SessionConfig {
            return SessionConfig(
                useCases = useCases,
                viewPort = viewPort,
                effects = effects.toList(),
                frameRateRange = frameRateRange,
                requiredFeatureGroup = requiredFeatureGroup.toSet(),
                preferredFeatureGroup = preferredFeatureGroup.toList(),
            )
        }
    }
}

/** The legacy SessionConfig which allows sequential binding. This is used internally. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalSessionConfig::class)
public class LegacySessionConfig(
    useCases: List<UseCase>,
    viewPort: ViewPort? = null,
    effects: List<CameraEffect> = emptyList(),
) : SessionConfig(useCases, viewPort, effects) {
    public override val isLegacy: Boolean = true

    public constructor(
        useCaseGroup: UseCaseGroup
    ) : this(useCaseGroup.useCases, useCaseGroup.viewPort, useCaseGroup.effects)
}
