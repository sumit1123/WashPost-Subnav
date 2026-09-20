package com.wapo.zendesk.notification

import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * Application observer that removes ongoing beta notification when the app stops
 */
class ZendeskApplicationObserver(private val appContext: Context) : LifecycleObserver {

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onApplicationStop() {
        ZendeskFeedbackNotification.removeNotification(appContext)
    }
}