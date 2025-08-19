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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The default scrim implementation shown with a levitated pane to block the user interaction from
 * the underlying layout. See [AdaptStrategy.Levitate] for more detailed info.
 *
 * @sample androidx.compose.material3.adaptive.samples.levitateAsDialogSample
 * @param onClick the on-click listener of the scrim; usually used to dismiss the levitated pane;
 *   i.e. remove the pane from the top of the destination history. By default this will be an empty
 *   lambda, which simply blocks the user interaction from the underlying layout.
 * @param color the color of scrim, by default if [Color.Unspecified] is provided, the pane scaffold
 *   implementation will use a translucent black color.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun Scrim(onClick: (() -> Unit) = {}, color: Color = ScrimDefaults.color) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(color)
                .clickable(interactionSource = null, indication = null, onClick = onClick)
    )
}

/** The objet to provide default values of [Scrim]. */
@ExperimentalMaterial3AdaptiveApi
object ScrimDefaults {
    /** The default color of the scrim, which is a translucent black. */
    val color = Color.Black.copy(alpha = 0.32f)
}
