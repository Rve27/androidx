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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.modifiers.BorderModifier as CreationBorderModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BorderModifier(public val width: RemoteFloat, public val color: RemoteColor) :
    RemoteModifier.Element {
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        // TODO use addModifierDynamicBorder
        return CreationBorderModifier(
            width.floatId,
            0f,
            color.constantValue!!.toArgb(),
            ShapeType.RECTANGLE,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.border(width: RemoteDp, color: RemoteColor): RemoteModifier =
    then(BorderModifier(width.toPx(), color))
