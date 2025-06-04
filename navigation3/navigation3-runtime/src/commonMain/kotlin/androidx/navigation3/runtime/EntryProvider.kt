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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass

@DslMarker public annotation class EntryDsl

/** Creates an [EntryProviderBuilder] with the entry providers provided in the builder. */
public inline fun <T : Any> entryProvider(
    noinline fallback: (unknownScreen: T) -> NavEntry<T> = {
        throw IllegalStateException("Unknown screen $it")
    },
    builder: EntryProviderBuilder<T>.() -> Unit,
): (T) -> NavEntry<T> = EntryProviderBuilder<T>(fallback).apply(builder).build()

/** DSL for constructing a new [NavEntry] */
@Suppress("TopLevelBuilder")
@EntryDsl
public class EntryProviderBuilder<T : Any>(
    private val fallback: (unknownScreen: T) -> NavEntry<T>
) {
    private val clazzProviders = mutableMapOf<KClass<*>, EntryClassProvider<*>>()
    private val providers = mutableMapOf<Any, EntryProvider<*>>()

    /** Builds a [NavEntry] for the given [key] that displays [content]. */
    @Suppress("SetterReturnsThis", "MissingGetterMatchingBuilder")
    public fun <T : Any> addEntryProvider(
        key: T,
        contentKey: Any = key,
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (T) -> Unit,
    ) {
        require(key !in providers) {
            "An `entry` with the key `key` has already been added: ${key}."
        }
        providers[key] = EntryProvider(key, contentKey, metadata, content)
    }

    /** Builds a [NavEntry] for the given [clazz] that displays [content]. */
    @Suppress("SetterReturnsThis", "MissingGetterMatchingBuilder")
    public fun <T : Any> addEntryProvider(
        clazz: KClass<T>,
        clazzContentKey: (key: @JvmSuppressWildcards T) -> Any = { it },
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (T) -> Unit,
    ) {
        require(clazz !in clazzProviders) {
            "An `entry` with the same `clazz` has already been added: ${clazz.simpleName}."
        }
        clazzProviders[clazz] = EntryClassProvider(clazz, clazzContentKey, metadata, content)
    }

    /**
     * Returns an instance of entryProvider created from the entry providers set on this builder.
     */
    @Suppress("UNCHECKED_CAST")
    public fun build(): (T) -> NavEntry<T> = { key ->
        val entryClassProvider = clazzProviders[key::class] as? EntryClassProvider<T>
        val entryProvider = providers[key] as? EntryProvider<T>
        entryClassProvider?.run { NavEntry(key, clazzContentKey(key), metadata, content) }
            ?: entryProvider?.run { NavEntry(key, contentKey, metadata, content) }
            ?: fallback.invoke(key)
    }
}

/** Add an entry provider to the [EntryProviderBuilder] */
public fun <T : Any> EntryProviderBuilder<T>.entry(
    key: T,
    contentKey: Any = key,
    metadata: Map<String, Any> = emptyMap(),
    content: @Composable (T) -> Unit,
) {
    addEntryProvider(key, contentKey, metadata, content)
}

/** Add an entry provider to the [EntryProviderBuilder] */
public inline fun <reified T : Any> EntryProviderBuilder<*>.entry(
    noinline clazzContentKey: (key: @JvmSuppressWildcards T) -> Any = { it },
    metadata: Map<String, Any> = emptyMap(),
    noinline content: @Composable (T) -> Unit,
) {
    addEntryProvider(T::class, clazzContentKey, metadata, content)
}

/** Holds a Entry class, metadata, and content for that class */
@Suppress("DataClassDefinition")
public data class EntryClassProvider<T : Any>(
    val clazz: KClass<T>,
    val clazzContentKey: (key: @JvmSuppressWildcards T) -> Any,
    val metadata: Map<String, Any>,
    val content: @Composable (T) -> Unit,
)

/** Holds a Entry class, metadata, and content for that key */
@Suppress("DataClassDefinition")
public data class EntryProvider<T : Any>(
    val key: T,
    val contentKey: Any,
    val metadata: Map<String, Any>,
    val content: @Composable (T) -> Unit,
)
