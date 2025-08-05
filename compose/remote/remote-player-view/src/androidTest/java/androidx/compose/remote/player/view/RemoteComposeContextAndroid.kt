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

package androidx.compose.remote.player.view

import android.graphics.Bitmap
import androidx.compose.remote.core.Platform
import androidx.compose.remote.creation.Painter
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.profile.PlatformProfile
import androidx.compose.remote.creation.profile.Profile

class RemoteComposeContextAndroid : RemoteComposeContext {

    fun getPainter(): Painter {
        if (mRemoteWriter is RemoteComposeWriterAndroid) {
            return (mRemoteWriter as RemoteComposeWriterAndroid).painter
        }
        throw (Exception("This RemoteComposeContext is not an Android one"))
    }

    constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        platform: Platform,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(width, height, contentDescription, platform)) {
        content()
    }

    constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        apiLevel: Int,
        profiles: Int,
        platform: Platform,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(
        RemoteComposeWriterAndroid(width, height, contentDescription, apiLevel, profiles, platform)
    ) {
        content()
    }

    constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        profile: Profile = PlatformProfile.ANDROIDX,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(width, height, contentDescription, profile) {
        content()
    }

    public fun drawBitmap(image: Bitmap, contentDescription: String) {
        mRemoteWriter.drawBitmap(image, image.width, image.height, contentDescription)
    }
}
