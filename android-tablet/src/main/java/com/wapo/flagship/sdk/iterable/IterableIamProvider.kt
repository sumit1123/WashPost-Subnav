// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable

import com.wapo.android.commons.util.Logger
import com.wapo.android.push.PushService

class IterableIamProvider(
    private val iterableSdk: IterableSdk
) : PushService.IamProvider {

    override fun pauseInAppAutomation(pause: Boolean) {
        Logger.d(TAG, "Iterable, InAppMessage, pauseInAppAutomation, pause=$pause")
        if (pause)
            iterableSdk.pauseIamMessages()
        else
            iterableSdk.resumeIamMessages()
    }

    companion object {
        private const val TAG = "IterableIamProvider"
    }
}
