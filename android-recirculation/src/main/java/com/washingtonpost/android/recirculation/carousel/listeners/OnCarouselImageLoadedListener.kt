/*
 * Copyright (c) 2018. The Washington Post
 */
package com.washingtonpost.android.recirculation.carousel.listeners

import android.graphics.Bitmap

/**
 * Created by adkinsj on 8/13/18.
 */
interface OnCarouselImageLoadedListener {
    /**
     * Success response uses this method.
     */
    fun onBitmapLoaded(bitmap: Bitmap)
    /**
     * Success response can use this method to display bitmap with or without an animation .
     */
    fun onBitmapLoaded(bitmap: Bitmap, animate: Boolean) {}
    /**
     * Failure cases uses this method.
     */
    fun onBitmapError(bitmap: Bitmap?)
    /**
     * LowDataModeEnable.
     */
    fun onLowDataModeChange(isEnable: Boolean)
}