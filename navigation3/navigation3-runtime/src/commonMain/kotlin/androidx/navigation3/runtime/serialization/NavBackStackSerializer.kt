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

package androidx.navigation3.runtime.serialization

import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

/**
 * A [KSerializer] for [NavBackStack].
 *
 * This serializer wraps a [KSerializer] for the element type [T], enabling serialization and
 * deserialization of [NavBackStack] instances. The serialization of individual elements is
 * delegated to the provided [elementSerializer].
 *
 * If your stack elements [T] are open polymorphic (e.g., a interface for different screens), the
 * provided [elementSerializer] must be correctly configured to handle this.
 *
 * @param T The type of elements stored in the [NavBackStack].
 * @param elementSerializer The [KSerializer] used to serialize and deserialize individual elements.
 */
public class NavBackStackSerializer<T : NavKey>(private val elementSerializer: KSerializer<T>) :
    KSerializer<NavBackStack<T>> {

    private val surrogate = SnapshotStateListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor
        get() = surrogate.descriptor

    override fun serialize(encoder: Encoder, value: NavBackStack<T>) {
        // TODO(mgalhardo): make `NavBackStack.base` internal
        encoder.encodeSerializableValue(serializer = surrogate, value = value.toMutableStateList())
    }

    override fun deserialize(decoder: Decoder): NavBackStack<T> {
        return NavBackStack(base = decoder.decodeSerializableValue(deserializer = surrogate))
    }
}

/**
 * Creates a [NavBackStackSerializer] that can handle polymorphic [NavKey] types.
 *
 * This factory function is a convenience for creating a serializer for a [NavBackStack] whose
 * elements are polymorphic (e.g., different implementations of a `NavKey` interface).
 *
 * It automatically resolves the correct [KSerializer] for the element type [T] (which is typically
 * the base [NavKey] interface) using the `serializersModule` from the provided [configuration].
 *
 * **Important:** The [SavedStateConfiguration] (and its `serializersModule`) passed here **must**
 * be the same one provided to the `Encoder`/`Decoder` (e.g., via `Json.encodeToDynamic` or
 * `Json.decodeFromDynamic`). `kotlinx.serialization`'s polymorphic dispatch relies on the module
 * available during the encoding/decoding process, not the one used to create this serializer.
 *
 * @param T The reified element type, typically the base [NavKey] interface.
 * @param configuration The [SavedStateConfiguration] containing the `serializersModule` which
 *   registers all concrete [NavKey] implementations.
 * @return A new [NavBackStackSerializer] configured for polymorphic serialization.
 */
public inline fun <reified T : NavKey> NavBackStackSerializer(
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT
): NavBackStackSerializer<T> {
    return NavBackStackSerializer(
        // Resolve the element serializer from the provided configuration’s serializersModule.
        // Important: you MUST pass the same module to your encode/decode calls. Polymorphic
        // dispatch depends on the Encoder/Decoder’s module, not on this KSerializer.
        elementSerializer = configuration.serializersModule.serializer<T>()
    )
}
