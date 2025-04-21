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
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED

/**
 * Represents a session configuration to start a camera session. When used with `camera-lifecycle`,
 * this SessionConfig is expected to be bound to the [androidx.lifecycle.LifecycleOwner] via
 * [androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle] which allows the lifecycle
 * events to start and stop the camera session with this given configuration.
 *
 * It consists of a collection of [UseCase], session parameters to be applied on the camera session
 * , and common properties like the field-of-view defined by [ViewPort] and the [CameraEffect].
 *
 * The following shows the example of how to configure the SessionConfig in CameraX.
 *
 * @sample androidx.camera.lifecycle.samples.bindSessionConfigToLifecycle
 * @param useCases The list of [UseCase] to be attached to the camera and receive camera data. This
 *   can't be empty.
 * @param viewPort The [ViewPort] to be applied on the camera session. If not set, the default is no
 *   viewport.
 * @param effects The list of [CameraEffect] to be applied on the camera session. If not set, the
 *   default is no effects.
 * @See androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalSessionConfig
public open class SessionConfig
@JvmOverloads
constructor(
    useCases: List<UseCase>,
    public val viewPort: ViewPort? = null,
    public val effects: List<CameraEffect> = emptyList(),
) {
    public val useCases: List<UseCase> = useCases.distinct()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val isMultipleBindingAllowed: Boolean = false
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val targetHighSpeedFrameRate: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED

    /** Builder for [SessionConfig] */
    public class Builder(private val useCases: List<UseCase>) {
        private var viewPort: ViewPort? = null
        private var effects: MutableList<CameraEffect> = mutableListOf()

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

        /** Builds a [SessionConfig] from the current configuration. */
        public fun build(): SessionConfig {
            return SessionConfig(useCases, viewPort, effects.toList())
        }
    }
}

/** The legacy SessionConfig which allows multiple binding. This is used internally. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalSessionConfig::class)
public class LegacySessionConfig(
    useCases: List<UseCase>,
    viewPort: ViewPort? = null,
    effects: List<CameraEffect> = emptyList(),
    public override val targetHighSpeedFrameRate: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED
) : SessionConfig(useCases, viewPort, effects) {
    public override val isMultipleBindingAllowed: Boolean = true

    public constructor(
        useCaseGroup: UseCaseGroup
    ) : this(
        useCaseGroup.useCases,
        useCaseGroup.viewPort,
        useCaseGroup.effects,
        useCaseGroup.targetHighSpeedFrameRate
    )
}
