/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.activity

import android.os.Build
import android.window.OnBackInvokedDispatcher
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventCallback
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationInputHandler

/**
 * Dispatcher that can be used to register [OnBackPressedCallback] instances for handling the
 * [ComponentActivity.onBackPressed] callback via composition.
 *
 * ```
 * class FormEntryFragment : Fragment() {
 *   override fun onAttach(context: Context) {
 *     super.onAttach(context)
 *     val callback = object : OnBackPressedCallback(
 *       true // default to enabled
 *     ) {
 *       override fun handleOnBackPressed() {
 *         showAreYouSureDialog()
 *       }
 *     }
 *     requireActivity().onBackPressedDispatcher.addCallback(
 *       this, // LifecycleOwner
 *       callback
 *     )
 *   }
 * }
 * ```
 *
 * When constructing an instance of this class, the [fallbackOnBackPressed] can be set to receive a
 * callback if [onBackPressed] is called when [hasEnabledCallbacks] returns `false`.
 */
// Implementation/API compatibility note: previous releases included only the Runnable? constructor,
// which permitted both first-argument and trailing lambda call syntax to specify
// fallbackOnBackPressed. To avoid silently breaking source compatibility the new
// primary constructor has no optional parameters to avoid ambiguity/wrong overload resolution
// when a single parameter is provided as a trailing lambda.
class OnBackPressedDispatcher
constructor(
    private val fallbackOnBackPressed: Runnable?,
    private val onHasEnabledCallbacksChanged: Consumer<Boolean>?
) {
    private val onBackPressedCallbacks = ArrayDeque<OnBackPressedCallback>()
    private var inProgressCallback: OnBackPressedCallback? = null
    private var eventDispatcher: NavigationEventDispatcher? = null
    private var hasEnabledCallbacks = false

    private val eventCallback: NavigationEventCallback =
        object : NavigationEventCallback(hasEnabledCallbacks) {
            override fun onEventStarted(event: NavigationEvent) {
                onBackStarted(BackEventCompat(event))
            }

            override fun onEventProgressed(event: NavigationEvent) {
                onBackProgressed(BackEventCompat(event))
            }

            override fun onEventCompleted() {
                onBackPressed()
            }

            override fun onEventCancelled() {
                onBackCancelled()
            }
        }

    @JvmOverloads
    constructor(fallbackOnBackPressed: Runnable? = null) : this(fallbackOnBackPressed, null)

    /**
     * Sets the [OnBackInvokedDispatcher] for handling system back for Android SDK T+.
     *
     * @param invoker the OnBackInvokedDispatcher to be set on this dispatcher
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setOnBackInvokedDispatcher(invoker: OnBackInvokedDispatcher) {
        if (eventDispatcher == null) {
            // To preserve existing behavior, if the NavigationEventDispatcher hasn't been set yet,
            // we initialize a stub dispatcher. In most cases, this won't be needed.
            setNavigationEventDispatcher(NavigationEventDispatcher(null))
        }
        // Since OnBackInvokedDispatcher is handled inside NavigationInputHandler, we create a stub
        // input handler to ensure the dispatcher is properly configured and behavior remains
        // consistent with the current production behaviour.
        NavigationInputHandler(eventDispatcher!!).setOnBackInvokedDispatcher(invoker)
    }

    fun setNavigationEventDispatcher(eventDispatcher: NavigationEventDispatcher) {
        this.eventDispatcher = eventDispatcher
        eventDispatcher.addCallback(eventCallback)
    }

    private fun updateEnabledCallbacks() {
        val hadEnabledCallbacks = hasEnabledCallbacks
        val hasEnabledCallbacks = onBackPressedCallbacks.any { it.isEnabled }
        this.hasEnabledCallbacks = hasEnabledCallbacks
        if (hasEnabledCallbacks != hadEnabledCallbacks) {
            onHasEnabledCallbacksChanged?.accept(hasEnabledCallbacks)
            eventCallback.isEnabled = hasEnabledCallbacks
        }
    }

    /**
     * Add a new [OnBackPressedCallback]. Callbacks are invoked in the reverse order in which they
     * are added, so this newly added [OnBackPressedCallback] will be the first callback to receive
     * a callback if [onBackPressed] is called.
     *
     * This method is **not** [Lifecycle] aware - if you'd like to ensure that you only get
     * callbacks when at least [started][Lifecycle.State.STARTED], use [addCallback]. It is expected
     * that you call [OnBackPressedCallback.remove] to manually remove your callback.
     *
     * @param onBackPressedCallback The callback to add
     * @see onBackPressed
     */
    @MainThread
    fun addCallback(onBackPressedCallback: OnBackPressedCallback) {
        addCancellableCallback(onBackPressedCallback)
    }

    /**
     * Internal implementation of [addCallback] that gives access to the [AutoCloseable] that
     * specifically removes this callback from the dispatcher without relying on
     * [OnBackPressedCallback.remove] which is what external developers should be using.
     *
     * @param onBackPressedCallback The callback to add
     * @return a [AutoCloseable] which can be used to `close()` the callback and remove it from the
     *   set of OnBackPressedCallbacks.
     */
    @MainThread
    internal fun addCancellableCallback(
        onBackPressedCallback: OnBackPressedCallback,
    ): AutoCloseable {
        onBackPressedCallbacks.add(onBackPressedCallback)
        val closeable = OnBackPressedCloseable(onBackPressedCallback)
        onBackPressedCallback.addCloseable(closeable)
        updateEnabledCallbacks()
        onBackPressedCallback.enabledChangedCallback = ::updateEnabledCallbacks
        return closeable
    }

    /**
     * Receive callbacks to a new [OnBackPressedCallback] when the given [LifecycleOwner] is at
     * least [started][Lifecycle.State.STARTED].
     *
     * This will automatically call [addCallback] and remove the callback as the lifecycle state
     * changes. As a corollary, if your lifecycle is already at least
     * [started][Lifecycle.State.STARTED], calling this method will result in an immediate call to
     * [addCallback].
     *
     * When the [LifecycleOwner] is [destroyed][Lifecycle.State.DESTROYED], it will automatically be
     * removed from the list of callbacks. The only time you would need to manually call
     * [OnBackPressedCallback.remove] is if you'd like to remove the callback prior to destruction
     * of the associated lifecycle.
     *
     * If the Lifecycle is already [destroyed][Lifecycle.State.DESTROYED] when this method is
     * called, the callback will not be added.
     *
     * @param owner The LifecycleOwner which controls when the callback should be invoked
     * @param onBackPressedCallback The callback to add
     * @see onBackPressed
     */
    @MainThread
    fun addCallback(owner: LifecycleOwner, onBackPressedCallback: OnBackPressedCallback) {
        val lifecycle = owner.lifecycle
        if (lifecycle.currentState === Lifecycle.State.DESTROYED) {
            return
        }
        onBackPressedCallback.addCloseable(
            closeable = LifecycleOnBackPressedCloseable(lifecycle, onBackPressedCallback)
        )
        updateEnabledCallbacks()
        onBackPressedCallback.enabledChangedCallback = ::updateEnabledCallbacks
    }

    /**
     * Returns `true` if there is at least one [enabled][OnBackPressedCallback.isEnabled] callback
     * registered with this dispatcher.
     *
     * @return True if there is at least one enabled callback.
     */
    @MainThread fun hasEnabledCallbacks(): Boolean = hasEnabledCallbacks

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackStarted(backEvent: BackEventCompat) {
        onBackStarted(backEvent)
    }

    @MainThread
    private fun onBackStarted(backEvent: BackEventCompat) {
        val callback = onBackPressedCallbacks.lastOrNull { it.isEnabled }
        if (inProgressCallback != null) {
            onBackCancelled()
        }
        inProgressCallback = callback
        if (callback != null) {
            callback.handleOnBackStarted(backEvent)
            return
        }
    }

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackProgressed(backEvent: BackEventCompat) {
        onBackProgressed(backEvent)
    }

    @MainThread
    private fun onBackProgressed(backEvent: BackEventCompat) {
        val callback = inProgressCallback ?: onBackPressedCallbacks.lastOrNull { it.isEnabled }
        if (callback != null) {
            callback.handleOnBackProgressed(backEvent)
            return
        }
    }

    /**
     * Trigger a call to the currently added [callbacks][OnBackPressedCallback] in reverse order in
     * which they were added. Only if the most recently added callback is not
     * [enabled][OnBackPressedCallback.isEnabled] will any previously added callback be called.
     *
     * If [hasEnabledCallbacks] is `false` when this method is called, the [fallbackOnBackPressed]
     * set by the constructor will be triggered.
     */
    @MainThread
    fun onBackPressed() {
        val callback = inProgressCallback ?: onBackPressedCallbacks.lastOrNull { it.isEnabled }
        inProgressCallback = null
        if (callback != null) {
            callback.handleOnBackPressed()
            return
        }
        fallbackOnBackPressed?.run()
    }

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackCancelled() {
        onBackCancelled()
    }

    @MainThread
    private fun onBackCancelled() {
        val callback = inProgressCallback ?: onBackPressedCallbacks.lastOrNull { it.isEnabled }
        inProgressCallback = null
        if (callback != null) {
            callback.handleOnBackCancelled()
            return
        }
    }

    private inner class OnBackPressedCloseable(
        private val onBackPressedCallback: OnBackPressedCallback
    ) : AutoCloseable {
        override fun close() {
            onBackPressedCallbacks.remove(onBackPressedCallback)
            if (inProgressCallback == onBackPressedCallback) {
                onBackPressedCallback.handleOnBackCancelled()
                inProgressCallback = null
            }
            onBackPressedCallback.removeCloseable(closeable = this)
            onBackPressedCallback.enabledChangedCallback?.invoke()
            onBackPressedCallback.enabledChangedCallback = null
        }
    }

    private inner class LifecycleOnBackPressedCloseable(
        private val lifecycle: Lifecycle,
        private val onBackPressedCallback: OnBackPressedCallback
    ) : LifecycleEventObserver, AutoCloseable {
        private var currentCloseable: AutoCloseable? = null

        init {
            lifecycle.addObserver(observer = this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event === Lifecycle.Event.ON_START) {
                currentCloseable = addCancellableCallback(onBackPressedCallback)
            } else if (event === Lifecycle.Event.ON_STOP) {
                // Should always be non-null
                currentCloseable?.close()
            } else if (event === Lifecycle.Event.ON_DESTROY) {
                close()
            }
        }

        override fun close() {
            lifecycle.removeObserver(observer = this)
            onBackPressedCallback.removeCloseable(closeable = this)
            currentCloseable?.close()
            currentCloseable = null
        }
    }
}

/**
 * Create and add a new [OnBackPressedCallback] that calls [onBackPressed] in
 * [OnBackPressedCallback.handleOnBackPressed].
 *
 * If an [owner] is specified, the callback will only be added when the Lifecycle is
 * [androidx.lifecycle.Lifecycle.State.STARTED].
 *
 * A default [enabled] state can be supplied.
 */
@Suppress("RegistrationName")
fun OnBackPressedDispatcher.addCallback(
    owner: LifecycleOwner? = null,
    enabled: Boolean = true,
    onBackPressed: OnBackPressedCallback.() -> Unit
): OnBackPressedCallback {
    val callback =
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
    if (owner != null) {
        addCallback(owner, callback)
    } else {
        addCallback(callback)
    }
    return callback
}
