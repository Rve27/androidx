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

package androidx.xr.runtime.testing

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.MaterialResource
import androidx.xr.runtime.internal.RenderingRuntime
import androidx.xr.runtime.internal.SceneRuntime
import androidx.xr.runtime.internal.TextureResource
import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.ListenableFuture

/** Test-only implementation of [SceneRuntime] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeRenderingRuntime(
    private val sceneRuntime: SceneRuntime,
    private val activity: Activity,
) : RenderingRuntime {
    /**
     * For test purposes only.
     *
     * A fake implementation of [MaterialResource] used to simulate a water material within the test
     * environment.
     *
     * <p>Instances of this class are created by [createWaterMaterial] and can be accessed for
     * verification via the [createdWaterMaterials] list. Tests can inspect the public properties of
     * this class (e.g., [reflectionMap], [normalTiling]) to confirm that the code under test
     * correctly configures the material's attributes.
     *
     * @param isAlphaMapVersion The value provided during creation, indicating which version of the
     *   water material was requested.
     */
    public class FakeWaterMaterial(public val isAlphaMapVersion: Boolean) : MaterialResource {
        public var reflectionMap: TextureResource? = null
        public var normalMap: TextureResource? = null
        public var normalTiling: Float = 0.0f
        public var normalSpeed: Float = 0.0f
        public var alphaStepMultiplier: Float = 0.0f
        public var alphaMap: TextureResource? = null
        public var normalZ: Float = 0.0f
        public var normalBoundary: Float = 0.0f
    }

    /**
     * For test purposes only.
     *
     * A list of all [FakeWaterMaterial] instances created via [createWaterMaterial]. Tests can
     * inspect this list to verify the number of materials created and to access their properties
     * for further assertions.
     */
    public val createdWaterMaterials: MutableList<FakeWaterMaterial> =
        mutableListOf<FakeWaterMaterial>()

    @Suppress("AsyncSuffixFuture")
    override fun createWaterMaterial(
        isAlphaMapVersion: Boolean
    ): ListenableFuture<MaterialResource> {
        val newMaterial = FakeWaterMaterial(isAlphaMapVersion)
        createdWaterMaterials.add(newMaterial)
        return immediateFuture(newMaterial)
    }

    override fun destroyWaterMaterial(material: MaterialResource) {
        createdWaterMaterials.remove(material)
    }

    override fun setReflectionMapOnWaterMaterial(
        material: MaterialResource,
        reflectionMap: TextureResource,
    ) {
        (material as? FakeWaterMaterial)?.reflectionMap = reflectionMap
    }

    override fun setNormalMapOnWaterMaterial(
        material: MaterialResource,
        normalMap: TextureResource,
    ) {
        (material as? FakeWaterMaterial)?.normalMap = normalMap
    }

    override fun setNormalTilingOnWaterMaterial(material: MaterialResource, normalTiling: Float) {
        (material as? FakeWaterMaterial)?.normalTiling = normalTiling
    }

    override fun setNormalSpeedOnWaterMaterial(material: MaterialResource, normalSpeed: Float) {
        (material as? FakeWaterMaterial)?.normalSpeed = normalSpeed
    }

    override fun startRenderer() {}

    override fun stopRenderer() {}

    override fun dispose() {}
}
