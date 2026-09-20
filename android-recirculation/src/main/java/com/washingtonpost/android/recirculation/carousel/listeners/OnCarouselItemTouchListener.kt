/*
 * Copyright (c) 2022. The Washington Post
 */
package com.washingtonpost.android.recirculation.carousel.listeners

/**
 * An OnCarouselItemTouchListener allows the application to intercept
 * requestDisallowInterceptTouchEvent touch events in progress at the
 * view hierarchy level of the RecyclerView before those touch events are considered for
 * RecyclerView's own scrolling behavior.
 *
 * This can be useful for applications that wish to implement various forms of manipulation of
 * item views within the RecyclerView.
 */
interface OnCarouselItemTouchListener {
    fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean)
}