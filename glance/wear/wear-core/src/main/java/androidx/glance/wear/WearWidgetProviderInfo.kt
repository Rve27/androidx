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
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.glance.wear.WearWidgetProviderInfoXmlParser.parseWearWidgetProviderInfo
import java.util.Objects
import org.xmlpull.v1.XmlPullParserException

/**
 * Describes the metadata for a Wear Widget provider.
 *
 * The fields in this class correspond to the fields in the `<wearwidget-provider>` xml tag.
 *
 * For example, the `AndroidManifest.xml` would contain:
 * ```xml
 * <service
 *     android:name=".MyWidgetService"
 *     android:exported="true">
 *     <meta-data
 *         android:name="androidx.glance.wear.widget.provider"
 *         android:resource="@xml/my_widget_info" />
 * </service>
 * ```
 *
 * And `res/xml/my_widget_info.xml` would contain:
 * ```xml
 * <wearwidget-provider
 *     label="@string/widget_label"
 *     description="@string/widget_description"
 *     icon="@drawable/widget_icon"
 *     preferredType="@integer/glance_wear_container_type_small" >
 *
 *     <container
 *         type="@integer/glance_wear_container_type_small"
 *         previewImage="@drawable/small_preview"/>
 *
 *     <container
 *         type="@integer/glance_wear_container_type_large"
 *         previewImage="@drawable/large_preview"
 *         label="@string/large_container_label_override" />
 *
 * </wearwidget-provider>
 * ```
 *
 * @property providerService The [ComponentName] of this widget provider.
 * @property label The label of this widget.
 * @property description The description of this widget.
 * @property icon The resource id of the icon for this widget.
 * @property containers The list of [ContainerInfo] supported for this widget provider. Each element
 *   represents a supported type.
 * @property preferredType The preferred [ContainerType] to use for a widget instance when type is
 *   not specified when adding a widget. This can be used when this provider is replacing a legacy
 *   Wear Tile.
 * @property group The name of the group this widget provider is associated with. Defaults to the
 *   fully qualified name of the provider service.
 * @property isMultiInstanceSupported Whether this widget provider supports multiple instances.
 * @property configIntentAction The intent action to launch an activity for configuring the widget
 *   when [isMultiInstanceSupported] is set to true. This can be null if no configuration is needed.
 * @property minSchemaVersion The minimum schema version supported by this widget provider.
 * @property maxSchemaVersion The maximum schema version supported by this widget provider.
 */
// TODO: populate default min schema version for remote compose widgets.
public class WearWidgetProviderInfo
@RestrictTo(LIBRARY)
public constructor(
    public val providerService: ComponentName,
    public val label: String,
    public val description: String,
    @DrawableRes public val icon: Int,
    public val containers: List<ContainerInfo>,
    public val preferredType: ContainerType,
    public val group: String = providerService.className,
    public val isMultiInstanceSupported: Boolean = false,
    public val configIntentAction: String? = null,
    public val minSchemaVersion: SchemaVersion? = null,
    public val maxSchemaVersion: SchemaVersion? = null,
) {
    public companion object {
        /** Name for the `meta-data` tag for the provider info. */
        private const val META_DATA_WEAR_WIDGET_PROVIDER = "androidx.glance.wear.widget.provider"

        /**
         * Parses a [WearWidgetProviderInfo] from the metadata of a service.
         *
         * The metadata with name `androidx.glance.wear.widget.provider` should reference an XML
         * resource with the provider info.
         *
         * @param context The [Context] to use for resolving resources and package manager.
         * @param providerService The [ComponentName] of the widget provider service.
         * @throws [PackageManager.NameNotFoundException] if the metadata is not found or the
         *   resource is invalid.
         * @throws [XmlPullParserException] if there is an error parsing the XML resource.
         */
        @Throws(XmlPullParserException::class)
        public fun parseFromService(
            context: Context,
            providerService: ComponentName,
        ): WearWidgetProviderInfo {
            val serviceInfo =
                context.packageManager.getServiceInfo(providerService, PackageManager.GET_META_DATA)
            val xmlParser =
                serviceInfo.loadXmlMetaData(context.packageManager, META_DATA_WEAR_WIDGET_PROVIDER)
                    ?: throw PackageManager.NameNotFoundException(
                        "Invalid meta-data name $META_DATA_WEAR_WIDGET_PROVIDER for service $providerService"
                    )
            return xmlParser.parseWearWidgetProviderInfo(
                context.resources,
                providerService,
                defaultPreferredContainerType = ContainerType.Small,
                defaultGroup = providerService.className,
            )
        }
    }
}

/**
 * Describes a container supported by a widget provider.
 *
 * A container is one representation of a widget, with a given size and shape.
 *
 * The fields in this class correspond to the fields in the `<container>` xml tag.
 *
 * For example:
 * ```xml
 * <container
 *     android:type="@integer/glance_wear_container_type_large"
 *     android:previewImage="@drawable/large_preview"
 *     android:label="@string/large_label" />
 * ```
 *
 * @property type The type of this widget container.
 * @property previewImage The resource id of the preview image for this widget container.
 * @property label The override label for this widget container.
 * @property description The override description for this widget container.
 */
public class ContainerInfo
@RestrictTo(LIBRARY)
public constructor(
    public val type: ContainerType,
    @DrawableRes public val previewImage: Int,
    public val label: String? = null,
    public val description: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContainerInfo) return false
        return type == other.type &&
            previewImage == other.previewImage &&
            label == other.label &&
            description == other.description
    }

    override fun hashCode(): Int {
        return Objects.hash(type, previewImage, label, description)
    }
}

/** The container type of a widget. It defines the size and shape of the container. */
@JvmInline
public value class ContainerType internal constructor(private val value: Int) {

    public companion object {
        /** Represents a fullscreen widget container, equivalent to a Wear Tile. */
        public val Fullscreen: ContainerType = ContainerType(0)

        /**
         * Represents a large widget container. Support for this container type is device dependent.
         */
        public val Large: ContainerType = ContainerType(1)

        /**
         * Represents a small widget container. Support for this container type is device dependent.
         */
        public val Small: ContainerType = ContainerType(2)
    }
}

/**
 * The schema version of the widget renderer.
 *
 * @property major The major version of the schema.
 * @property minor The minor version of the schema.
 */
public class SchemaVersion(public val major: Int, public val minor: Int)
