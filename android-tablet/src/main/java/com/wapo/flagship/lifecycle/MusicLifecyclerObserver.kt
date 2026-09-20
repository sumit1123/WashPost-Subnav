/* Copyright (c) 2025 The Washington Post. All rights reserved. */

package com.wapo.flagship.lifecycle

import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.audio.service2.media.MusicService

class MusicLifecyclerObserver(lifecycle: Lifecycle) : FlagshipLifecycleObserver(lifecycle) {

    @OptIn(UnstableApi::class)
    override fun onApplicationStart() {
        super.onApplicationStart()
        val application = FlagshipApplication.getInstance()
        application.musicServiceConnection.init(application, ComponentName(application, MusicService::class.java))
    }

    override fun onApplicationDestroy() {
        super.onApplicationDestroy()
        FlagshipApplication.getInstance().releaseMusicService()
    }
}