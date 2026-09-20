package com.wapo.animation

import android.view.ViewGroup

fun ViewGroup.fadeOut(duration: Long = 100L, onComplete: (() -> Unit)? = null) {
    this
        .animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction(onComplete)
}

fun ViewGroup.slideFromLeft(duration: Long = 100L, onComplete: (() -> Unit)? = null) {
    translationX = -width.toFloat()
    this
        .animate()
        .setDuration(duration)
        .translationX(0f)
        .withEndAction(onComplete)
}