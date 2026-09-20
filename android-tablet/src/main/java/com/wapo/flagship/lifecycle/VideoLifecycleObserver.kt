package com.wapo.flagship.lifecycle

import androidx.lifecycle.Lifecycle
import com.wapo.flagship.FlagshipApplication

class VideoLifecycleObserver(
    lifecycle: Lifecycle,
) : FlagshipLifecycleObserver(lifecycle) {
    override fun onApplicationPause() {
        super.onApplicationPause()
        if (FlagshipApplication.getInstance().videoManager.isBeingShared) {
            FlagshipApplication.getInstance().videoManager.isBeingShared = false
        } else {
            FlagshipApplication.getInstance().releaseVideoManager()
        }
    }
}
