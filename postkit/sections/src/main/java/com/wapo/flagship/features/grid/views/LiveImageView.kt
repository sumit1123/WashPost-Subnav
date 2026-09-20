/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import com.wapo.android.commons.util.Logger
import androidx.appcompat.widget.AppCompatImageView
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.pagebuilder.holders.LiveImageRequestData
import com.washingtonpost.android.wapocontent.ILoader
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

/**
 * LiveImageView takes in an updating imageURL, makes a LiveImageRequest, and schedules a TimerTask
 * to refresh the image from the network at a given interval.
 */
class LiveImageView : AppCompatImageView {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)

    interface Listener {
        fun onLiveImageReady()
    }

    private val TAG: String = this.javaClass.simpleName
    private val DEFAULT_REFRESH_RATE: Long = 60 * 1000
    private lateinit var url: String
    private var refreshInterval: Long = DEFAULT_REFRESH_RATE
    private lateinit var timerTaskRunnable: Runnable
    private var timerTask: TimerTask? = null
    private var subscription: Subscription? = null
    private var imageService: ILoader? = null
    private var listener: Listener? = null

    fun setImage(url: String, imageService: ILoader, refreshInterval: Long, width: Int = Int.MAX_VALUE, height: Int = Int.MAX_VALUE) {
        this.url = url
        this.refreshInterval = refreshInterval
        this.imageService = imageService
        subscribe()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private fun makeImageRequest(url: String, imageService: ILoader, width: Int, height: Int): Subscription {
        val requestData = LiveImageRequestData(url, width, height)
        return imageService.getImage(requestData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                { imageResponse ->
                    if (imageResponse.data !is Bitmap) {
                        Logger.e(TAG, "Error updating LiveImageView from ${getImagePathFromURL(url)}: imageResponse.data is not a Bitmap")
                        return@subscribe
                    }
                    Logger.d(TAG, "Updating LiveImageView from ${getImagePathFromURL(url)}")
                    listener?.onLiveImageReady()
                    setImageBitmap(imageResponse.data as Bitmap)
                },
                { throwable ->
                    Logger.e(TAG, "Error updating LiveImageView from ${getImagePathFromURL(url)}", throwable)
                    setImageDrawable(null)
                    val gridEnvironment = context.findActivityOfType<GridActivity>()?.getGridEnvironment()
                    gridEnvironment?.let {
                        if (gridEnvironment.isConnected()) {
                            val imageTag = "$TAG + (url = $url)"
                            gridEnvironment.remoteLogError(imageTag, throwable)
                        }
                    }
                })

    }

    private fun startImageTimerTask() {
        val timer = Timer()
        timer.schedule(timerTask, 0, refreshInterval)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribe()
    }

    override fun onDetachedFromWindow() {
        if (this::url.isInitialized) {
            unsubscribe()
        }
        super.onDetachedFromWindow()
    }

    private fun subscribe() {
        var maxWidth = Int.MAX_VALUE
        var maxHeight = Int.MAX_VALUE
        if (subscription?.isUnsubscribed == false) return
        val imageService = imageService ?: return
        Logger.d(TAG, "Subscribing LiveImageView updates for $url because the view is attached")
        subscription = makeImageRequest(url, imageService, maxWidth, maxHeight)
        timerTaskRunnable = Runnable { makeImageRequest(url, imageService, maxWidth, maxHeight) }
        timerTask = object: TimerTask() {
            override fun run() {
                timerTaskRunnable.run()
            }
        }
        startImageTimerTask()
    }

    private fun unsubscribe() {
        Logger.d(TAG, "LiveImageView canceled its image refresh TimerTask for $url because the view is detached")
        timerTask?.cancel()
        timerTask = null
        subscription?.unsubscribe()
    }

    private fun getImagePathFromURL(url: String) : String {
        val uri = Uri.parse(url)
        return uri.path ?: url
    }
}