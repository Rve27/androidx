/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.frontend.state

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.Paint
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

/** Base type for [ColorFilter]s that are parameterized by expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface RemoteColorFilter

/**
 * An [RemoteColorFilter] that represents a [BlendModeColorFilter] where the [color] is
 * parameterized by a [RemoteColor] expression.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBlendModeColorFilter(
    public val color: RemoteColor,
    public val blendMode: BlendMode,
) : RemoteColorFilter

/**
 * An extension of [Paint] that supports binding expressions where we can't do that via existing
 * APIs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemotePaint(flags: Int) : Paint(flags) {
    /** Constructs a [RemotePaint] with [Paint.ANTI_ALIAS_FLAG]. */
    public constructor() : this(Paint.ANTI_ALIAS_FLAG)

    /** The current [RemoteColorFilter] if any. */
    public var remoteColorFilter: RemoteColorFilter? = null
        set(remoteColorFilter) {
            field = remoteColorFilter
            // We don't want both a ColorFilter and a RemoteColorFilter.
            super.setColorFilter(null)
        }

    override fun setColorFilter(filter: ColorFilter?): ColorFilter? {
        // We don't want both a ColorFilter and a RemoteColorFilter.
        remoteColorFilter = null
        return super.setColorFilter(filter)
    }
}
