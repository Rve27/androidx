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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.integration.demos.previews

import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rememberRemoteColor
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
private fun RemoteIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemotePreview(profile = profile) {
        Container { RemoteIcon(imageVector = VolumeUp, contentDescription = null) }
    }

@WearPreviewDevices
@Composable
private fun RemoteIconColorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemotePreview(profile = profile) {
        Container {
            val color = rememberRemoteColor("testColor") { Color.Red }
            RemoteIcon(imageVector = VolumeUp, contentDescription = null, tint = color)
        }
    }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(
        modifier,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
        content = content,
    )
}

private val VolumeUp =
    object : RemoteImageVector(autoMirror = true) {
        override fun RemotePath.buildPath() {
            moveTo(3.0f, 9.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(4.0f)
            lineToRelative(5.0f, 5.0f)
            lineTo(12.0f, 4.0f)
            lineTo(7.0f, 9.0f)
            lineTo(3.0f, 9.0f)
            close()
            moveTo(16.5f, 12.0f)
            curveToRelative(0.0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
            verticalLineToRelative(8.05f)
            curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
            close()
            moveTo(14.0f, 3.23f)
            verticalLineToRelative(2.06f)
            curveToRelative(2.89f, 0.86f, 5.0f, 3.54f, 5.0f, 6.71f)
            reflectiveCurveToRelative(-2.11f, 5.85f, -5.0f, 6.71f)
            verticalLineToRelative(2.06f)
            curveToRelative(4.01f, -0.91f, 7.0f, -4.49f, 7.0f, -8.77f)
            reflectiveCurveToRelative(-2.99f, -7.86f, -7.0f, -8.77f)
            close()
        }
    }
