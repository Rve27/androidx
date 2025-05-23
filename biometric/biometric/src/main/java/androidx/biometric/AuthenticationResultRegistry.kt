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

package androidx.biometric

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.Executor

/**
 * A registry that stores [auth result callbacks][AuthenticationCallback] for
 * [registered calls][registerForAuthenticationResult].
 */
internal class AuthenticationResultRegistry(
    private var activity: FragmentActivity? = null,
    private var fragment: Fragment? = null,
) {
    fun register(
        resultCallback: AuthenticationResultCallback,
        callbackExecutor: Executor? = null,
    ): AuthenticationResultLauncher {
        val callback =
            object : AuthenticationCallback() {
                override fun onAuthenticationFailed() {
                    resultCallback.onAuthFailure()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    resultCallback.onAuthResult(
                        AuthenticationResult.Success(result.cryptoObject, result.authenticationType)
                    )
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    resultCallback.onAuthResult(AuthenticationResult.Error(errorCode, errString))
                }
            }

        var biometricPrompt: BiometricPrompt? = null

        val lifecycleContainer =
            LifecycleContainer(if (activity != null) activity!!.lifecycle else fragment!!.lifecycle)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                ON_START -> {
                    biometricPrompt =
                        if (activity != null) {
                            activity!!.createBiometricPrompt(callbackExecutor, callback)
                        } else {
                            fragment!!.createBiometricPrompt(callbackExecutor, callback)
                        }
                }
                ON_STOP -> {
                    // TODO(b/349213716): remove callback from BiometricViewModel
                }
                ON_DESTROY -> {
                    // TODO(b/349213716): remove callback from BiometricViewModel
                    lifecycleContainer.clearObservers()
                }
                else -> {}
            }
        }
        lifecycleContainer.addObserver(observer)

        return object : AuthenticationResultLauncher {
            override fun launch(input: AuthenticationRequest) {
                biometricPrompt?.let { onLaunch(input, it) }
            }

            override fun cancel() {
                biometricPrompt?.cancelAuthentication()
                lifecycleContainer.clearObservers()
            }
        }
    }

    private class LifecycleContainer(val lifecycle: Lifecycle) {
        private val observers = mutableListOf<LifecycleEventObserver>()

        fun addObserver(observer: LifecycleEventObserver) {
            lifecycle.addObserver(observer)
            observers.add(observer)
        }

        fun clearObservers() {
            observers.forEach { observer -> lifecycle.removeObserver(observer) }
            observers.clear()
        }
    }
}

private fun onLaunch(input: AuthenticationRequest, biometricPrompt: BiometricPrompt) {
    when (input) {
        is AuthenticationRequest.Biometric ->
            biometricPrompt.authInternal(
                title = input.title,
                subtitle = input.subtitle,
                content = input.content,
                logoBitmap = input.logoBitmap,
                logoRes = input.logoRes,
                logoDescription = input.logoDescription,
                minBiometricStrength = input.minStrength,
                isConfirmationRequired = input.isConfirmationRequired,
                authFallback = input.authFallback,
            )
        is AuthenticationRequest.Credential ->
            biometricPrompt.authInternal(
                title = input.title,
                subtitle = input.subtitle,
                content = input.content,
                cryptoObjectForCredentialOnly = input.cryptoObject,
            )
    }
}

/** Creates a [BiometricPrompt] with given [AuthenticationCallback] */
internal fun FragmentActivity.createBiometricPrompt(
    executor: Executor?,
    callback: AuthenticationCallback,
): BiometricPrompt {
    return if (executor == null) {
        BiometricPrompt(this, callback)
    } else {
        BiometricPrompt(this, executor, callback)
    }
}

/** Creates a [BiometricPrompt] with given [AuthenticationCallback] */
internal fun Fragment.createBiometricPrompt(
    executor: Executor?,
    callback: AuthenticationCallback,
): BiometricPrompt {
    return if (executor == null) {
        BiometricPrompt(this, callback)
    } else {
        BiometricPrompt(this, executor, callback)
    }
}

/** Shows the authentication prompt to the user with biometric and/or device credential. */
@SuppressLint("MissingPermission")
internal fun BiometricPrompt.authInternal(
    title: String,
    subtitle: String? = null,
    content: AuthenticationRequest.BodyContent? = null,
    logoBitmap: Bitmap? = null,
    logoRes: Int = 0,
    logoDescription: String? = null,
    minBiometricStrength: Biometric.Strength? = null,
    isConfirmationRequired: Boolean = true,
    cryptoObjectForCredentialOnly: CryptoObject? = null,
    authFallback: Biometric.Fallback? = null,
) {

    PromptInfo.Builder().apply {
        // Set authenticators and fallbacks
        var authType =
            minBiometricStrength?.toAuthenticationType()
                ?: BiometricManager.Authenticators.DEVICE_CREDENTIAL
        when (authFallback) {
            is Biometric.Fallback.DeviceCredential ->
                authType = authType or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            is Biometric.Fallback.NegativeButton ->
                setNegativeButtonText(authFallback.negativeButtonText)
        }
        setAllowedAuthenticators(authType)

        // Set body content
        when (content) {
            is AuthenticationRequest.BodyContent.PlainText -> setDescription(content.description)
            is AuthenticationRequest.BodyContent.VerticalList -> {
                PromptVerticalListContentView.Builder().apply {
                    content.items.forEach { addListItem(it) }
                    content.description?.let { setDescription(content.description) }
                    setContentView(build())
                }
            }
            is AuthenticationRequest.BodyContent.ContentViewWithMoreOptionsButton -> {
                PromptContentViewWithMoreOptionsButton.Builder().apply {
                    content.description?.let { setDescription(content.description) }
                    setContentView(build())
                }
            }
        }

        // Set logo
        if (logoRes != 0) {
            setLogoRes(logoRes)
        } else if (logoBitmap != null) {
            setLogoBitmap(logoBitmap)
        }
        if (logoDescription != null) {
            setLogoDescription(logoDescription)
        }

        // Set other configurations
        setTitle(title)
        setSubtitle(subtitle)
        setConfirmationRequired(isConfirmationRequired)

        val cryptoObject =
            when (minBiometricStrength) {
                is Biometric.Strength.Class3 -> minBiometricStrength.cryptoObject
                null -> cryptoObjectForCredentialOnly
                else -> null
            }

        if (cryptoObject == null) {
            authenticate(build())
        } else {
            authenticate(build(), cryptoObject)
        }
    }
}
