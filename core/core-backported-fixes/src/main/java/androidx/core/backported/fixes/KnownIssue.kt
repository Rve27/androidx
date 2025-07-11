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
 * A public issue from the [Google Issue Tracker](https://issuetracker.google.com) with a fix that
 * may be backported.
 *
 * [BackportedFixManager.isFixed] will report if the issue is fixed on a device.
 *
 * The `id` and `alias` of a known issue come from the list of approved backported fixes in the
 * Android Compatibility Test source directory
 * [cts/backported_fixes/approved](https://cs.android.com/android/platform/superproject/+/android-latest-release:cts/backported_fixes/approved/).
 * [KnownIssues] contains constants for all issues in the approved list.
 *
 * @property id The public id of this issue in the
 *   [Google Issue Tracker](https://issuetracker.google.com)
 * @property alias The alias for this issue, if one exists.
 * @param precondition A function that returns true if the issue applies to a device.
 */
internal class KnownIssue
internal constructor(

    /**
     * The public id of this issue in the [Google Issue Tracker](https://issuetracker.google.com)
     */
    internal val id: Long,
    /**
     * The alias for this issue, if one exists.
     *
     * Known issues can have at most one alias.
     *
     * The value 0 indicates there is no alias for this issue. Non-zero alias values are unique
     * across all known issues.
     */
    internal val alias: Int,
    /**
     * A function that returns true if the issue applies to a device.
     *
     * Override if the issue should only apply to specific brands, models or other static
     * properties.
     *
     * Defaults to true for all devices.
     */
    internal val precondition: () -> Boolean = { true },
) {
    // TODO b/381266031 - Make public
    // TODO b/381267367 - Add link to public list issues

    /**
     * The url to the [Google Issue Tracker](https://issuetracker.google.com) for this known issue.
     */
    internal val url = "https://issuetracker.google.com/issues/$id"

    override fun equals(other: Any?): Boolean = other is KnownIssue && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return if (alias == 0) {
            "$id without alias"
        } else {
            "$id with alias $alias"
        }
    }
}
