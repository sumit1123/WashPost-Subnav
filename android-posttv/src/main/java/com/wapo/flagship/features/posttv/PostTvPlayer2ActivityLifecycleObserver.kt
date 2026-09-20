/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv

import android.app.Activity
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.posttv.listeners.PiPActivity

/**
 * LifecycleObserver class to handle [PostTvPlayer2Coordinator]'s life cycle methods.
 * Any Activity can initialize this class, and then this class manages the players that are owned
 * by that activity using [PostTvPlayer2Coordinator] class.
 * Activities can still maintain player states without using this class. But this class simplifies that
 * management and brings consistent behavior across all the players that are maintained by the
 * [PostTvPlayer2Coordinator] class.
 * Skip pause and resume players if current Android sdk version is 26 and above and
 * current activity is a PiPActivity. PiPActivity pauses and resumes while it transitions.
 */
class PostTvPlayer2ActivityLifecycleObserver(val activity: Activity) : LifecycleObserver {

    private val tag = PostTvPlayer2ActivityLifecycleObserver::class.java.simpleName

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Logger.d(tag, "PostTvPlayer2, onResume(), activity=${activity.javaClass.simpleName}")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity !is PiPActivity) {
            PostTvPlayer2Coordinator.resumePlayers(activity)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Logger.d(tag, "PostTvPlayer2, onPause(), activity=${activity.javaClass.simpleName}")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity !is PiPActivity) {
            PostTvPlayer2Coordinator.pausePlayers(activity)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Logger.d(tag, "PostTvPlayer2, onDestroy(), activity=${activity.javaClass.simpleName}")
        PostTvPlayer2Coordinator.releasePlayers(activity)
    }
}