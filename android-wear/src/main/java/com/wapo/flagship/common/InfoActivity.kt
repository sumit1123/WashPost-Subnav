/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 *  [InfoActivity] customizes [androidx.wear.activity.ConfirmationActivity]
 *  to provide equivalence of mobile's dialogs on wear app.
 *
 *  This activity displays an overlay with a message, and then is self-finished
 *  after a specified duration.
 *
 */
class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra(MESSAGE)

        InfoOverlay()
            .setMessage(message)
            .setDuration(2000L)
            .setFinishedActivityListener(object: InfoOverlay.OnActivityFinishedListener {
                override fun onActivityFinished() {
                    onAnimationFinished()
                }
            })
            .showOn(this)
    }

    private fun onAnimationFinished() {
        finish()
    }

    companion object {
        const val MESSAGE = "com.wapo.flagship.common.extra.MESSAGE"

        fun show(activity: Activity, message: String)  {
            Intent(activity, InfoActivity::class.java).apply {
                putExtra(MESSAGE, message)
            }.also { activity.startActivity(it) }
        }
    }

}