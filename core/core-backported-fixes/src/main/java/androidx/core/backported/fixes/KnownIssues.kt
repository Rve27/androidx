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

package androidx.core.backported.fixes

import android.os.Build

/**
 * List of all known issues reportable by [BackportedFixManager].
 *
 * The `id` and `alias` of a known issue come from the list of approved backported fixes in the
 * Android Compatibility Test source directory
 * [cts/backported_fixes/approved](https://cs.android.com/android/platform/superproject/+/android-latest-release:cts/backported_fixes/approved/).
 */
internal class KnownIssues private constructor() {
    companion object {

        /** Sample known issue that is always fixed on a device. */
        @JvmField internal val KI_350037023 = KnownIssue(350037023L, 1)

        /** Sample known issue that is never fixed on a device. */
        @JvmField internal val KI_350037348 = KnownIssue(350037348L, 3)

        /** Sample known issue that is only applies to robolectric devices */
        @JvmField
        internal val KI_372917199 =
            KnownIssue(372917199L, 2) { (Build.BRAND.equals("robolectric")) }
    }
}
