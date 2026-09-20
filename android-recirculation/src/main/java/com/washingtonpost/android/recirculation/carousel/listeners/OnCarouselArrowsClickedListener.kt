/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.listeners

/**
 * Interface methods for the arrows on the carousels.
 * Application view classes can implement the interface while initiating the
 * [com.washingtonpost.android.recirculation.carousel.views.CarouselView] class
 */
interface OnCarouselArrowsClickedListener {
    fun onForwardArrowClicked()
    fun onBackwardArrowClicked()
}