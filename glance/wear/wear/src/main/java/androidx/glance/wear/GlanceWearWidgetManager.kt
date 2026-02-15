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
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WidgetInstanceId
import com.google.wear.Sdk
import com.google.wear.services.tiles.TileInstance
import com.google.wear.services.tiles.TilesManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manager for Glance Wear Widgets.
 *
 * This is used to query the wear widgets currently active on the system.
 */
// TODO: b/429980862 - Make this public.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GlanceWearWidgetManager {

    /** The [TilesManager] used to query for active tiles/widgets on API 34+. */
    @VisibleForTesting internal val tilesManager: TilesManager?
    /** The [ActiveWidgetStore] used to query for active widgets on API < 34. */
    @VisibleForTesting internal val activeWidgetStore: ActiveWidgetStore?

    /**
     * Creates a new [GlanceWearWidgetManager].
     *
     * @param tilesManager The [TilesManager] used to query for active tiles/widgets on API 34+.
     * @param activeWidgetStore The [ActiveWidgetStore] used to query for active widgets on API
     *   < 34.
     */
    @VisibleForTesting
    internal constructor(tilesManager: TilesManager, activeWidgetStore: ActiveWidgetStore) {
        this.tilesManager = tilesManager
        this.activeWidgetStore = activeWidgetStore
    }

    /**
     * Creates a new [GlanceWearWidgetManager].
     *
     * @param context The application context.
     */
    public constructor(context: Context) {
        val isAtLeastU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        this.tilesManager =
            if (isAtLeastU) Sdk.getWearManager(context, TilesManager::class.java) else null
        this.activeWidgetStore = if (!isAtLeastU) ActiveWidgetStore(context) else null
    }

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
    public suspend fun getActiveWidgetsForProvider(
        clazz: KClass<out GlanceWearWidgetService>
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
    public suspend fun getActiveWidgets(): List<ActiveWearWidgetHandle> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.getActiveTiles(
                tilesManager ?: throw IllegalStateException("TilesManager is not available.")
            )
        } else {
            activeWidgetStore?.getActiveWidgets()
                ?: throw IllegalStateException("ActiveWidgetStore is not available")
        }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object Api34Impl {
        @DoNotInline
        suspend fun getActiveTiles(tilesManager: TilesManager): List<ActiveWearWidgetHandle> =
            suspendCancellableCoroutine { continuation ->
                val receiver = ContinuationOutcomeReceiver(continuation)
                tilesManager.getActiveTiles(Runnable::run, receiver)
            }

        private class ContinuationOutcomeReceiver(
            private val continuation: CancellableContinuation<List<ActiveWearWidgetHandle>>
        ) : OutcomeReceiver<List<TileInstance>, Exception> {
            override fun onResult(result: List<TileInstance>) {
                val widgets =
                    result.map { instance ->
                        val provider = instance.tileProvider
                        ActiveWearWidgetHandle(
                            provider = provider.componentName,
                            instanceId =
                                WidgetInstanceId(
                                    WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE,
                                    instance.id,
                                ),
                            // TODO: b/429980862 - Set container type correctly.
                            containerType = ContainerInfo.CONTAINER_TYPE_FULLSCREEN,
                        )
                    }
                continuation.resume(widgets)
            }

            override fun onError(error: Exception) {
                continuation.resumeWithException(error)
            }
        }
    }
}
