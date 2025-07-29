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

package androidx.xr.scenecore.spatial.rendering;

import android.app.Activity;
import android.os.Looper;

import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.runtime.internal.MaterialResource;
import androidx.xr.runtime.internal.RenderingRuntime;
import androidx.xr.runtime.internal.SceneRuntime;
import androidx.xr.runtime.internal.TextureResource;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.ImpressApi;
import androidx.xr.scenecore.impl.impress.ImpressApiImpl;
import androidx.xr.scenecore.impl.impress.WaterMaterial;

import com.android.extensions.xr.XrExtensions;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngine;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of [RenderingRuntime] for devices that support the [Feature.SPATIAL] system
 * feature.
 */
class SpatialRenderingRuntime implements RenderingRuntime {
    private static final String SPLIT_ENGINE_LIBRARY_NAME = "impress_api_jni";

    private final @NonNull SceneRuntime mSceneRuntime;
    private @Nullable Activity mActivity;

    private final XrExtensions mExtensions;
    private final ImpressApi mImpressApi;
    private SplitEngineSubspaceManager mSplitEngineSubspaceManager;
    private ImpSplitEngineRenderer mSplitEngineRenderer;
    private boolean mIsDisposed = false;
    private boolean mFrameLoopStarted;

    private SpatialRenderingRuntime(
            @NonNull SceneRuntime sceneRuntime,
            @NonNull Activity activity,
            @NonNull XrExtensions extensions,
            @NonNull ImpressApi impressApi,
            @NonNull SplitEngineSubspaceManager subspaceManager,
            @NonNull ImpSplitEngineRenderer renderer) {
        mSceneRuntime = sceneRuntime;
        mActivity = activity;
        mExtensions = extensions;
        mImpressApi = impressApi;
        mSplitEngineRenderer = renderer;
        mSplitEngineSubspaceManager = subspaceManager;

        startRenderer();
    }

    /** Create a new @c RenderingRuntime. */
    @VisibleForTesting
    static @NonNull SpatialRenderingRuntime create(
            @NonNull SceneRuntime sceneRuntime,
            @NonNull Activity activity,
            @Nullable ImpressApi impressApi,
            @Nullable SplitEngineSubspaceManager splitEngineSubspaceManager,
            @Nullable ImpSplitEngineRenderer splitEngineRenderer) {
        XrExtensions extensions = XrExtensionsProvider.getXrExtensions();
        if (extensions == null) throw new IllegalStateException("XrExtensions is null");
        if (impressApi == null) impressApi = new ImpressApiImpl();
        if (splitEngineRenderer == null) {
            ImpSplitEngine.SplitEngineSetupParams impApiSetupParams =
                    new ImpSplitEngine.SplitEngineSetupParams();
            impApiSetupParams.jniLibraryName = SPLIT_ENGINE_LIBRARY_NAME;
            splitEngineRenderer =
                    ImpSplitEngineRenderer.create(activity, impApiSetupParams, extensions);
        }
        if (splitEngineSubspaceManager == null) {
            splitEngineSubspaceManager =
                    new SplitEngineSubspaceManager(
                            splitEngineRenderer, extensions, null, null, SPLIT_ENGINE_LIBRARY_NAME);
        }
        impressApi.setup(splitEngineRenderer.getView());
        return new SpatialRenderingRuntime(
                sceneRuntime,
                activity,
                extensions,
                impressApi,
                splitEngineSubspaceManager,
                splitEngineRenderer);
    }

    /**
     * Create a new @c SpatialRenderingRuntime.
     *
     * @param sceneRuntime The SceneRuntime provide basic function for creating entities.
     * @param activity The Activity to use.
     * @return A new SpatialRenderingRuntime.
     */
    static @NonNull SpatialRenderingRuntime create(
            @NonNull SceneRuntime sceneRuntime, @NonNull Activity activity) {
        return SpatialRenderingRuntime.create(sceneRuntime, activity, null, null, null);
    }

    private static MaterialResourceImpl getMaterialResourceFromToken(long token) {
        return new MaterialResourceImpl(token);
    }

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    @Override
    public @NonNull ListenableFuture<MaterialResource> createWaterMaterial(
            boolean isAlphaMapVersion) {
        ResolvableFuture<MaterialResource> materialResourceFuture = ResolvableFuture.create();
        // TODO:b/374216912 - Consider calling setFuture() here to catch if the application calls
        // cancel() on the return value from this function, so we can propagate the cancelation
        // message to the Impress API.

        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("This method must be called on the main thread.");
        }

        ListenableFuture<WaterMaterial> materialFuture;
        materialFuture = mImpressApi.createWaterMaterial(isAlphaMapVersion);

        materialFuture.addListener(
                () -> {
                    try {
                        WaterMaterial material = materialFuture.get();
                        materialResourceFuture.set(
                                getMaterialResourceFromToken(material.getNativeHandle()));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        materialResourceFuture.setException(e);
                    }
                },
                // It's convenient for the main application for us to dispatch their listeners on
                // the main thread, because they are required to call back to Impress from there,
                // and it's likely that they will want to call back into the SDK to create entities
                // from within a listener. We defensively post to the main thread here, but in
                // practice this should not cause a thread hop because the Impress API already
                // dispatches its callbacks to the main thread.
                mActivity::runOnUiThread);
        return materialResourceFuture;
    }

    @Override
    public void destroyWaterMaterial(@NonNull MaterialResource material) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.destroyNativeObject(((MaterialResourceImpl) material).getMaterialToken());
    }

    @Override
    public void setReflectionMapOnWaterMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource reflectionMap) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(reflectionMap instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setReflectionMapOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) reflectionMap).getTextureToken());
    }

    @Override
    public void setNormalMapOnWaterMaterial(
            @NonNull MaterialResource material, @NonNull TextureResource normalMap) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        if (!(normalMap instanceof TextureResourceImpl)) {
            throw new IllegalArgumentException("TextureResource is not a TextureResourceImpl");
        }
        mImpressApi.setNormalMapOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(),
                ((TextureResourceImpl) normalMap).getTextureToken());
    }

    @Override
    public void setNormalTilingOnWaterMaterial(
            @NonNull MaterialResource material, float normalTiling) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setNormalTilingOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), normalTiling);
    }

    @Override
    public void setNormalSpeedOnWaterMaterial(
            @NonNull MaterialResource material, float normalSpeed) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setNormalSpeedOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), normalSpeed);
    }

    @Override
    public void setAlphaStepMultiplierOnWaterMaterial(
            @NonNull MaterialResource material, float alphaStepMultiplier) {
        if (!(material instanceof MaterialResourceImpl)) {
            throw new IllegalArgumentException("MaterialResource is not a MaterialResourceImpl");
        }
        mImpressApi.setAlphaStepMultiplierOnWaterMaterial(
                ((MaterialResourceImpl) material).getMaterialToken(), alphaStepMultiplier);
    }

    @Override
    public void startRenderer() {
        if (mSplitEngineRenderer == null || mFrameLoopStarted) {
            return;
        }
        mFrameLoopStarted = true;
        mSplitEngineRenderer.startFrameLoop();
    }

    @Override
    public void stopRenderer() {
        if (mSplitEngineRenderer == null || !mFrameLoopStarted) {
            return;
        }
        mFrameLoopStarted = false;
        mSplitEngineRenderer.stopFrameLoop();
    }

    @Override
    public void dispose() {
        if (mIsDisposed) {
            return;
        }
        stopRenderer();
        mImpressApi.clearPreferredEnvironmentIblAsset();
        mImpressApi.disposeAllResources();

        mActivity = null;
        if (mSplitEngineRenderer != null && mSplitEngineSubspaceManager != null) {
            mSplitEngineSubspaceManager.destroy();
            mSplitEngineRenderer.destroy();
            mSplitEngineSubspaceManager = null;
            mSplitEngineRenderer = null;
        }
        mIsDisposed = true;
    }

    @VisibleForTesting
    boolean isFrameLoopStarted() {
        return mFrameLoopStarted;
    }
}
