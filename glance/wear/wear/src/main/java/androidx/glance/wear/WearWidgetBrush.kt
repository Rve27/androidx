/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteColor

/**
 * Defines a brush for a Wear Widget surface.
 *
 * This class acts similar to [androidx.compose.remote.creation.compose.shaders.RemoteBrush] but it
 * restricts the available options to ensure compatibility with surfaces that support a limited
 * feature set, such as [WearWidgetDocument.background].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WearWidgetBrush
internal constructor(
    // TODO: b/464273091 - Change it to
    // [androidx.compose.remote.creation.compose.shaders.RemoteBrush] when public.
    internal val color: RemoteColor
) {
    /**
     * A companion object for [WearWidgetBrush]. Use it to create a new [WearWidgetBrush] using
     * modifier extension factory functions.
     *
     * Example: `WearWidgetBrush.color(Color.Black.rc)`
     */
    public companion object
}

/** A solid color background. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WearWidgetBrush.Companion.color(color: RemoteColor): WearWidgetBrush =
    WearWidgetBrush(color)
