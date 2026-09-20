package com.wapo.zendesk.notification

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner


/**
 * Activity lifecycle observer that automatically posts an ongoing feedback notification
 */
class ZendeskActivityObserver(private val iconId: Int) :
    DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (owner is Activity) {
            ZendeskFeedbackNotification.displayFeedbackNotification(
                owner, iconId, Intent(owner, owner::class.java).apply {
                    putExtra(ZendeskFeedbackNotification.EXTRA_BETA_FEEDBACK, true)
                    flags = FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }
    }
}
