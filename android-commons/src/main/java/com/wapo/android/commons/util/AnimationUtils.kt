package com.wapo.android.commons.util

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd

enum class Property {
    ELEVATION,
    ALPHA,
    TRANSLATION_Y,
    TRANSLATION_X,
    SCALE,
    ROTATION,
    SIZEX,
    SIZEY
}

fun View.animateAlpha(
    valFrom: Float,
    valTo: Float,
    duration: Long = 200,
    delayVal: Long = 0,
    interpolator: android.view.animation.Interpolator = DecelerateInterpolator()
): Animator {
    this.alpha = valFrom
    return this.animateFloat(valFrom, valTo, duration, delayVal, interpolator, Property.ALPHA)
}

fun View.animateScale(
    valFrom: Float,
    valTo: Float,
    duration: Long = 200,
    delayVal: Long = 0,
    interpolator: android.view.animation.Interpolator = DecelerateInterpolator()
): Animator {
    this.scaleX = valFrom
    this.scaleY = valFrom
    return this.animateFloat(valFrom, valTo, duration, delayVal, interpolator, Property.SCALE)
}

fun View.animateRotate(
    valFrom: Float,
    valTo: Float,
    pivotX: Float = 0.5f,
    pivotY: Float = 0.5f,
    reset: Boolean = false,
    duration: Long = 200,
    delayVal: Long = 0,
    interpolator: android.view.animation.Interpolator = DecelerateInterpolator(),
    setPivot: Boolean = true,
): Animator {
    if (setPivot) {
        this.pivotX = pivotX
        this.pivotY = pivotY
    }
    if (reset) {
        this.rotation = valFrom
    }
    return this.animateFloat(valFrom, valTo, duration, delayVal, interpolator, Property.ROTATION)
}

fun View.animateSize(
    valXFrom: Float,
    valXTo: Float,
    valYFrom: Float,
    valYTo: Float,
    duration: Long = 200,
    delayVal: Long = 0,
    interpolator: android.view.animation.Interpolator = DecelerateInterpolator(),
    onComplete:()->Unit = {}
) {
    val animatorSet = AnimatorSet()
    val animatorX = this.animateFloat(valFrom = valXFrom, valTo =  valXTo, property = Property.SIZEX)
    val animatorY = this.animateFloat(valFrom = valYFrom, valTo =  valYTo, property = Property.SIZEY)
    animatorSet.playTogether(animatorX,animatorY)
    animatorSet.doOnEnd {
        onComplete()
    }
    animatorSet.start()
}

private fun View.animateFloat(
    valFrom: Float,
    valTo: Float,
    duration: Long = 200,
    delayVal: Long = 0,
    interpolator: android.view.animation.Interpolator = DecelerateInterpolator(),
    property: Property
): Animator {
    val animator = ValueAnimator.ofFloat(valFrom, valTo)
    animator.apply {
        this.interpolator = interpolator
        this.duration = duration
        this.startDelay = delayVal
    }.addUpdateListener {
        this.setFloatProperty(property, it.animatedValue as Float)
    }
    return animator
}

private fun View.setFloatProperty(property: Property, floatVal: Float) {
    when (property) {
        Property.ELEVATION -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.elevation = floatVal
            }
        }
        Property.ALPHA -> {
            this.alpha = floatVal
        }
        Property.TRANSLATION_Y -> {
            this.translationY = floatVal
        }
        Property.TRANSLATION_X -> {
            this.translationX = floatVal
        }
        Property.SCALE -> {
            this.scaleX = floatVal
            this.scaleY = floatVal
        }
        Property.ROTATION -> {
            this.rotation = floatVal
        }
        Property.SIZEX -> {
            val layoutParams = this.layoutParams
            layoutParams.width = floatVal.toInt()
            this.layoutParams = layoutParams
        }
        Property.SIZEY -> {
            val layoutParams = this.layoutParams
            layoutParams.height = floatVal.toInt()
            this.layoutParams = layoutParams
        }
    }
}