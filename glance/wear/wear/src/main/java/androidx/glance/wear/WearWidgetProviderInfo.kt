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

package androidx.glance.wear

import android.content.ComponentName
import androidx.annotation.DrawableRes

/**
 * Describes the meta data for a Wear Widget provider.
 *
 * The fields in this class correspond to the fields in the `<wearwidget-provider>` xml tag.
 *
 * @property provider The provider component for this widget.
 * @property label The label of this widget
 * @property description The description of this widget.
 * @property icon The resource id of the icon for this widget.
 * @property containerType The container size for this widget.
 * @property previewImage The resource id for the preview image for this widget.
 * @property group The name of the group this widget provider is associated with. Defaults to the
 *   fully qualified provider name.
 * @property isMultiInstanceSupported Whether this widget supports multiple instances.
 * @property configAction The configuration action for the widget.
 * @property minSchemaVersion The minimum schema version supported by this widget.
 * @property maxSchemaVersion The maximum schema version supported by this widget.
 *
 * TODO: Allow multiple containers.
 * TODO: populate default min schema version for remote compose widgets.
 */
internal class WearWidgetProviderInfo(
    public val provider: ComponentName,
    public val label: String,
    public val description: String,
    public @DrawableRes val icon: Int,
    public val containerType: ContainerType,
    public @DrawableRes val previewImage: Int,
    public val group: String = provider.className,
    public val isMultiInstanceSupported: Boolean = false,
    public val configAction: String? = null,
    public val minSchemaVersion: SchemaVersion? = null,
    public val maxSchemaVersion: SchemaVersion? = null,
)

/** The container size of a widget. */
@JvmInline
internal value class ContainerType private constructor(private val value: Int) {
    public companion object {
        public val Large: ContainerType = ContainerType(1)
        public val Small: ContainerType = ContainerType(2)
    }
}

/**
 * The host schema version.
 *
 * @property major The major version of the schema.
 * @property minor The minor version of the schema.
 */
internal class SchemaVersion(public val major: Int, public val minor: Int)
