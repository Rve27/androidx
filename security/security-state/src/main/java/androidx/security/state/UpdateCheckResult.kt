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

package androidx.security.state

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import java.util.Objects

/**
 * Container for the result of a security update check from a specific provider.
 *
 * This class encapsulates the list of available updates along with metadata about the source and
 * freshness of the data. It is returned by the [IUpdateInfoService.listAvailableUpdates] method.
 */
@SuppressLint("BanParcelableUsage")
public class UpdateCheckResult(
    /**
     * The package name of the application that provided these update results.
     *
     * This field identifies the authoritative source of the update data (e.g.,
     * "com.google.android.gms" for System Updates).
     */
    public val providerPackageName: String,

    /** The list of [UpdateInfo] objects representing available updates found during this check. */
    public val updates: List<UpdateInfo>,

    /**
     * The timestamp of the last successful synchronization with the backend, in milliseconds since
     * the epoch.
     *
     * This field is critical for clients to understand the freshness of the result, especially when
     * [updates] is empty. It allows the UI to display messages like "Last checked: 5 minutes ago"
     * versus "Last checked: Yesterday".
     */
    public val lastCheckTimeMillis: Long,
) : Parcelable {
    internal constructor(
        parcel: Parcel
    ) : this(
        providerPackageName = parcel.readString() ?: "",
        updates = parcel.createTypedArrayList(UpdateInfo.CREATOR) ?: emptyList(),
        lastCheckTimeMillis = parcel.readLong(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(providerPackageName)
        parcel.writeTypedList(updates)
        parcel.writeLong(lastCheckTimeMillis)
    }

    override fun describeContents(): Int = 0

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<UpdateCheckResult> =
            object : Parcelable.Creator<UpdateCheckResult> {
                override fun createFromParcel(parcel: Parcel): UpdateCheckResult {
                    return UpdateCheckResult(parcel)
                }

                override fun newArray(size: Int): Array<UpdateCheckResult?> {
                    return arrayOfNulls(size)
                }
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateCheckResult

        if (providerPackageName != other.providerPackageName) return false
        if (updates != other.updates) return false
        if (lastCheckTimeMillis != other.lastCheckTimeMillis) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(providerPackageName, updates, lastCheckTimeMillis)
    }

    override fun toString(): String {
        return "UpdateCheckResult(providerPackageName=$providerPackageName, updates=$updates, lastCheckTimeMillis=$lastCheckTimeMillis)"
    }
}
