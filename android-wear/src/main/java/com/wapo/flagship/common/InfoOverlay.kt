/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.common

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.MainThread
import com.washingtonpost.android.R

/**
 *  [InfoOverlay] is written based on [androidx.wear.widget.ConfirmationOverlay].
 *
 */
class InfoOverlay {

    interface OnActivityFinishedListener {
        /**
         * Called when the activity is finished.
         */
        fun onActivityFinished()
    }

    private var mDurationMillis = DEFAULT_DURATION_MS
    var mListener: OnActivityFinishedListener? = null
    private var mMessage: String? = null
    private var mOverlayView: View? = null
    private var mIsShowing = false

    private val mMainThreadHandler = Handler(Looper.getMainLooper())
    private val mFinishRunnable = Runnable { mListener?.onActivityFinished() }

    fun setMessage(message: String?): InfoOverlay {
        mMessage = message
        return this
    }

    fun setDuration(millis: Long): InfoOverlay {
        mDurationMillis = millis
        return this
    }

    fun setFinishedActivityListener(listener: OnActivityFinishedListener?): InfoOverlay {
        mListener = listener
        return this
    }

    @MainThread
    fun showOn(activity: Activity) {
        if (mIsShowing) return

        mIsShowing = true
        updateOverlayView(activity)
        activity.window.addContentView(mOverlayView, mOverlayView!!.layoutParams)
        mMainThreadHandler.postDelayed(mFinishRunnable, mDurationMillis)
    }

    @MainThread
    private fun updateOverlayView(context: Context) {
        if (mOverlayView == null) {
            mOverlayView = LayoutInflater
                .from(context)
                .inflate(R.layout.overlay_info, null)
        }
        mOverlayView?.apply {
            setOnTouchListener { v, event -> true }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            updateMessageView(this)
        }
    }

    @MainThread
    private fun updateMessageView(overlayView: View) {
        val messageView = overlayView.findViewById<TextView>(R.id.tv_message)

        if (mMessage != null) {
            messageView.text = mMessage
        } else {
            messageView.visibility  = View.GONE
        }
    }

    companion object {
        const val DEFAULT_DURATION_MS = 1000L
    }

}
