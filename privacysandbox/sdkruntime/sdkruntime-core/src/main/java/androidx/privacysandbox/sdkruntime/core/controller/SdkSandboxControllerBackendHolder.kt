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

package androidx.privacysandbox.sdkruntime.core.controller

import android.os.Bundle
import android.os.IBinder
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat.SandboxControllerImpl
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature.SDK_SANDBOX_CONTROLLER_BACKEND_HOLDER
import java.util.concurrent.Executor
import org.jetbrains.annotations.TestOnly

/** Holds client provided implementation of [SdkSandboxControllerBackend]. */
@RestrictTo(LIBRARY_GROUP)
public object SdkSandboxControllerBackendHolder {
    public var LOCAL_BACKEND: SdkSandboxControllerBackend? = null

    /**
     * Inject backend from client library. Implementation will be used only if loaded locally. This
     * method will be called from client side via reflection during loading SDK.
     */
    @JvmStatic
    @Keep
    public fun injectLocalBackend(backend: SdkSandboxControllerBackend) {
        check(LOCAL_BACKEND == null) { "Local backend already injected" }

        LOCAL_BACKEND = backend
    }

    /**
     * Inject legacy backend from client library. Should be used only when SDK loaded by
     * sdkruntime-client without [SDK_SANDBOX_CONTROLLER_BACKEND_HOLDER]
     */
    internal fun injectLegacyImpl(legacyImpl: SandboxControllerImpl) {
        injectLocalBackend(LegacyBackend(legacyImpl))
    }

    @TestOnly
    public fun resetLocalBackend() {
        LOCAL_BACKEND = null
    }

    /**
     * When SDK loaded by old version of client library, converts legacy [SandboxControllerImpl] to
     * [SdkSandboxControllerBackend].
     */
    private class LegacyBackend(private val legacyImpl: SandboxControllerImpl) :
        SdkSandboxControllerBackend {

        override fun loadSdk(
            sdkName: String,
            params: Bundle,
            executor: Executor,
            callback: LoadSdkCallback,
        ): Unit = legacyImpl.loadSdk(sdkName, params, executor, callback)

        override fun getSandboxedSdks(): List<SandboxedSdkCompat> = legacyImpl.getSandboxedSdks()

        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
            legacyImpl.getAppOwnedSdkSandboxInterfaces()

        override fun registerSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        ): IBinder = legacyImpl.registerSdkSandboxActivityHandler(handlerCompat)

        override fun unregisterSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        ): Unit = legacyImpl.unregisterSdkSandboxActivityHandler(handlerCompat)

        override fun getClientPackageName(): String = legacyImpl.getClientPackageName()

        override fun registerSdkSandboxClientImportanceListener(
            executor: Executor,
            listenerCompat: SdkSandboxClientImportanceListenerCompat,
        ): Unit = legacyImpl.registerSdkSandboxClientImportanceListener(executor, listenerCompat)

        override fun unregisterSdkSandboxClientImportanceListener(
            listenerCompat: SdkSandboxClientImportanceListenerCompat
        ): Unit = legacyImpl.unregisterSdkSandboxClientImportanceListener(listenerCompat)
    }
}
