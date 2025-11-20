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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDrawScope
import androidx.compose.remote.creation.compose.capture.painter.RemotePainter
import androidx.compose.remote.creation.compose.capture.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.capture.shaders.linearGradient
import androidx.compose.remote.creation.compose.capture.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.capture.shapes.RemoteShape
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.remoteComponentHeight
import androidx.compose.remote.creation.compose.layout.remoteComponentWidth
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults.scrimGradientEndColor
import androidx.wear.compose.material3.ButtonDefaults.scrimGradientStartColor

/**
 * Base level Wear Material3 [RemoteButton] that offers a single slot to take any content. Used as
 * the container for more opinionated [RemoteButton] components that take specific content such as
 * icons and labels.
 *
 * [RemoteButton] takes the [RemoteButtonDefaults.buttonColors] color scheme by default, with
 * colored background, contrasting content color and no border. This is a high-emphasis button for
 * the primary, most important or most common action on a screen.
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. It must be a constant value.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [RemoteButtonColors] that will be used to resolve the background and content color
 *   for this button in different states. See [RemoteButtonDefaults.buttonColors].
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content Slot for composable body content displayed on the Button
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public fun RemoteButton(
    vararg onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = RemoteBoolean(true),
    colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
    shape: RemoteShape = RemoteButtonDefaults.shape,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        border = border,
        borderColor = borderColor,
        shape = shape,
        contentPadding = contentPadding,
        labelFont = RemoteMaterialTheme.typography.typography.labelMedium,
        containerPainter = null,
        disabledContainerPainter = null,
        content = content,
    )
}

/**
 * Base level Wear Material3 [RemoteButton] that offers parameters for container image backgrounds,
 * with a single slot to take any content.
 *
 * An Image background is a means to reinforce the meaning of information in a Button. Buttons
 * should have a content color that contrasts with the background image and scrim.
 *
 * [RemoteButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable. It must be a constant value.
 * @param containerPainter The background image of this [RemoteButton] when enabled
 * @param disabledContainerPainter The background image of this [RemoteButton] when disabled
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material3 Theme
 * @param colors [RemoteButtonColors] that will be used to resolve the background and content color
 *   for this button in different states. See [RemoteButtonDefaults.buttonColors].
 * @param border Optional [RemoteDp] that will be used to resolve the border for this button in
 *   different states.
 * @param borderColor Optional [RemoteColor] that will be used to resolve the border color for this
 *   button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param content Slot for composable body content displayed on the Button
 */
@Composable
@RemoteComposable
public fun RemoteButton(
    vararg onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = RemoteBoolean(true),
    containerPainter: RemotePainter,
    disabledContainerPainter: RemotePainter =
        RemoteButtonDefaults.disabledContainerPainter(containerPainter),
    colors: RemoteButtonColors = RemoteButtonDefaults.buttonWithContainerPainterColors(),
    border: RemoteDp? = null,
    borderColor: RemoteColor? = null,
    shape: RemoteShape = RemoteButtonDefaults.shape,
    contentPadding: RemotePaddingValues = RemoteButtonDefaults.ContentPadding,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    RemoteButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerPainter = containerPainter,
        disabledContainerPainter = disabledContainerPainter,
        colors = colors,
        shape = shape,
        contentPadding = contentPadding,
        border = border,
        borderColor = borderColor,
        labelFont = RemoteMaterialTheme.typography.typography.labelMedium,
        content = content,
    )
}

/**
 * Button with label. This allows to use the token values for individual buttons instead of relying
 * on common values.
 */
@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
private fun RemoteButtonImpl(
    vararg onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteButtonColors,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    enabled: RemoteBoolean,
    border: RemoteDp?,
    borderColor: RemoteColor?,
    shape: RemoteShape,
    contentPadding: RemotePaddingValues,
    labelFont: TextStyle,
    content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    val state = LocalRemoteComposeCreationState.current
    val containerModifier =
        RemoteModifier.clickable(*onClick, enabled = enabled.constantValue ?: false)
            .padding(
                left = contentPadding.leftPadding.value,
                top = contentPadding.topPadding.value,
                right = contentPadding.rightPadding.value,
                bottom = contentPadding.bottomPadding.value,
            )

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        horizontalArrangement = RemoteArrangement.CenterHorizontally,
        modifier =
            modifier
                .drawWithContent {
                    drawShapedBackground(
                        shape = shape,
                        color = colors.containerColor(enabled),
                        enabled = enabled,
                        containerPainter = containerPainter,
                        disabledContainerPainter = disabledContainerPainter,
                        borderColor = borderColor,
                        borderStrokeWidth = border?.value,
                        state = state,
                    )
                    drawContent()
                }
                .then(containerModifier),
        content = content, // TODO: handle labelFont and content color for content
    )
}

/** Contains the default values used by [RemoteButton] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public object RemoteButtonDefaults {
    /** Recommended [RemoteRoundedCornerShape] for [RemoteButton]. */
    public val shape: RemoteRoundedCornerShape
        @Composable get() = RemoteRoundedCornerShape(16.rdp)

    /**
     * Creates a [RemoteButtonColors] that represents the default background and content colors used
     * in a [RemoteButton].
     */
    @Composable
    public fun buttonColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultButtonColors

    /**
     * Creates a [RemoteButtonColors] for the content in a [RemoteButton] with an image container
     * painter.
     */
    @Composable
    public fun buttonWithContainerPainterColors(): RemoteButtonColors =
        RemoteMaterialTheme.colorScheme.defaultButtonWithContainerPainterColors

    /** The default minimum height applied for the [RemoteButton]. */
    public val Height: Dp = 52.dp

    /** The default minimum width applied for the [RemoteButton]. */
    public val Width: Dp = 12.dp

    /** The recommended horizontal padding used by [RemoteButton] by default */
    public val ButtonHorizontalPadding: RemoteDp = RemoteDp(14f.rf)

    /** The recommended vertical padding used by [RemoteButton] by default */
    public val ButtonVerticalPadding: RemoteDp = RemoteDp(6f.rf)

    /** The default content padding used by [RemoteButton] */
    public val ContentPadding: RemotePaddingValues =
        RemotePaddingValues(horizontal = ButtonHorizontalPadding, vertical = ButtonVerticalPadding)

    /** The default alpha applied to the container when the button is disabled. */
    public val DisabledContainerAlpha: Float = 0.12f

    private val RemoteColorScheme.defaultButtonColors: RemoteButtonColors
        @Composable
        get() {
            return RemoteButtonColors(
                containerColor = primary,
                contentColor = onPrimary,
                secondaryContentColor = onPrimary.copy(alpha = 0.8f.rf),
                iconColor = onPrimary,
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    private val RemoteColorScheme.defaultButtonWithContainerPainterColors: RemoteButtonColors
        @Composable
        get() {
            return RemoteButtonColors(
                containerColor = RemoteColor(Color.Unspecified),
                contentColor = onBackground,
                secondaryContentColor = onBackground.copy(alpha = 0.8f.rf),
                iconColor = onBackground,
                disabledContainerColor = RemoteColor(Color.Unspecified),
                disabledContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSecondaryContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
        }

    /**
     * Creates a [RemotePainter] for the background of a [RemoteButton] with container painter, that
     * displays an image with a scrim on top to make sure that any content above the background will
     * be legible.
     *
     * An Image background is a means to reinforce the meaning of information in a Button. Buttons
     * should have a content color that contrasts with the background image and scrim.
     *
     * @param image The [RemotePainter] to use to draw the container background of the
     *   [RemoteButton].
     * @param scrim The [RemoteBrush] to use to paint a scrim over the container image to ensure
     *   that any text drawn over the image is legible.
     * @param alpha Opacity of the container image painter and scrim.
     */
    @Composable
    public fun containerPainter(
        image: RemotePainter,
        scrim: RemoteBrush? = scrimBrush(image.intrinsicSize),
        alpha: RemoteFloat = DefaultAlpha.rf,
    ): RemotePainter {
        return remoteContainerPainter(image, scrim, alpha)
    }

    /**
     * Creates a [RemotePainter] for the disabled background of a [RemoteButton] with container
     * painter - draws the containerPainter with an alpha applied to achieve a disabled effect.
     *
     * An Image background is a means to reinforce the meaning of information in a Button. Buttons
     * should have a content color that contrasts with the background image and scrim.
     *
     * @param containerPainter The [RemotePainter] to use to draw the container background of the
     *   [RemoteButton].
     */
    @Composable
    public fun disabledContainerPainter(containerPainter: RemotePainter): RemotePainter {
        return disabledRemoteContainerPainter(
            painter = containerPainter,
            alpha = DisabledContainerAlpha.rf,
        )
    }

    /**
     * Creates a [RemoteBrush] for the recommended scrim drawn on top of image container
     * backgrounds.
     */
    @Composable
    public fun scrimBrush(size: RemoteSize): RemoteBrush {
        val startColor = scrimGradientStartColor
        val endColor = scrimGradientEndColor
        return RemoteBrush.linearGradient(
            colors = listOf(startColor, endColor),
            RemoteOffset.Zero,
            RemoteOffset(size.width, size.height),
        )
    }
}

/**
 * Represents the container and content colors used in buttons in different states.
 *
 * @param containerColor The background color of this [RemoteButton] when enabled (overridden by the
 *   containerPainter parameter on Buttons with image backgrounds).
 * @param contentColor The content color of this [RemoteButton] when enabled.
 * @param secondaryContentColor The content color of this [RemoteButton] when enabled.
 * @param iconColor The content color of this [RemoteButton] when enabled.
 * @param disabledContainerColor The background color of this [RemoteButton] when not enabled
 *   (overridden by the disabledContainerPainter parameter on Buttons with image backgrounds)
 * @param disabledContentColor The content color of this [RemoteButton] when not enabled.
 * @param disabledSecondaryContentColor The content color of this [RemoteButton] when not enabled.
 * @param disabledIconColor The content color of this [RemoteButton] when not enabled.
 */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("RestrictedApiAndroidX")
public class RemoteButtonColors(
    public val containerColor: RemoteColor,
    public val contentColor: RemoteColor,
    public val secondaryContentColor: RemoteColor,
    public val iconColor: RemoteColor,
    public val disabledContainerColor: RemoteColor,
    public val disabledContentColor: RemoteColor,
    public val disabledSecondaryContentColor: RemoteColor,
    public val disabledIconColor: RemoteColor,
) {
    @Stable
    internal fun contentColor(enabled: RemoteBoolean = RemoteBoolean(true)): RemoteColor {
        return enabled.select(ifTrue = contentColor, ifFalse = disabledContentColor)
    }

    @Stable
    internal fun containerColor(enabled: RemoteBoolean = RemoteBoolean(true)): RemoteColor {
        return enabled.select(ifTrue = containerColor, ifFalse = disabledContainerColor)
    }
}

/** Draws a colored and shaped background with when clipping is not supported. */
private fun RemoteDrawScope.drawShapedBackground(
    shape: RemoteShape,
    color: RemoteColor,
    borderColor: RemoteColor?,
    borderStrokeWidth: RemoteFloat?,
    enabled: RemoteBoolean,
    containerPainter: RemotePainter?,
    disabledContainerPainter: RemotePainter?,
    state: RemoteComposeCreationState,
) {
    val w = remoteComponentWidth(state)
    val h = remoteComponentHeight(state)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        canvas.drawRect(0f.rf, 0f.rf, w, h, RemotePaint().apply { remoteColor = color })
        return
    }

    if (!enabled.hasConstantValue) {
        TODO("Dynamic clickable enabled value is not supported.")
    }

    val backgroundImagePainter =
        if (enabled.constantValue == true) containerPainter else disabledContainerPainter

    if (backgroundImagePainter != null) {
        // Draws solid shape as destination
        drawSolidColorShape(shape, w, h)

        // TODO: Fix BlendMode.SRC_IN so it draws an shaped image
        with(backgroundImagePainter) { draw() }
    } else {
        // Draws solid color shape
        drawSolidColorShape(shape, w, h, color)
    }

    // Draw border if specified
    if (borderColor != null && borderStrokeWidth != null) {
        drawBorder(borderColor, borderStrokeWidth, shape, w, h)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawBorder(
    borderColor: RemoteColor,
    borderStrokeWidth: RemoteFloat,
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
) {
    val borderPaint =
        RemotePaint().apply {
            remoteColor = borderColor
            strokeWidth = borderStrokeWidth.toFloat()
            style = Paint.Style.STROKE
        }
    with(shape.createOutline(RemoteSize(w, h), layoutDirection)) { drawOutline(borderPaint) }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawSolidColorShape(
    shape: RemoteShape,
    w: RemoteFloat,
    h: RemoteFloat,
    color: RemoteColor? = null,
) {
    val paint =
        RemotePaint().apply {
            style = Paint.Style.FILL
            remoteColor = color
        }

    with(shape.createOutline(RemoteSize(w, h), layoutDirection)) { drawOutline(paint) }
}

// TODO(b/451927368): Adds HeightInModifier and WidthInModifier that accept RemoteDp
// TODO(b/459724215): Constraint shouldn't be enforced when there is not enough space.
@Composable
internal fun RemoteModifier.buttonSizeModifier(): RemoteModifier =
    this.heightIn(min = RemoteButtonDefaults.Height).widthIn(min = RemoteButtonDefaults.Width)

internal fun RemoteColor.toDisabledColor(
    disabledAlpha: RemoteFloat = DisabledContentAlpha.rf
): RemoteColor = this.copy(alpha = this.alpha * disabledAlpha)
