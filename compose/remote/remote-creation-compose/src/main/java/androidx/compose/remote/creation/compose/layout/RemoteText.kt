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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.material.LocalTextStyle
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.state.MutableRemoteString
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteIntReference
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rememberRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
@RemoteComposable
public fun RemoteText(
    text: String,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor = RemoteColor(Color.Black),
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    val remoteText = rememberRemoteString { text }
    RemoteText(
        remoteText,
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        textAlign,
        overflow,
        maxLines,
        style,
    )
}

@Composable
@RemoteComposable
public fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    val textColor = color ?: RemoteColor(style.color.takeOrElse { Color.Black })

    val style =
        style.merge(
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            fontFamily = fontFamily,
            fontStyle = fontStyle,
        )

    val fontSize =
        with(LocalDensity.current) {
                if (style.fontSize == TextUnit.Unspecified) 12.sp.toPx() else style.fontSize.toPx()
            }
            .rf

    RemoteText(
        text = text,
        modifier = modifier,
        color = textColor,
        fontSize = fontSize,
        fontStyle = style.fontStyle ?: FontStyle.Normal,
        fontWeight = style.fontWeight?.weight?.rf ?: 400.rf,
        fontFamily = fontFamily.encode(),
        textAlign = style.textAlign,
        overflow = overflow,
        maxLines = maxLines,
        fontVariationSettings = fontVariationSettings,
    )
}

@Composable
@RemoteComposable
public fun RemoteText(
    text: RemoteString,
    color: RemoteColor,
    fontSize: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    fontStyle: FontStyle = FontStyle.Normal,
    fontWeight: RemoteFloat = 400.rf,
    textAlign: TextAlign = TextAlign.Start,
    fontFamily: String? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    val captureMode = LocalRemoteComposeCreationState.current

    val useCoreTextComponent =
        LocalRemoteComposeCreationState.current.profile.supportedOperations.contains(
            Operations.CORE_TEXT
        )
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    if (useCoreTextComponent) {
        androidx.compose.foundation.layout.Box(
            RemoteComposeCoreTextComponentModifier(
                    modifier = modifier.toRemoteCompose(),
                    id = text,
                    color = color,
                    fontSize = fontSize,
                    fontStyle = fontStyle.encode(),
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    textAlign = textAlign.encode(),
                    overflow = overflow.encode(),
                    maxLines = maxLines,
                    fontVariationSettings = fontVariationSettings,
                )
                .then(modifier.toComposeUiLayout())
        )
    } else {
        androidx.compose.foundation.layout.Box(
            RemoteComposeTextComponentModifier(
                    modifier = modifier.toRemoteCompose(),
                    id = RemoteIntReference(text.getIdForCreationState(captureMode)),
                    color =
                        color.constantValue?.toArgb() ?: color.getIdForCreationState(captureMode),
                    isColorConstant = color.hasConstantValue,
                    fontSize = fontSize.id,
                    fontStyle = fontStyle.encode(),
                    fontWeight = fontWeight.constantValue ?: 400f,
                    fontFamily = fontFamily,
                    textAlign = textAlign.encode(),
                    overflow = overflow.encode(),
                    maxLines = maxLines,
                )
                .then(modifier.toComposeUiLayout())
        )
    }
}

@Composable
private fun TextOverflow.encode(): Int =
    when (this) {
        TextOverflow.Clip -> TextLayout.OVERFLOW_CLIP
        TextOverflow.Visible -> TextLayout.OVERFLOW_VISIBLE
        TextOverflow.Ellipsis -> TextLayout.OVERFLOW_ELLIPSIS
        TextOverflow.StartEllipsis -> TextLayout.OVERFLOW_START_ELLIPSIS
        TextOverflow.MiddleEllipsis -> TextLayout.OVERFLOW_MIDDLE_ELLIPSIS
        else -> -1
    }

@Composable
private fun TextAlign.encode(): Int =
    when (this) {
        TextAlign.Left -> TextLayout.TEXT_ALIGN_LEFT
        TextAlign.Right -> TextLayout.TEXT_ALIGN_RIGHT
        TextAlign.Center -> TextLayout.TEXT_ALIGN_CENTER
        TextAlign.Justify -> TextLayout.TEXT_ALIGN_JUSTIFY
        TextAlign.Start -> TextLayout.TEXT_ALIGN_START
        TextAlign.End -> TextLayout.TEXT_ALIGN_END
        TextAlign.Unspecified -> Int.MIN_VALUE
        else -> -1
    }

@Composable
private fun FontStyle.encode(): Int =
    when (this) {
        FontStyle.Normal -> 0
        FontStyle.Italic -> 1
        else -> -1
    }

@Composable
private fun FontFamily?.encode(): String? =
    when (this) {
        FontFamily.Default -> "default"
        FontFamily.SansSerif -> "sans-serif"
        FontFamily.Serif -> "serif"
        FontFamily.Monospace -> "monospace"
        FontFamily.Cursive -> "cursive"
        is GenericFontFamily -> name
        else -> null
    }

/** Utility modifier to record the layout information */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeTextComponentModifier(
    public var modifier: RecordingModifier,
    public var id: RemoteIntReference,
    public var color: Int,
    public val isColorConstant: Boolean,
    public var fontSize: Float,
    public var fontStyle: Int,
    public var fontWeight: Float,
    public var fontFamily: String?,
    public var textAlign: Int,
    public var overflow: Int,
    public var maxLines: Int,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            val flags =
                if (isColorConstant) {
                    0
                } else {
                    TextLayout.FLAG_IS_DYNAMIC_COLOR.toShort()
                }

            canvas.document.startTextComponent(
                modifier,
                id.toInt(),
                color,
                fontSize,
                fontStyle,
                fontWeight,
                fontFamily,
                flags,
                textAlign.toShort(),
                overflow,
                maxLines,
            )

            this@draw.drawContent()
            canvas.document.endTextComponent()
        }
    }
}

/** Utility modifier to record the layout information */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeCoreTextComponentModifier(
    public val modifier: RecordingModifier,
    public val id: RemoteString,
    public val color: RemoteColor,
    public val fontSize: RemoteFloat,
    public val fontStyle: Int,
    public val fontWeight: RemoteFloat,
    public val fontFamily: String?,
    public val textAlign: Int,
    public val overflow: Int,
    public val maxLines: Int,
    public val fontVariationSettings: FontVariation.Settings?,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        val settings = fontVariationSettings?.settings
        drawIntoRemoteCanvas { canvas ->
            val (fontAxisNames, fontAxisValues) = extractFontSettings(settings)
            canvas.document.startTextComponent(
                modifier,
                id.getIdForCreationState(canvas.creationState),
                color.constantValue?.toArgb() ?: Color.Black.toArgb(),
                if (color.hasConstantValue) -1
                else color.getIdForCreationState(canvas.creationState),
                fontSize.getFloatIdForCreationState(canvas.creationState),
                fontStyle,
                fontWeight.getFloatIdForCreationState(canvas.creationState),
                fontFamily,
                textAlign,
                overflow,
                maxLines,
                0f,
                0f,
                1f,
                0,
                0,
                0,
                false,
                false,
                fontAxisNames,
                fontAxisValues,
                false,
                0,
            )

            this@draw.drawContent()
            canvas.document.endTextComponent()
        }
    }

    private fun extractFontSettings(
        settings: List<FontVariation.Setting>?
    ): Pair<Array<String>?, FloatArray?> {
        val size = settings?.size ?: return Pair(null, null)

        val fontAxisNames = Array(size) { settings[it].axisName }
        val fontAxisValues = FloatArray(size) { settings[it].toVariationValue(null) }

        return Pair(fontAxisNames, fontAxisValues)
    }
}

@Composable
@RemoteComposable
public fun RemoteText(
    textId: RemoteIntReference,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor = RemoteColor(Color.Black),
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    RemoteText(
        MutableRemoteString(textId.toInt()),
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        textAlign,
        overflow,
        maxLines,
        style,
    )
}
