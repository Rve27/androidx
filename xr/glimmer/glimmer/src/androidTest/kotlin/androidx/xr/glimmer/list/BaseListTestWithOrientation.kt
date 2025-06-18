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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

abstract class BaseListTestWithOrientation(protected val orientation: Orientation) {

    protected val vertical: Boolean = orientation == Orientation.Vertical

    @Composable
    internal fun TestList(
        modifier: Modifier = Modifier,
        horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
        verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
        verticalArrangement: Arrangement.Vertical = Arrangement.Center,
        state: ListState = rememberListState(),
        itemsCount: Int = Int.MAX_VALUE,
        keyProvider: ((index: Int) -> Any)? = null,
        itemContent: @Composable (index: Int) -> Unit,
    ) {
        List(
            state = state,
            orientation = orientation,
            horizontalAlignment = horizontalAlignment,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            verticalArrangement = verticalArrangement,
            modifier = modifier.testTag(LIST_TEST_TAG),
        ) {
            items(itemsCount, key = keyProvider) { index -> itemContent(index) }
        }
    }

    companion object {
        internal const val LIST_TEST_TAG: String = "glimmer-lazy-list"
    }
}
