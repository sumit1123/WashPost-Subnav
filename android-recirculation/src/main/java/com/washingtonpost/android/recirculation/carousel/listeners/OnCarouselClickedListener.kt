/*
 * Copyright (c) 2018. The Washington Post
 */
package com.washingtonpost.android.recirculation.carousel.listeners

/**
 * Created by adkinsj on 8/13/18.
 */
interface OnCarouselClickedListener {
    fun onCardClicked(url: String?, positionInCarousel: Int)
}