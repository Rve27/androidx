/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.backported.fixes

/**
 * Reports the status of a known issue on this device.
 *
 * @param resolver a function that takes a [KnownIssue] and returns its [Status] on this device.
 *   This parameter is only used for testing. In normal flows, the empty constructor should be used.
 */
internal class BackportedFixManager(private val resolver: StatusResolver) {
    /** Creates a BackportedFixManager object using the default lookup strategy. */
    public constructor() :
        this(
            // TODO b/381267367 - Use Build.getBackportedFixStatus in when available.
            resolver = SystemPropertyResolver()
        )

    /**
     * Is the known issue fixed on this device.
     *
     * @param ki The known issue to check.
     */
    internal fun isFixed(ki: KnownIssue): Boolean {
        return when (getStatus(ki)) {
            Status.Unknown -> false
            Status.Fixed -> true
            Status.NotApplicable -> true
            Status.NotFixed -> false
        }
    }

    /**
     * The status of a known issue on this device.
     *
     * @param ki The known issue to check.
     */
    internal fun getStatus(ki: KnownIssue): Status {
        return if (ki.precondition.invoke()) {
            resolver.invoke(ki)
        } else {
            Status.NotApplicable
        }
    }
}

/** Function that takes a [KnownIssue] and returns its [Status] on this device. */
internal typealias StatusResolver = (KnownIssue) -> Status
