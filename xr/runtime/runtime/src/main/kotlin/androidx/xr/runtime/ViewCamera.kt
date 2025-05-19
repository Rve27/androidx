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

package androidx.xr.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose

/**
 * Wraps the view camera data (pose and FOV). Represents a virtual camera used for rendering content
 * (in contrast to a physical camera used for tracking).
 *
 * @property pose The pose of the view camera.
 * @property fov The field of view of the view camera.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ViewCamera constructor(public val pose: Pose, public val fov: FieldOfView) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewCamera) return false

        if (pose != other.pose) return false
        if (fov != other.fov) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pose.hashCode()
        result = 31 * result + fov.hashCode()
        return result
    }

    override fun toString(): String = "ViewCamera{\n\tpose=$pose\n\tfov=$fov\n}"
}
