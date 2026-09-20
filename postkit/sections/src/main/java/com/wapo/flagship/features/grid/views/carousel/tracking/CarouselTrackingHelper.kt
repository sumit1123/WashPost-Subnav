package com.wapo.flagship.features.grid.views.carousel.tracking

import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.UiUtils

abstract class CarouselTrackingHelper {

    private var onGlobalChangeListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var onWindowFocusChangeListener: ViewTreeObserver.OnWindowFocusChangeListener? = null
    private var attachStateListener: View.OnAttachStateChangeListener? = null

    fun bind(recyclerView: RecyclerView) {
        startObservingCarouselVisibility(recyclerView)
    }

    fun unbind(recyclerView: RecyclerView) {
        removeListeners(recyclerView)
        attachStateListener?.let {
            recyclerView.removeOnAttachStateChangeListener(it)
        }
        attachStateListener = null
    }

    private fun startObservingCarouselVisibility(recyclerView: RecyclerView) {
        setupObservers(recyclerView)
    }

    private fun setupObservers(recyclerView: RecyclerView) {
        attachStateListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                unbind(recyclerView)
            }
        }.also {
            recyclerView.addOnAttachStateChangeListener(it)
        }

        recyclerView.viewTreeObserver?.let { viewTreeObserver ->
            onGlobalChangeListener = ViewTreeObserver.OnGlobalLayoutListener {
                if (UiUtils.isViewHeightFullyVisibleOnScreen(recyclerView)) {
                    trackCarouselSeen()
                    if (recyclerView.viewTreeObserver.isAlive) {
                        recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalChangeListener)
                    }
                    onGlobalChangeListener = null
                }
            }

            onWindowFocusChangeListener = ViewTreeObserver.OnWindowFocusChangeListener { gainingFocus ->
                if (gainingFocus) {
                    if (UiUtils.isViewHeightFullyVisibleOnScreen(recyclerView)) {
                        trackCarouselSeen()
                    } else {
                        resetGlobalLayoutObserver(recyclerView)
                    }
                }
            }

            viewTreeObserver.apply {
                addOnGlobalLayoutListener(onGlobalChangeListener)
                addOnWindowFocusChangeListener(onWindowFocusChangeListener)
            }
        }
    }

    private fun resetGlobalLayoutObserver(recyclerView: RecyclerView) {
        val viewTreeObserver = recyclerView.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnGlobalLayoutListener(onGlobalChangeListener)
        }

        onGlobalChangeListener = ViewTreeObserver.OnGlobalLayoutListener {
            /** Track xxxx_carousel_seen event once the carousel's recyclerview is fully visible on the screen */
            if (UiUtils.isViewHeightFullyVisibleOnScreen(recyclerView)) {
                trackCarouselSeen()
                // Since this should only be triggered once while scrolling on the section front, we remove this listener.
                if (recyclerView.viewTreeObserver.isAlive) {
                    recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalChangeListener)
                }
            }
        }

        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(onGlobalChangeListener)
        }
    }

    private fun removeListeners(recyclerView: RecyclerView) {
        val vto = recyclerView.viewTreeObserver
        if (vto.isAlive) {
            onGlobalChangeListener?.let { vto.removeOnGlobalLayoutListener(it) }
            onWindowFocusChangeListener?.let { vto.removeOnWindowFocusChangeListener(it) }
        }
        onGlobalChangeListener = null
        onWindowFocusChangeListener = null
    }

    /**
     * Override this function to fire carousel specific seen event. ie immersion_carousel_seen, audio_carousel_seen, etc.
     */
    abstract fun trackCarouselSeen()
}