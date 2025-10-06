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

package androidx.compose.runtime.retain

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainStateProvider.AlwaysRetainExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.NeverRetainExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.NeverRetainExitedValues.isRetainingExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.RetainStateObserver
import androidx.compose.runtime.retain.RetainedValuesStore.*
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Remember the value produced by [calculation] and retain it in the current [RetainedValuesStore].
 * A retained value is one that is persisted in memory to survive transient destruction and
 * recreation of a portion or the entirety of the content in the composition hierarchy. Some
 * examples of when content is transiently destroyed (later referred to as a "retention scenario")
 * include:
 * - Navigation destinations that are on the back stack, not currently visible, and not composed
 * - UI components that are collapsed, not rendering, and not composed
 * - On Android, composition hierarchies hosted by an Activity that is being destroyed and recreated
 *   due to a configuration change
 *
 * When a value retained by [retain] leaves the composition hierarchy during one of these retention
 * scenarios, the [LocalRetainedValuesStore] will persist it until the content is recreated. If an
 * instance of this function then re-enters the composition hierarchy during the recreation, it will
 * return the retained value instead of invoking [calculation] again.
 *
 * If this function leaves the composition hierarchy when the [LocalRetainedValuesStore] is not
 * retaining values that exit the composition, the value will be discarded immediately.
 *
 * The lifecycle of the retained value can be observed by implementing [RetainObserver]. Callbacks
 * from [RememberObserver] are never invoked on objects retained this way. It is invalid to retain
 * an object that is a [RememberObserver] but not a [RetainObserver], and an exception will be
 * thrown.
 *
 * The lifecycle of a retained value is shown in the diagram below. This diagram tracks how a
 * retained value is held through its lifecycle and when it transitions between states.
 *
 * ```
 * ┌──────────────────────┐
 * │                      │
 * │ retain(keys) { ... } │
 * │        ┌────────────┐│
 * └────────┤  value: T  ├┘
 *          └──┬─────────┘
 *             │   ▲
 *         Exit│   │Enter
 *  composition│   │composition
 *    or change│   │
 *         keys│   │                         ┌───────────────────────────┐
 *             │   ├───No retained value─────┤   calculation: () -> T    │
 *             │   │   or different keys     └───────────────────────────┘
 *             │   │                         ┌───────────────────────────┐
 *             │   └───Re-enter composition──┤ Local RetainedValuesStore │
 *             │       with the same keys    └─────────────────┬─────────┘
 *             │                                           ▲   │
 *             │                      ┌─Yes────────────────┘   │ value not
 *             │                      │                        │ restored and
 *             │   .──────────────────┴──────────────────.     │ store stops
 *             └─▶(        isRetainingExitedValues        )    │ retaining exited
 *                 `──────────────────┬──────────────────'     │ values
 *                                    │                        ▼
 *                                    │      ┌──────────────────────────┐
 *                                    └─No──▶│     value is retired     │
 *                                           └──────────────────────────┘
 * ```
 *
 * **Important:** Retained values are held longer than the lifespan of the composable they are
 * associated with. This can cause memory leaks if a retained object is kept beyond its expected
 * lifetime. Be cautious with the types of data that you retain. Never retain an Android Context or
 * an object that references a Context (including View), either directly or indirectly. To mark that
 * a custom class should not be retained (possibly because it will cause a memory leak), you can
 * annotate your class definition with [androidx.compose.runtime.annotation.DoNotRetain].
 *
 * @param calculation A computation to invoke to create a new value, which will be used when a
 *   previous one is not available to return because it was neither remembered nor retained.
 * @return The result of [calculation]
 * @throws IllegalArgumentException if the return result of [calculation] both implements
 *   [RememberObserver] and does not also implement [RetainObserver]
 * @see remember
 */
@Composable
public inline fun <reified T> retain(noinline calculation: () -> T): T {
    return retain(typeHash = classHash<T>(), calculation = calculation)
}

/**
 * Remember the value produced by [calculation] and retain it in the current [RetainedValuesStore].
 * A retained value is one that is persisted in memory to survive transient destruction and
 * recreation of a portion or the entirety of the content in the composition hierarchy. Some
 * examples of when content is transiently destroyed (later referred to as a "retention scenario")
 * include:
 * - Navigation destinations that are on the back stack, not currently visible, and not composed
 * - UI components that are collapsed, not rendering, and not composed
 * - On Android, composition hierarchies hosted by an Activity that is being destroyed and recreated
 *   due to a configuration change
 *
 * When a value retained by [retain] leaves the composition hierarchy during one of these retention
 * scenarios, the [LocalRetainedValuesStore] will persist it until the content is recreated. If an
 * instance of this function then re-enters the composition hierarchy during the recreation, it will
 * return the retained value instead of invoking [calculation] again.
 *
 * If this function leaves the composition hierarchy when the [LocalRetainedValuesStore] is not
 * retaining values that exit the composition or is invoked with list of [keys] that are not all
 * equal (`==`) to the values they had in the previous composition, the value will be discarded
 * immediately and [calculation] will execute again when a new value is needed.
 *
 * The lifecycle of the retained value can be observed by implementing [RetainObserver]. Callbacks
 * from [RememberObserver] are never invoked on objects retained this way. It is illegal to retain
 * an object that is a [RememberObserver] but not a [RetainObserver].
 *
 * Keys passed to this composable will be kept in-memory while the computed value is retained for
 * comparison against the old keys until the value is [retired][RetainObserver.onRetired]. Keys are
 * allowed to implement [RememberObserver] arbitrarily, unlike the values returned by [calculation].
 * If a key implements [RetainObserver], it will **not** receive retention callbacks from this
 * usage.
 *
 * The lifecycle of a retained value is shown in the diagram below. This diagram tracks how a
 * retained value is held through its lifecycle and when it transitions between states.
 *
 * ```text
 * ┌──────────────────────┐
 * │                      │
 * │ retain(keys) { ... } │
 * │        ┌────────────┐│
 * └────────┤  value: T  ├┘
 *          └──┬─────────┘
 *             │   ▲
 *         Exit│   │Enter
 *  composition│   │composition
 *    or change│   │
 *         keys│   │                         ┌───────────────────────────┐
 *             │   ├───No retained value─────┤   calculation: () -> T    │
 *             │   │   or different keys     └───────────────────────────┘
 *             │   │                         ┌───────────────────────────┐
 *             │   └───Re-enter composition──┤ Local RetainedValuesStore │
 *             │       with the same keys    └─────────────────┬─────────┘
 *             │                                           ▲   │
 *             │                      ┌─Yes────────────────┘   │ value not
 *             │                      │                        │ restored and
 *             │   .──────────────────┴──────────────────.     │ store stops
 *             └─▶(        isRetainingExitedValues        )    │ retaining exited
 *                 `──────────────────┬──────────────────'     │ values
 *                                    │                        ▼
 *                                    │      ┌──────────────────────────┐
 *                                    └─No──▶│     value is retired     │
 *                                           └──────────────────────────┘
 * ```
 *
 * **Important:** Retained values are held longer than the lifespan of the composable they are
 * associated with. This can cause memory leaks if a retained object is kept beyond its expected
 * lifetime. Be cautious with the types of data that you retain. Never retain an Android Context or
 * an object that references a Context (including View), either directly or indirectly. To mark that
 * a custom class should not be retained (possibly because it will cause a memory leak), you can
 * annotate your class definition with [androidx.compose.runtime.annotation.DoNotRetain].
 *
 * @param keys An arbitrary list of keys that, if changed, will cause an old retained value to be
 *   discarded and for [calculation] to return a new value, regardless of whether the old value was
 *   being retained in the [RetainedValuesStore] or not.
 * @param calculation A producer that will be invoked to initialize the retained value if a value
 *   from the previous composition isn't available.
 * @return The result of [calculation]
 * @throws IllegalArgumentException if the return result of [calculation] both implements
 *   [RememberObserver] and does not also implement [RetainObserver]
 * @see remember
 */
@Composable
public inline fun <reified T> retain(vararg keys: Any?, noinline calculation: () -> T): T {
    return retain(typeHash = classHash<T>(), keys = keys, calculation = calculation)
}

@PublishedApi
@Composable
internal fun <T> retain(typeHash: Int, calculation: () -> T): T {
    return retainImpl(
        key =
            RetainKeys(
                keys = null,
                positionalKey = currentCompositeKeyHashCode,
                typeHash = typeHash,
            ),
        calculation = calculation,
    )
}

@PublishedApi
@Composable
internal fun <T> retain(typeHash: Int, vararg keys: Any?, calculation: () -> T): T {
    return retainImpl(
        key =
            RetainKeys(
                keys = keys,
                positionalKey = currentCompositeKeyHashCode,
                typeHash = typeHash,
            ),
        calculation = calculation,
    )
}

@Composable
private fun <T> retainImpl(key: RetainKeys, calculation: () -> T): T {
    val retainedValuesStore = LocalRetainedValuesStore.current
    val holder =
        remember(key) {
            val retainedValue =
                retainedValuesStore.getExitedValueOrDefault(key, RetainedValuesStoreMissingValue)
            if (retainedValue !== RetainedValuesStoreMissingValue) {
                RetainedValueHolder(
                    key = key,
                    value = @Suppress("UNCHECKED_CAST") (retainedValue as T),
                    owner = retainedValuesStore,
                    isNewlyRetained = false,
                )
            } else {
                RetainedValueHolder(
                    key = key,
                    value = calculation(),
                    owner = retainedValuesStore,
                    isNewlyRetained = true,
                )
            }
        }

    if (holder.owner !== retainedValuesStore) {
        SideEffect { holder.readoptUnder(retainedValuesStore) }
    }
    return holder.value
}

private val RetainedValuesStoreMissingValue = Any()

/**
 * The [RetainedValuesStore] in which [retain] values will be tracked in. Since a
 * RetainedValuesStore controls retention scenarios and signals when to start and end the retention
 * of objects removed from composition, a composition hierarchy may have several
 * RetainedValuesStores to introduce retention periods to specific pieces of content.
 *
 * The default implementation is a [ForgetfulRetainedValuesStore] that causes [retain] to behave the
 * same as [remember]. On Android, a lifecycle-aware [RetainedValuesStore] is installed at the root
 * of the composition that retains values across configuration changes.
 *
 * If this CompositionLocal is updated, all values previously returned by [retain] will be adopted
 * to the new store and will follow the new store's retention lifecycle.
 *
 * RetainedValuesStores should be installed so that their tracked transiently removed content is
 * always removed from composition in the same frame (and by extension, all retained values leave
 * composition in the same frame). If the RetainedValuesStore starts retaining exited values and its
 * tracked content is removed in an arbitrary order across several recompositions, it may cause
 * retained values to be restored incorrectly if the retained values from different regions in the
 * composition have the same [currentCompositeKeyHashCode].
 */
public val LocalRetainedValuesStore: ProvidableCompositionLocal<RetainedValuesStore> =
    staticCompositionLocalOf {
        ForgetfulRetainedValuesStore
    }

/**
 * A RetainedValuesStore acts as a storage area for objects being retained. An instance of a
 * RetainedValuesStore also defines a specific retention policy to describe when removed retained
 * values should be kept for future reuse and when they should be retired.
 *
 * The general pattern for retention is as follows:
 * 1. The RetainedValuesStore receives a notification that transient content removal is about to
 *    begin. The source of this notification varies depending on what retention scenario is being
 *    captured, but could, for example, be a signal that an Android Activity is being recreated, or
 *    that content is about to be navigated away from/collapsed with the potential of being returned
 *    to. At this time, the store's owner should call [requestRetainExitedValues].
 * 2. Transient content removal begins. The content is recomposed, removed from the hierarchy, and
 *    remembered values are forgotten. Values remembered by [retain] leave the composition but are
 *    not yet released. Every value returned by [retain] will be passed as an argument to
 *    [saveExitingValue] so that it can later be returned by [getExitedValueOrDefault].
 * 3. An arbitrary amount of time passes, and the removed content is restored in the composition
 *    hierarchy at its previous location. When a [retain] call is invoked during the restoration, it
 *    calls [getExitedValueOrDefault]. If all the input keys match a retained value, the previous
 *    result is returned and the retained value is removed from the pool of restorable objects that
 *    exited the previous composition. This step may be skipped if it becomes impossible to return
 *    to the transiently removed content while this store is retaining exited values.
 * 4. The content finishes composing after being restored, and the entire frame completes. The owner
 *    of this store should call [unRequestRetainExitedValues]. When retention stops being requested,
 *    it immediately ends. Any values that are retained and not currently used in a composition (and
 *    therefore not restored by [getExitedValueOrDefault]) are then immediately discarded.
 *
 * A given `RetainedValuesStore` should only be used by a single
 * [Recomposer][androidx.compose.runtime.Recomposer] at a time. It can move between recomposers (for
 * example, when the Window is recreated), but should never be used by two Recomposers
 * simultaneously. It is valid for a RetainedValuesStore to be used in multiple compositions at the
 * same time, or in the same composition multiple times.
 *
 * @see retain
 * @see LocalRetainedValuesStore
 * @see ControlledRetainedValuesStore
 * @see ForgetfulRetainedValuesStore
 */
public abstract class RetainedValuesStore : RetainStateProvider {

    protected var retainExitedValuesRequests: Int = 0
        private set

    final override val isRetainingExitedValues: Boolean
        get() = retainExitedValuesRequests > 0

    private val observers = MutableScatterSet<RetainStateObserver>(0)

    /**
     * If this store is currently retaining exited values and has a value previously created with
     * the given [key], its original record is returned and removed from the list of exited kept
     * objects that this store is tracking.
     *
     * @param key The keys to resolve a retained value that has left composition
     * @param defaultIfAbsent A value to be returned if there are no retained values that have
     *   exited composition and are being held by this RetainedValuesStore for the given [key].
     * @return A retained value for [key] if there is one and it hasn't already re-entered
     *   composition, otherwise [defaultIfAbsent].
     */
    public abstract fun getExitedValueOrDefault(key: Any, defaultIfAbsent: Any?): Any?

    /**
     * Invoked when a retained value is exiting composition while this store is retaining exited
     * values. It is up to the implementation of this method to decide whether and how to store
     * these values so that they can later be retrieved by [getExitedValueOrDefault].
     *
     * The given [key] are not guaranteed to be unique. To handle duplicate keys, implementors
     * should return retained values with the same keys from [getExitedValueOrDefault] in the
     * opposite order they are received by [saveExitingValue].
     *
     * If the implementation of this store does not accept this value into its kept exited object
     * list, it MUST call [RetainObserver.onRetired] if [value] implements [RetainObserver].
     */
    protected abstract fun saveExitingValue(key: Any, value: Any?)

    /**
     * Called to increment the number of requests to retain exited values. When there are a positive
     * number of requests, this store begins retaining exited values and continues until all
     * requests are cleared.
     *
     * This method is not thread safe and should only be called on the applier thread.
     */
    protected fun requestRetainExitedValues() {
        if (retainExitedValuesRequests++ == 0) {
            onStartRetainingExitedValues()
            observers.forEach { it.onStartRetainingExitedValues() }
        }
    }

    /**
     * Clears a previous call to [requestRetainExitedValues]. If all requests to retain exited
     * values have been cleared, this store will stop retaining exited values.
     *
     * This method is not thread safe and should only be called on the applier thread.
     *
     * @throws IllegalStateException if [unRequestRetainExitedValues] is called more times than
     *   [requestRetainExitedValues] has been called.
     */
    protected fun unRequestRetainExitedValues() {
        checkPrecondition(isRetainingExitedValues) {
            "Unexpected call to unRequestRetainExitedValues() without a " +
                "corresponding requestRetainExitedValues()"
        }
        if (--retainExitedValuesRequests == 0) {
            onStopRetainingExitedValues()
            observers.forEach { it.onStopRetainingExitedValues() }
        }
    }

    /**
     * Called when this store first starts to retain exited values (i.e. when
     * [isRetainingExitedValues] transitions from false to true). When this is called, implementors
     * should prepare to begin to store values they receive from [saveExitingValue].
     */
    protected abstract fun onStartRetainingExitedValues()

    /**
     * Called when this store stops retaining exited values (i.e. when [isRetainingExitedValues]
     * transitions from true to false). After this is called, all exited values that have been kept
     * and not restored via [getExitedValueOrDefault] should be retired.
     *
     * Implementors MUST invoke [RetainObserver.onRetired] for all exited and unrestored
     * [RememberObservers][RememberObserver] when this method is invoked.
     */
    protected abstract fun onStopRetainingExitedValues()

    final override fun addRetainStateObserver(observer: RetainStateObserver) {
        observers += observer
    }

    final override fun removeRetainStateObserver(observer: RetainStateObserver) {
        observers -= observer
    }

    internal class RetainedValueHolder<out T>
    internal constructor(
        val key: Any,
        val value: T,
        owner: RetainedValuesStore,
        private var isNewlyRetained: Boolean,
    ) : RememberObserver {

        var owner: RetainedValuesStore = owner
            private set

        init {
            if (value is RememberObserver && value !is RetainObserver) {
                throw IllegalArgumentException(
                    "Retained a value that implements RememberObserver but not RetainObserver. " +
                        "To receive the correct callbacks, the retained value '$value' must also " +
                        "implement RetainObserver."
                )
            }
        }

        internal fun readoptUnder(newStore: RetainedValuesStore) {
            owner = newStore
        }

        override fun onRemembered() {
            if (value is RetainObserver) {
                if (isNewlyRetained) {
                    isNewlyRetained = false
                    value.onRetained()
                }
                value.onEnteredComposition()
            }
        }

        override fun onForgotten() {
            if (owner.isRetainingExitedValues) {
                owner.saveExitingValue(key, value)
            }

            if (value is RetainObserver) {
                value.onExitedComposition()
                if (!owner.isRetainingExitedValues) value.onRetired()
            }
        }

        override fun onAbandoned() {
            if (owner.isRetainingExitedValues) {
                if (value is RetainObserver) value.onRetained()
                owner.saveExitingValue(key, value)
            } else if (value is RetainObserver) {
                value.onUnused()
            }
        }
    }
}

/**
 * [RetentionStateOwner] is an owner of the [isRetainingExitedValues] state used by
 * [RetainedValuesStore]. This interface is extracted to allow retain state to be observed without
 * the presence of the value storage. This is particularly useful as most [RetainedValuesStore]s
 * respect a hierarchy where they begin retaining exited values when either their retain condition
 * becomes true or their parent store begins retaining exited values.
 */
public interface RetainStateProvider {
    /**
     * Returns whether retained values should continue to be held when they are removed from the
     * composition hierarchy. This indicates that content is being destroyed transiently and that
     * the associated retention scenario of this [RetentionStateOwner] (e.g. navigation moving a
     * screen to the back stack, activity recreation, hidden UI, etc.) is currently active.
     *
     * When true, associated [RetainedValuesStore]s should continue to retain objects as they are
     * removed from the composition hierarchy for future reuse.
     */
    public val isRetainingExitedValues: Boolean

    /**
     * Registers the given [observer] with this [RetainStateProvider] to be notified when the value
     * of [isRetainingExitedValues] changes. The added observer will receive its first notification
     * the next time [isRetainingExitedValues] is updated.
     *
     * This method is not thread safe and should only be invoked on the applier thread.
     *
     * @see removeRetainStateObserver
     */
    public fun addRetainStateObserver(observer: RetainStateObserver)

    /**
     * Removes a previously registered [observer]. It will receive no further updates from this
     * [RetainStateProvider] unless it is registered again in the future. If the observer is not
     * currently registered, this this method does nothing.
     *
     * This method is not thread safe and should only be invoked on the applier thread.
     *
     * @see addRetainStateObserver
     */
    public fun removeRetainStateObserver(observer: RetainStateObserver)

    /**
     * Listener interface to observe changes in the value of
     * [RetainStateProvider.isRetainingExitedValues].
     *
     * @see RetainStateProvider.addRetainStateObserver
     * @see RetainStateProvider.removeRetainStateObserver
     */
    @Suppress("CallbackName")
    public interface RetainStateObserver {
        /**
         * Called to indicate that [RetainStateProvider.isRetainingExitedValues] has become `true`.
         * This callback should only be invoked on the applier thread.
         */
        public fun onStartRetainingExitedValues()

        /**
         * Called to indicate that [RetainStateProvider.isRetainingExitedValues] has become `false`.
         * This callback should only be invoked on the applier thread.
         */
        public fun onStopRetainingExitedValues()
    }

    /**
     * An implementation of [RetainStateProvider] that is not backed by a [RetainedValuesStore] and
     * is always set to retain exited values. This object is stateless and can be used to orphan a
     * nested [RetainedValuesStore] while maintaining it in a state where the store retains all
     * exited values.
     */
    @Stable
    public object AlwaysRetainExitedValues : RetainStateProvider {
        override val isRetainingExitedValues: Boolean
            get() = true

        override fun addRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }

        override fun removeRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }
    }

    /**
     * An implementation of [RetainStateProvider] that is not backed by a [RetainedValuesStore] and
     * is never set to retain exited values. This object is stateless and can be used to orphan a
     * nested [RetainedValuesStore] and clear any parent-driven state of [isRetainingExitedValues].
     */
    @Stable
    public object NeverRetainExitedValues : RetainStateProvider {
        override val isRetainingExitedValues: Boolean
            get() = false

        override fun addRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }

        override fun removeRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }
    }
}

/**
 * A [ControlledRetainedValuesStore] is effectively a "Mutable" [RetainedValuesStore]. This store
 * can be used to define a custom retain scenario and supports nesting within another
 * [RetainedValuesStore] via [setParentRetainStateProvider].
 *
 * This class can be used to create your own retention scenario. A retention scenario is a situation
 * in which content is transiently removed from the composition hierarchy and can be restored with
 * the retained values from the previous composition.
 *
 * When using this class to create your own retention scenario, call [startRetainingExitedValues] to
 * make this store start retaining exited values state before any content is transiently removed.
 * When the transiently removed content is restored, call [stopRetainingExitedValues] **after all
 * content has been restored**. You can use
 * [androidx.compose.runtime.Recomposer.scheduleFrameEndCallback] or
 * [androidx.compose.runtime.Composer.scheduleFrameEndCallback] to ensure that all content has
 * settled in subcompositions and movable content that may not be realized or applied in as part of
 * a composition that is currently ongoing.
 *
 * To create a [ControlledRetainedValuesStore] that is managed entirely within the composition
 * hierarchy, you can use [retainControlledRetainedValuesStore] to create a
 * [ControlledRetainedValuesStore] that is automatically parented to the current
 * [LocalRetainedValuesStore].
 */
public class ControlledRetainedValuesStore : RetainedValuesStore() {
    private val keptExitedValues = SafeMultiValueMap<Any, Any?>()

    private var parentStore: RetainStateProvider = NeverRetainExitedValues
    private val parentObserver =
        object : RetainStateObserver {
            override fun onStartRetainingExitedValues() {
                requestRetainExitedValues()
            }

            override fun onStopRetainingExitedValues() {
                unRequestRetainExitedValues()
            }
        }

    /**
     * Returns the number of calls to [startRetainingExitedValues] - [stopRetainingExitedValues].
     * Effectively, the total number of active requests to this [ControlledRetainedValuesStore].
     *
     * Note that this value **ignores any parent state.** It only counts explicit requests from the
     * user to [startRetainingExitedValues]. This store could still be retaining if this value is
     * `0` if the parent store is retaining. This is useful if you want to track your contributions
     * to this store's state, ignoring the parent.
     */
    public val retainExitedValuesRequestsFromSelf: Int
        get() = retainExitedValuesRequests - if (parentStore.isRetainingExitedValues) 1 else 0

    /**
     * Calling this function will automatically mirror the state of [isRetainingExitedValues] to
     * match [parent]'s state. This is an addition to requests made on the
     * [ControlledRetainedValuesStore], so retaining exited values is a function of whether the
     * parent is retaining exited values OR this store has been requested to retain exited values.
     *
     * A [ControlledRetainedValuesStore] can only have one parent. If a new parent is provided, it
     * will replace the old one and will match the new parent's [isRetainingExitedValues] state.
     * This may cause this store to start or stop retaining exited values if this store has no other
     * active requests from [startRetainingExitedValues].
     */
    public fun setParentRetainStateProvider(parent: RetainStateProvider) {
        val oldParent = parentStore
        parentStore = parent

        parent.addRetainStateObserver(parentObserver)
        oldParent.removeRetainStateObserver(parentObserver)

        if (parent.isRetainingExitedValues) startRetainingExitedValues()
        if (oldParent.isRetainingExitedValues) stopRetainingExitedValues()
    }

    /**
     * Indicates that this store should continue to retain values that exit the composition. If this
     * store is already in this mode, the store will not change states. The number of times this
     * function is called is tracked and must be matched by the same number of calls to
     * [stopRetainingExitedValues] before the kept values will be retired.
     */
    public fun startRetainingExitedValues(): Unit = requestRetainExitedValues()

    /**
     * Stops retaining values that exit the composition. This function cancels a request that
     * previously began by calling [startRetainingExitedValues]. If [startRetainingExitedValues] has
     * been called more than [stopRetainingExitedValues], the store will continue to retain values
     * that have exited the composition until [stopRetainingExitedValues] has been called the same
     * number of times as [startRetainingExitedValues].
     *
     * @throws IllegalStateException if [stopRetainingExitedValues] is called more times than
     *   [startRetainingExitedValues]
     */
    public fun stopRetainingExitedValues(): Unit = unRequestRetainExitedValues()

    override fun onStartRetainingExitedValues() {
        checkPrecondition(keptExitedValues.isEmpty()) {
            "Attempted to start retaining exited values with pending exited values"
        }
    }

    override fun onStopRetainingExitedValues() {
        keptExitedValues.forEachValue { value -> if (value is RetainObserver) value.onRetired() }
        keptExitedValues.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getExitedValueOrDefault(key: Any, defaultIfAbsent: Any?): Any? {
        return keptExitedValues.removeLast(key, defaultIfAbsent)
    }

    override fun saveExitingValue(key: Any, value: Any?) {
        keptExitedValues.add(key, value)
    }
}

/**
 * The ForgetfulRetainedValuesStore is an implementation of [RetainedValuesStore] that is incapable
 * of retaining any exited values. When installed as the [LocalRetainedValuesStore], all invocations
 * of [retain] will behave like a standard [remember]. [RetainObserver] callbacks are still
 * dispatched instead of [RememberObserver] callbacks, meaning that this class will always
 * immediately [retire][RetainObserver.onRetired] a value as soon as it exits composition.
 */
public object ForgetfulRetainedValuesStore : RetainedValuesStore() {
    override fun onStartRetainingExitedValues() {
        throw UnsupportedOperationException(
            "ForgetfulRetainedValuesStore can never retain exited values."
        )
    }

    override fun onStopRetainingExitedValues() {
        // Do nothing. This implementation never retains exited values.
    }

    override fun getExitedValueOrDefault(key: Any, defaultIfAbsent: Any?): Any? {
        return defaultIfAbsent
    }

    override fun saveExitingValue(key: Any, value: Any?) {
        throw UnsupportedOperationException(
            "ForgetfulRetainedValuesStore can never retain exited values."
        )
    }
}

/**
 * `RetainedContentHost` is used to install a [RetainedValuesStore] around a block of [content]. The
 * installed `RetainedValuesStore` is managed such that the store will start to retain exited values
 * when [active] is false, and stop retaining exited values when [active] becomes true. See
 * [RetainedValuesStore.isRetainingExitedValues] for more information on this terminology.
 *
 * `RetainedContentHost` is designed as an out-of-the-box solution for managing content that's
 * controlled effectively by an if/else statement. The [content] provided to this lambda will render
 * when [active] is true, and be removed when [active] is false. If the content is hidden and then
 * shown again in this way, the installed RetainedValuesStore will restore all retained values from
 * the last time the content was shown.
 *
 * The managed RetainedValuesStore is _also_ retained. If this composable is removed while the
 * parent store is retaining its exited values, this store will be persisted so that it can be
 * restored in the future. If this composable is removed while its parent store is not retaining its
 * exited values, the store will be discarded and all its held values will be immediately retired.
 *
 * For this reason, when using this as a mechanism to retain values for content that is being shown
 * and hidden, this composable must be hoisted high enough so that it is not removed when the
 * content being retained is hidden.
 *
 * @param active Whether this host should compose its [content]. When this value is true, [content]
 *   will be rendered and the installed [RetainedValuesStore] will not retain exited values. When
 *   this value is false, [content] will stop being rendered and the installed [RetainedValuesStore]
 *   will collect and retain its exited values for future restoration.
 * @param content The content to render. Inside of this lambda, [LocalRetainedValuesStore] is set to
 *   the [RetainedValuesStore] managed by this composable.
 * @sample androidx.compose.runtime.retain.samples.retainedContentHostSample
 * @see retainControlledRetainedValuesStore
 */
@Composable
public fun RetainedContentHost(active: Boolean, content: @Composable () -> Unit) {
    val retainedValuesStore = retainControlledRetainedValuesStore()
    if (active) {
        CompositionLocalProvider(LocalRetainedValuesStore provides retainedValuesStore, content)

        // Match the isRetainingExitedValues state to the active parameter. This effect must come
        // AFTER the content to correctly capture values.
        val composer = currentComposer
        DisposableEffect(retainedValuesStore) {
            // Stop retaining exited values when we become active. Use the request count to only
            // look at our state and to ignore any parent-influenced requests.
            val cancellationHandle =
                if (retainedValuesStore.retainExitedValuesRequestsFromSelf > 0) {
                    composer.scheduleFrameEndCallback {
                        retainedValuesStore.stopRetainingExitedValues()
                    }
                } else {
                    null
                }

            onDispose {
                // Start retaining exited values when we deactivate
                cancellationHandle?.cancel()
                retainedValuesStore.startRetainingExitedValues()
            }
        }
    }
}

/**
 * Retains a [ControlledRetainedValuesStore] that is nested under the current
 * [LocalRetainedValuesStore] and has no other defined retention scenarios.
 *
 * A [ControlledRetainedValuesStore] created in this way will mirror the retention behavior of
 * [LocalRetainedValuesStore]. When the parent store begins retaining its values, the returned store
 * will receive a request to start retaining values as well. When the parent store stops retaining
 * values, that request is cleared.
 *
 * This API is available as a building block for other retain stores defined in composition. To
 * define your own retention scenario, call
 * [ControlledRetainedValuesStore.startRetainingExitedValues] and
 * [ControlledRetainedValuesStore.stopRetainingExitedValues] on the returned scope as appropriate.
 * You must also install this scope in the composition hierarchy by providing it as the value of
 * [LocalRetainedValuesStore].
 *
 * When this value stops being retained, it will automatically stop retaining exited values,
 * regardless of how many times [ControlledRetainedValuesStore.startRetainingExitedValues] was
 * called.
 *
 * @return A [ControlledRetainedValuesStore] nested under the [LocalRetainedValuesStore], ready to
 *   be installed in the composition hierarchy and be used to define a retention scenario.
 * @sample androidx.compose.runtime.retain.samples.retainControlledRetainedValuesStoreSample
 * @see RetainedContentHost
 */
@Composable
public fun retainControlledRetainedValuesStore(): ControlledRetainedValuesStore {
    val retainedValuesStore =
        retain { RetainControlledRetainedValuesStoreWrapper() }.retainedValuesStore

    val parentStore = LocalRetainedValuesStore.current
    DisposableEffect(parentStore) {
        retainedValuesStore.setParentRetainStateProvider(parentStore)
        onDispose {
            // Keep the parent's state until we get a new store. This lets us continue
            // retaining when the composition hierarchy is destroyed and this parent is removed.
            retainedValuesStore.setParentRetainStateProvider(
                if (parentStore.isRetainingExitedValues) {
                    AlwaysRetainExitedValues
                } else {
                    NeverRetainExitedValues
                }
            )
        }
    }

    return retainedValuesStore
}

private class RetainControlledRetainedValuesStoreWrapper : RetainObserver {
    val retainedValuesStore = ControlledRetainedValuesStore()

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
        // The retainedValuesStore has stopped being retained. Dispose it.
        retainedValuesStore.setParentRetainStateProvider(NeverRetainExitedValues)
        while (retainedValuesStore.isRetainingExitedValues) retainedValuesStore
            .stopRetainingExitedValues()
    }

    override fun onUnused() {
        // Need to clean up if the store is abandoned, in case our parent caused us
        // to initialize with kept values.
        onRetired()
    }
}

/**
 * Returns a [retain] instance of a new [RetainedValuesStoreRegistry]. A RetainedValuesStoreRegistry
 * is a container of [RetainedValuesStore]s that allows a parent composable to have children with
 * different retention lifecycles. See [RetainedValuesStoreRegistry] for more information on how to
 * use this class, including a sample.
 *
 * The returned provider will be parented to the [LocalRetainedValuesStore] at this point in the
 * composition hierarchy. If the [LocalRetainedValuesStore] is changed, the returned provider will
 * be re-parented to the new [LocalRetainedValuesStore]. When this invocation leaves composition, it
 * will continue retaining if its parent store was retaining. When this
 * [RetainedValuesStoreRegistry] is retired, its child stores will also be retired and the store
 * will be [disposed][RetainedValuesStoreRegistry.dispose].
 *
 * This method is intended to be used for managing retained values in composables that swap in and
 * out children arbitrarily.
 */
@Composable
public fun retainRetainedValuesStoreRegistry(): RetainedValuesStoreRegistry {
    val provider = retain { RetainedValuesStoreRegistryWrapper() }.retainedValuesStoreRegistry
    val parentStore = LocalRetainedValuesStore.current
    DisposableEffect(parentStore) {
        provider.setParentRetainStateProvider(parentStore)
        onDispose {
            provider.setParentRetainStateProvider(
                parent =
                    if (parentStore.isRetainingExitedValues) {
                        AlwaysRetainExitedValues
                    } else {
                        NeverRetainExitedValues
                    }
            )
        }
    }
    return provider
}

private class RetainedValuesStoreRegistryWrapper : RetainObserver {
    val retainedValuesStoreRegistry = RetainedValuesStoreRegistry()

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
        retainedValuesStoreRegistry.dispose()
    }

    override fun onUnused() {
        retainedValuesStoreRegistry.dispose()
    }
}

/**
 * A [RetainedValuesStoreRegistry] creates and manages [RetainedValuesStore] instances for
 * collections of items. This is desirable for components that swap in and out children where each
 * child should be able to retain values when it becomes removed from the composition hierarchy.
 *
 * To use this class, call [getOrCreateRetainedValuesStoreForChild] to instantiate the
 * [RetainedValuesStore] that should be installed for a given child content block. For automatic
 * installation and content tracking, wrap your content in [ProvideChildRetainedValuesStore].
 *
 * You can also install the managed [RetainedValuesStore]s manually by obtaining a
 * [RetainedValuesStore] with [getOrCreateRetainedValuesStoreForChild] and setting it as the
 * [LocalRetainedValuesStore] for your children's content. When a child is being removed, call
 * [startRetainingExitedValues] to begin the transient destruction phase of your retention scenario.
 * After the child has been added back to the composition, invoke [stopRetainingExitedValues] to
 * finalize the restoration of retained values.
 *
 * When a [RetainedValuesStoreRegistry] is no longer used, you must call [dispose] before the
 * provider is garbage collected. This ensures that all retained values are correctly retired.
 * Failure to do so may result in leaked memory from undispatched [RetainObserver.onRetired]
 * callbacks. Instances created by [retainRetainedValuesStoreRegistry] are automatically disposed
 * when the provider stops being retained.
 *
 * @sample androidx.compose.runtime.retain.samples.retainedValuesStoreRegistrySample
 */
public class RetainedValuesStoreRegistry() {
    private var isDisposed = false
    private val childStores = MutableScatterMap<Any?, ControlledRetainedValuesStore>()

    private var parent: RetainStateProvider = NeverRetainExitedValues
    private var isParentRetainingExitedValues = false
    private val parentObserver =
        object : RetainStateObserver {
            override fun onStartRetainingExitedValues() {
                setRetainExitedValues(true)
            }

            override fun onStopRetainingExitedValues() {
                setRetainExitedValues(false)
            }
        }

    /**
     * Starts retaining exited values for a child with the given [key]. If a [RetainedValuesStore]
     * has not been created for this key (because [getOrCreateRetainedValuesStoreForChild] was not
     * called for the key or it has been cleared with [clearChild] or [clearChildren]), then this
     * function does nothing. If the [RetainedValuesStore] for the given key is already retaining
     * exited values, the store will not change states. The number of times this function is called
     * is tracked and must be matched by the same number of calls to [stopRetainingExitedValues] for
     * the given key before its kept values will be retired.
     *
     * This function must be called **before** any content for the associated child is removed from
     * the composition hierarchy.
     *
     * @param key The key of the child to begin retention for
     */
    public fun startRetainingExitedValues(key: Any?) {
        val store = childStores[key] ?: return
        store.startRetainingExitedValues()
    }

    /**
     * Stops retaining exited values for a child with the given [key] as previously started by
     * [startRetainingExitedValues]. If the underlying store is not retaining because
     * [startRetainingExitedValues] has not been called, this function will throw an exception. If
     * no such retain store exists because it was cleared with [clearChild] or never created with
     * [getOrCreateRetainedValuesStoreForChild], this function will do nothing.
     *
     * If [startRetainingExitedValues] has been called more than [stopRetainingExitedValues], the
     * store will continue to retain values that have exited the composition until
     * [stopRetainingExitedValues] has been called the same number of times as
     * [startRetainingExitedValues].
     *
     * This function must be called **after** the completion of the frame in which the child content
     * is being restored to allow the restored child to re-consume all of its retained values. You
     * can use [androidx.compose.runtime.Recomposer.scheduleFrameEndCallback] or
     * [androidx.compose.runtime.Composer.scheduleFrameEndCallback] to insert a sufficient delay.
     *
     * @param key The key of the child to end retention for
     * @throws IllegalStateException if [startRetainingExitedValues] is called more times than
     *   [stopRetainingExitedValues] has been called for the given key
     */
    public fun stopRetainingExitedValues(key: Any?) {
        val store = childStores[key] ?: return
        checkPrecondition(store.externalRetainExitedValuesRequests >= 1) {
            "Unexpected call to stopRetainingExitedValues() without a " +
                "corresponding startRetainingExitedValues() for key $key"
        }
        store.stopRetainingExitedValues()
    }

    /**
     * Gets the total number of active requests from [startRetainingExitedValues] for the given
     * [key]. Effectively, this is the number of calls to [startRetainingExitedValues] minus the
     * number of calls to [stopRetainingExitedValues] for the given [key].
     *
     * This counter resets if [clearStore] is called for the given [key]. If the store has not been
     * created for [key] by [getOrCreateRetainedValuesStoreForChild], this function will return `0`.
     *
     * @param key the key of the child to look up
     * @return The number of active requests against the given child to retain exited values
     * @see ControlledRetainedValuesStore.retainExitedValuesRequestsFromSelf
     */
    public fun retainExitedValuesRequestsFor(key: Any?): Int {
        val store = childStores[key] ?: return 0
        return store.externalRetainExitedValuesRequests
    }

    // We manage the parent ourselves without wiring it up, because it is more efficient than
    // attaching a listener. Because of this, we need to manually subtract out the parent count.
    private val ControlledRetainedValuesStore.externalRetainExitedValuesRequests: Int
        get() = retainExitedValuesRequestsFromSelf - if (isParentRetainingExitedValues) 1 else 0

    /**
     * Installs child [content] that should be retained under the given [key].
     * [startRetainingExitedValues] and [stopRetainingExitedValues] and automatically called based
     * on the presence of this composable for the [key].
     *
     * When removed, this composable begins retaining exited values from the [content] lambda under
     * the given [key]. When added back to the composition hierarchy, the store will stop retaining
     * exited values once the composition completes and will release any unused values. The keys
     * used with this method should only be used once per [RetainedValuesStoreRegistry] in a
     * composition.
     *
     * This composable only attempts to manage the retention lifecycle for the [content] and [key]
     * pair. It will retain removed content indefinitely until [clearChild] or [clearChildren] is
     * invoked.
     *
     * @param key The child key associated with the given [content]. This key is used to identify
     *   the retention pool for objects [retained][retain] by the content composable.
     * @param content The composable content to compose with the [RetainedValuesStore] of the given
     *   [key]
     * @throws IllegalStateException if [dispose] has been called
     */
    @Composable
    public fun ProvideChildRetainedValuesStore(key: Any?, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalRetainedValuesStore provides getOrCreateRetainedValuesStoreForChild(key)
        ) {
            content()
            PresenceIndicator(key)
        }
    }

    /**
     * Indicates the presence of [key] in the composition hierarchy. When this composable is added,
     * [stopRetainingExitedValues] is called for the [key]. When this composable is removed,
     * [startRetainingExitedValues] will be called at the completion of the frame.
     *
     * **This composable must be placed such that it appears AFTER the composable content content
     * for [key] in a preorder traversal of the composition hierarchy.** Otherwise, the underlying
     * requests that start and stop retaining exited values may be scheduled in an incorrect order,
     * causing lost values. For example,
     * ```kotlin
     * // Correct ordering.
     * if (showA) {
     *     CompositionLocalProvider(
     *         LocalRetainedValuesStore provides getOrCreateRetainedValuesStoreForChild("A")
     *     ) {
     *         ContentA()
     *     }
     *     PresenceIndicator("A")
     * }
     *
     * // Incorrect ordering.
     * if (showB) {
     *     PresenceIndicator("B")
     *
     *     CompositionLocalProvider(
     *         LocalRetainedValuesStore provides getOrCreateRetainedValuesStoreForChild("B")
     *     ) {
     *         ContentB()
     *     }
     * }
     * ```
     */
    @Composable
    private fun PresenceIndicator(key: Any?) {
        val composer = currentComposer
        DisposableEffect(key) {
            // This key is entering the composition. End retention when the frame completes. Use
            // the request count to not attempt `stopRetainingExitedValues` when we initially enter
            // the composition.
            val endRetainHandle =
                if (retainExitedValuesRequestsFor(key) > 0) {
                    composer.scheduleFrameEndCallback { stopRetainingExitedValues(key) }
                } else {
                    null
                }
            onDispose {
                // This key is exiting the composition. Begin retaining now.
                endRetainHandle?.cancel()
                startRetainingExitedValues(key)
            }
        }
    }

    /**
     * Creates or returns a previously created [RetainedValuesStore] instance for the given [key].
     * The returned [RetainedValuesStore] will be managed by this provider. It will begin retaining
     * if the parent retain store starts retaining or if [startRetainingExitedValues] is called with
     * the same [key], and it will stop retaining with the parent retain store ends retaining and
     * there is no [startRetainingExitedValues] call without a corresponding
     * [stopRetainingExitedValues] call for the specified [key].
     *
     * The first time this function is called for a given [key], a new [RetainedValuesStore] is
     * created for the [key]. When this function is called for the same [key], it will return the
     * same [RetainedValuesStore] it originally returned. If a given [key]'s store is
     * [cleared][clearChild], then a new one will be created for it the next time it is requested
     * via this function.
     *
     * This function must be called before [startRetainingExitedValues] or
     * [stopRetainingExitedValues] is called for those two methods to have any effect on the
     * retention state for the given [key].
     *
     * @param key The [key] to return an existing [RetainedValuesStore] instance for, if one exists,
     *   or to create a new instance for
     * @return A [RetainedValuesStore] instance suitable to be installed as the
     *   [LocalRetainedValuesStore] for the child content with the specified [key]
     * @throws IllegalStateException if [dispose] has been called
     */
    public fun getOrCreateRetainedValuesStoreForChild(key: Any?): RetainedValuesStore {
        checkPrecondition(!isDisposed) {
            "Cannot get a RetainedValuesStore after a RetainedValuesStoreRegistry has been disposed."
        }

        return childStores.getOrPut(key) {
            ControlledRetainedValuesStore().apply {
                if (isParentRetainingExitedValues) startRetainingExitedValues()
            }
        }
    }

    /**
     * When a [RetainStateProvider] is set as the parent of a [RetainedValuesStoreRegistry], the
     * [RetainedValuesStoreRegistry] will mirror the retention state of the parent. If the parent
     * stops retaining, all children that have started retaining via [startRetainingExitedValues]
     * will continue being retained after the parent stops retaining.
     *
     * If this function is called twice, the new parent will replace the old parent. The new
     * parent's state is immediately applied to the child stores.
     *
     * To clear a parent, call this function and pass in either
     * [RetainStateProvider.AlwaysRetainExitedValues] or
     * [RetainStateProvider.NeverRetainExitedValues] depending on whether you want this store to
     * retain exited values in the absence of a parent.
     */
    public fun setParentRetainStateProvider(parent: RetainStateProvider) {
        val oldParent = this@RetainedValuesStoreRegistry.parent
        this@RetainedValuesStoreRegistry.parent = parent

        parent.addRetainStateObserver(parentObserver)
        oldParent.removeRetainStateObserver(parentObserver)

        setRetainExitedValues(parent.isRetainingExitedValues)
    }

    /**
     * Removes the [RetainedValuesStore] for the child with the given [key] from this
     * [RetainedValuesStoreRegistry]. If the key doesn't have an associated [RetainedValuesStore]
     * yet (either because it hasn't been created or has already been cleared), this function does
     * nothing.
     *
     * If the store being cleared is currently retaining exited values, it will stop as a result of
     * this call. If a child with the given [key] is currently in the composition hierarchy, its
     * retained values will not be persisted the next time the child content is destroyed. Orphaned
     * RetainedValuesStores will never begin retaining exited values, and the content will need to
     * be recreated with a new RetainedValuesStore before exited values will be kept again.
     *
     * If [getOrCreateRetainedValuesStoreForChild] is called again for the given [key], a new
     * [RetainedValuesStore] will be created and returned.
     *
     * @param key The key of the child content whose [RetainedValuesStore] should be discarded
     */
    public fun clearChild(key: Any?) {
        childStores.remove(key)?.let { clearStore(it) }
    }

    /**
     * Bulk removes all child stores for which the [predicate] returns true. This function follows
     * the same clearing rules as [clearChild].
     *
     * @param predicate The predicate to evaluate on all child keys in the
     *   [RetainedValuesStoreRegistry]. If the predicate returns `true` for a given key, it will be
     *   cleared. If the predicate returns `false` it will remain in the collection.
     * @see clearChild
     */
    public fun clearChildren(predicate: (key: Any?) -> Boolean) {
        childStores.removeIf { key, store -> predicate(key).also { if (it) clearStore(store) } }
    }

    private fun clearStore(store: ControlledRetainedValuesStore) {
        while (store.retainExitedValuesRequestsFromSelf > 0) store.stopRetainingExitedValues()
    }

    /**
     * Removes all child [RetainedValuesStore]s from this [RetainedValuesStoreRegistry] and marks it
     * as ineligible for future use. This is required to invoke when the store is no longer used to
     * retire any retained values. Failing to do so may result in memory leaks from undispatched
     * [RetainObserver.onRetired] and [RetainedEffect] callbacks. When this function is called, all
     * values retained in stores managed by this provider will be immediately retired.
     *
     * If this store has already been disposed, this function will do nothing.
     */
    public fun dispose() {
        isDisposed = true
        clearChildren { true }
    }

    private fun setRetainExitedValues(shouldRetain: Boolean) {
        if (shouldRetain == isParentRetainingExitedValues) return
        isParentRetainingExitedValues = shouldRetain

        if (shouldRetain) {
            childStores.forEachValue { store -> store.startRetainingExitedValues() }
        } else {
            childStores.forEachValue { store -> store.stopRetainingExitedValues() }
        }
    }
}

/**
 * Represents all identifying parameters passed into [retain]. Implementations of
 * [RetainedValuesStore] are given these keys to identify instances of a [retain] invocation.
 *
 * These keys should not be introspected.
 */
@Stable
private class RetainKeys(
    private val keys: Array<out Any?>?,
    val positionalKey: CompositeKeyHashCode,
    val typeHash: Int,
) {

    override fun equals(other: Any?): Boolean {
        return other is RetainKeys &&
            other.positionalKey == this.positionalKey &&
            other.typeHash == this.typeHash &&
            other.keys.contentEquals(this.keys)
    }

    override fun hashCode(): Int {
        var result = keys?.contentHashCode() ?: 0
        result = 31 * result + positionalKey.hashCode()
        result = 31 * result + typeHash.hashCode()
        return result
    }
}
