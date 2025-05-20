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

package androidx.xr.scenecore.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.Space;
import androidx.xr.runtime.internal.SurfaceEntity;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SurfaceEntityImplTest {
    private static final int SUBSPACE_ID = 5;

    private SurfaceEntity mSurfaceEntity;

    @Before
    public void setUp() {
        XrExtensions xrExtensions = XrExtensionsProvider.getXrExtensions();
        SplitEngineSubspaceManager splitEngineSubspaceManager =
                mock(SplitEngineSubspaceManager.class);

        Assert.assertNotNull(xrExtensions);
        Node node = xrExtensions.createNode();
        SubspaceNode expectedSubspaceNode = new SubspaceNode(SUBSPACE_ID, node);

        when(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(expectedSubspaceNode);

        FakeImpressApi impressApi = new FakeImpressApi();
        int subspaceImpressNode = impressApi.createImpressNode();
        String subspaceName = "stereo_surface_panel_entity_subspace_" + subspaceImpressNode;
        SubspaceNode subspaceNode =
                splitEngineSubspaceManager.createSubspace(subspaceName, subspaceImpressNode);

        EntityManager entityManager = new EntityManager();
        FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
        Entity parentEntity =
                new AndroidXrEntity(
                        subspaceNode.getSubspaceNode(), xrExtensions, entityManager, executor) {};

        int stereoMode = SurfaceEntity.StereoMode.MONO;
        SurfaceEntity.CanvasShape canvasShape = new SurfaceEntity.CanvasShape.Quad(1f, 1f);
        Pose pose = Pose.Identity;
        int contentSecurityLevel = 0;

        mSurfaceEntity =
                new SurfaceEntityImpl(
                        parentEntity,
                        impressApi,
                        splitEngineSubspaceManager,
                        xrExtensions,
                        entityManager,
                        executor,
                        stereoMode,
                        canvasShape,
                        contentSecurityLevel);
        mSurfaceEntity.setPose(pose, Space.PARENT);
    }

    @After
    public void tearDown() {
        mSurfaceEntity.dispose();
    }

    @Test
    public void setCanvasShape_setsCanvasShape() {
        SurfaceEntity.CanvasShape expectedCanvasShape =
                new SurfaceEntity.CanvasShape.Quad(12f, 12f);
        mSurfaceEntity.setCanvasShape(expectedCanvasShape);
        SurfaceEntity.CanvasShape canvasShape = mSurfaceEntity.getCanvasShape();

        assertThat(canvasShape.getClass()).isEqualTo(expectedCanvasShape.getClass());
        assertThat(canvasShape.getDimensions()).isEqualTo(expectedCanvasShape.getDimensions());

        expectedCanvasShape = new SurfaceEntity.CanvasShape.Vr360Sphere(11f);
        mSurfaceEntity.setCanvasShape(expectedCanvasShape);
        canvasShape = mSurfaceEntity.getCanvasShape();

        assertThat(canvasShape.getClass()).isEqualTo(expectedCanvasShape.getClass());
        assertThat(canvasShape.getDimensions()).isEqualTo(expectedCanvasShape.getDimensions());

        expectedCanvasShape = new SurfaceEntity.CanvasShape.Vr180Hemisphere(10f);
        mSurfaceEntity.setCanvasShape(expectedCanvasShape);
        canvasShape = mSurfaceEntity.getCanvasShape();

        assertThat(canvasShape.getClass()).isEqualTo(expectedCanvasShape.getClass());
        assertThat(canvasShape.getDimensions()).isEqualTo(expectedCanvasShape.getDimensions());
    }

    @Test
    public void setStereoMode_setsStereoMode() {
        int expectedStereoMode = SurfaceEntity.StereoMode.MONO;
        mSurfaceEntity.setStereoMode(expectedStereoMode);
        int stereoMode = mSurfaceEntity.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);

        expectedStereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM;
        mSurfaceEntity.setStereoMode(expectedStereoMode);
        stereoMode = mSurfaceEntity.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);
    }

    @Test
    public void setFeatherRadiusX_setsFeatherRadiusX() {
        float expectedFeatherRadiusX = 1;
        mSurfaceEntity.setFeatherRadiusX(expectedFeatherRadiusX);
        float featherRadiusX = mSurfaceEntity.getFeatherRadiusX();

        assertThat(featherRadiusX).isEqualTo(expectedFeatherRadiusX);

        expectedFeatherRadiusX = 11;
        mSurfaceEntity.setFeatherRadiusX(expectedFeatherRadiusX);
        featherRadiusX = mSurfaceEntity.getFeatherRadiusX();

        assertThat(featherRadiusX).isEqualTo(expectedFeatherRadiusX);
    }

    @Test
    public void setFeatherRadiusY_setsFeatherRadiusY() {
        float expectedFeatherRadiusY = 1;
        mSurfaceEntity.setFeatherRadiusY(expectedFeatherRadiusY);
        float featherRadiusY = mSurfaceEntity.getFeatherRadiusY();

        assertThat(featherRadiusY).isEqualTo(expectedFeatherRadiusY);

        expectedFeatherRadiusY = 11;
        mSurfaceEntity.setFeatherRadiusY(expectedFeatherRadiusY);
        featherRadiusY = mSurfaceEntity.getFeatherRadiusY();

        assertThat(featherRadiusY).isEqualTo(expectedFeatherRadiusY);
    }
}
