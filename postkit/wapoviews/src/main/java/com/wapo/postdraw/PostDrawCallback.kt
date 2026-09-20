package com.wapo.postdraw

import android.view.Choreographer
import android.view.View
import android.view.ViewTreeObserver

/**
 * Register a function to be called after the first frame is drawn and the TTID is reported.
 * The function is only called once.
 */
fun View.onFirstPostDraw(onPostDraw: () -> Unit) {
    this.viewTreeObserver.addOnPreDrawListener(PostDrawCallback(this, onPostDraw))
}

private class PostDrawCallback(val view: View, val onPostDraw: () -> Unit) : ViewTreeObserver.OnPreDrawListener {

    private var firstFrameDrawn = false

    override fun onPreDraw(): Boolean {
        if (firstFrameDrawn) return true

        Choreographer.getInstance().postFrameCallback {
            onPostDraw()
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }

        firstFrameDrawn = true
        return true
    }
}