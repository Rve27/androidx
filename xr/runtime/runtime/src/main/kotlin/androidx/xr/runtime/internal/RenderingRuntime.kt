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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Matrix3
import androidx.xr.runtime.math.Vector4
import com.google.common.util.concurrent.ListenableFuture

/**
 * RenderingRuntime encapsulates all the platform-specific rendering-related operations. Its
 * responsibilities include toggle the render loop, loading assets, and creating renderable scene
 * entities.
 *
 * It is designed to work in tandem with a [SceneRuntime], which manages the scene graph. An
 * instance of `RenderingRuntime` is always created for a specific [SceneRuntime], and both are
 * expected to operate within the same context and lifecycle.
 *
 * This API is not intended to be used by applications.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface RenderingRuntime {
    /**
     * Creates a water material by querying it from the system's built-in materials. The future
     * returned by this method will fire listeners on the UI thread if Runnable::run is supplied.
     *
     * @param isAlphaMapVersion True if the water material should be the alpha map version.
     * @return A ListenableFuture containing a WaterMaterial backed by an imp::WaterMaterial. The
     *   WaterMaterial can be destroyed by passing it to destroyNativeObject.
     */
    @Suppress("AsyncSuffixFuture")
    public fun createWaterMaterial(isAlphaMapVersion: Boolean): ListenableFuture<MaterialResource>

    /** Destroys the given water material resource. */
    public fun destroyWaterMaterial(material: MaterialResource)

    /**
     * Sets the reflection map texture for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param reflectionMap The handle of the texture to be used as the reflection map.
     */
    public fun setReflectionMapOnWaterMaterial(
        material: MaterialResource,
        reflectionMap: TextureResource,
    )

    /**
     * Sets the normal map texture for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalMap The handle of the texture to be used as the normal map.
     */
    public fun setNormalMapOnWaterMaterial(material: MaterialResource, normalMap: TextureResource)

    /**
     * Sets the normal tiling for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalTiling The tiling to use for the normal map.
     */
    public fun setNormalTilingOnWaterMaterial(material: MaterialResource, normalTiling: Float)

    /**
     * Sets the normal speed for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalSpeed The speed to use for the normal map.
     */
    public fun setNormalSpeedOnWaterMaterial(material: MaterialResource, normalSpeed: Float)

    /**
     * Sets the alpha step multiplier for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param alphaStepMultiplier The alpha step multiplier to use for the water material.
     */
    public fun setAlphaStepMultiplierOnWaterMaterial(
        material: MaterialResource,
        alphaStepMultiplier: Float,
    )

    /**
     * Sets the alpha map for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param alphaMap The handle of the texture to be used as the alpha map.
     */
    public fun setAlphaMapOnWaterMaterial(material: MaterialResource, alphaMap: TextureResource)

    /**
     * Sets the normal z for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalZ The normal z to use for the water material.
     */
    public fun setNormalZOnWaterMaterial(material: MaterialResource, normalZ: Float)

    /**
     * Sets the normal boundary for the water material.
     *
     * @param material The handle of the water material to be updated.
     * @param normalBoundary The normal boundary to use for the water material.
     */
    public fun setNormalBoundaryOnWaterMaterial(material: MaterialResource, normalBoundary: Float)

    /**
     * Creates a Khronos PBR material by querying it from the system's built-in materials. The
     * future returned by this method will fire listeners on the UI thread if Runnable::run is
     * supplied.
     */
    public fun createKhronosPbrMaterial(
        spec: KhronosPbrMaterialSpec
    ): ListenableFuture<MaterialResource>?

    /**
     * Destroys the given Khronos PBR material resource.
     *
     * @param material The KhronosPbrMaterial to destroy.
     */
    public fun destroyKhronosPbrMaterial(material: MaterialResource)

    /**
     * Sets the base color texture for the Khronos PBR material. This texture defines the albedo or
     * diffuse color of the material.
     *
     * @param material The handle of the Khronos PBR material.
     * @param baseColor The handle of the base color texture.
     */
    public fun setBaseColorTextureOnKhronosPbrMaterial(
        material: MaterialResource,
        baseColor: TextureResource,
    )

    /**
     * Sets the UV transformation matrix for the base color texture. This allows for scaling,
     * rotating, and translating the texture coordinates.
     *
     * @param material The handle of the Khronos PBR material.
     * @param uvTransform The uv coordinates of the transform stored in a matrix.
     */
    public fun setBaseColorUvTransformOnKhronosPbrMaterial(
        material: MaterialResource,
        uvTransform: Matrix3,
    )

    /**
     * Sets the base color factors for the Khronos PBR material. These factors multiplies the base
     * color texture or defines a uniform base color.
     *
     * @param material The handle of the Khronos PBR material.
     * @param factors The base colors on the Khronos PBR material.
     */
    public fun setBaseColorFactorsOnKhronosPbrMaterial(material: MaterialResource, factors: Vector4)

    /** Starts the renderer. */
    public fun startRenderer()

    /** Stops the renderer. */
    public fun stopRenderer()

    /** Disposes of the resources used by this runtime. */
    public fun dispose()
}
