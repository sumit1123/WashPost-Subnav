/*
 * Copyright (c) 2018. The Washington Post
 */
package com.washingtonpost.android.recirculation.carousel.listeners

import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader

/**
 * Created by adkinsj on 9/19/18.
 */
interface CarouselProvider {
    fun makeImageRequest(url: String?, maxWidth: Int, maxHeight: Int, listener: OnCarouselImageLoadedListener)
    fun makeLiveImageRequest(url: String?, maxWidth: Int, maxHeight: Int, listener: OnCarouselImageLoadedListener)
    fun getImageLoader() : AnimatedImageLoader
    fun tearDown()
}