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

package androidx.privacysandbox.sdkruntime.integration.testsdk

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.integration.testaidl.ISdkActivityApi
import androidx.privacysandbox.sdkruntime.integration.testaidl.ISdkActivityHandler

/** Inflate SDK Activity and provide API for controlling it to client. */
class ActivityHandlerWrapper(
    private val sdkContext: Context,
    private val appSideActivityHandler: ISdkActivityHandler,
) : SdkSandboxActivityHandlerCompat {
    override fun onActivityCreated(activityHolder: ActivityHolder) {
        val sdkActivityApi = SdkActivityApi(activityHolder)
        inflateActivity(activityHolder.getActivity(), sdkActivityApi)
        appSideActivityHandler.onActivityCreated(sdkActivityApi)
    }

    private fun inflateActivity(activity: Activity, sdkActivityApi: SdkActivityApi) {
        val mainLayout = LayoutInflater.from(sdkContext).inflate(R.layout.sdk_activity_layout, null)

        setupFinishActivityButton(mainLayout, sdkActivityApi)

        activity.setContentView(mainLayout)
    }

    private fun setupFinishActivityButton(mainLayout: View, sdkActivityApi: SdkActivityApi) {
        val finishActivityButton = mainLayout.findViewById<Button>(R.id.finishActivityButton)
        finishActivityButton.setOnClickListener { sdkActivityApi.finishActivity() }
    }

    private class SdkActivityApi(private val activityHolder: ActivityHolder) :
        ISdkActivityApi.Stub() {
        override fun finishActivity() {
            activityHolder.getActivity().finish()
        }
    }
}
