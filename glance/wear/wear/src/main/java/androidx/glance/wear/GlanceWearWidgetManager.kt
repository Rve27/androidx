/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.google.wear.Sdk
import com.google.wear.services.tiles.TileInstance
import com.google.wear.services.tiles.TilesManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manager for Glance Wear Widgets.
 *
 * This is used to query the wear widgets currently active on the system.
 */
// TODO: b/429980862 - Make this public.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GlanceWearWidgetManager
@VisibleForTesting
internal constructor(private val tilesManager: TilesManager) {

    /**
     * Creates a new [GlanceWearWidgetManager].
     *
     * @param context The application context.
     */
    public constructor(
        context: Context
    ) : this(Sdk.getWearManager(context, TilesManager::class.java))

    /**
     * Returns the currently active widgets associated with the specified component.
     *
     * This filters the results to only include widgets belonging to the provided [clazz].
     *
     * Note: A [ContainerInfo.CONTAINER_TYPE_FULLSCREEN] result indicates that the widget is running
     * in compatibility/fullscreen mode.
     *
     * @param clazz The component class of the [GlanceWearWidgetService] to filter by.
     */
    // TODO: b/429980862 - Add a fallback for older devices.
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public suspend fun <T : GlanceWearWidgetService> getActiveWidgetsForProvider(
        clazz: KClass<T>
    ): List<ActiveWearWidgetHandle> =
        getActiveWidgets().filter { it.provider.className == clazz.java.name }

    /**
     * Returns all active widgets and tiles associated with the calling package.
     *
     * This includes:
     * * **Tiles** provided via `androidx.wear.tiles.TileService`.
     * * **Widgets** provided via [GlanceWearWidgetService].
     * * Note: A [ContainerInfo.CONTAINER_TYPE_FULLSCREEN] result represents either a standard
     *   TileService or a [GlanceWearWidgetService] running in compatibility/fullscreen mode.
     */
    // TODO: b/429980862 - Add a fallback for older devices.
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public suspend fun getActiveWidgets(): List<ActiveWearWidgetHandle> =
        getActiveWidgetsInternal().map { instance ->
            val provider = instance.tileProvider
            ActiveWearWidgetHandle(
                provider = provider.componentName,
                instanceId =
                    WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, instance.id),
                // TODO: b/429980862 - Set container type correctly.
                containerType = ContainerInfo.CONTAINER_TYPE_FULLSCREEN,
            )
        }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun getActiveWidgetsInternal(): List<TileInstance> =
        suspendCancellableCoroutine { continuation ->
            tilesManager.getActiveTiles(
                Runnable::run,
                object : OutcomeReceiver<List<TileInstance>, Exception> {
                    override fun onResult(values: List<TileInstance>) {
                        continuation.resume(values)
                    }

                    override fun onError(error: Exception) {
                        continuation.resumeWithException(error)
                    }
                },
            )
        }
}
