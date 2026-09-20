package com.wapo.view

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView

class ShineFrameLayout : FrameLayout {

    private var asyncShineView: ImageView? = null
    private var asyncShineLoadingAnimation: Animation? = null
    private var shineHeight: Float? = null
    private var nightMode: Boolean = false

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ShineFrameLayout, 0, 0).apply {
            try {
                shineHeight = getDimension(R.styleable.ShineFrameLayout_shineHeight, -1f)
            } finally {
                recycle()
            }
        }
    }

    fun startShineAnimation() {
        stopShineAnimation()
        if (asyncShineView == null) {
            asyncShineView = ImageView(context).apply {
                setImageResource(R.drawable.async_shine)
                alpha = if (nightMode) SHINE_DARK_OPACITY else SHINE_LIGHT_OPACITY
                scaleType = ImageView.ScaleType.CENTER
                if (asyncShineLoadingAnimation == null) {
                    asyncShineLoadingAnimation = AnimationUtils.loadAnimation(context, R.anim.horizontal_anim)
                }
                this@ShineFrameLayout.addView(this)
                shineHeight?.let {
                    if (it != -1f) {
                        layoutParams.height = it.toInt()
                    }
                }
                startAnimation(asyncShineLoadingAnimation)
            }
        }
    }

    fun stopShineAnimation() {
        asyncShineView?.apply {
            clearAnimation()
            this@ShineFrameLayout.removeView(this)
        }
        asyncShineLoadingAnimation = null
        asyncShineView = null
    }

    fun setNightMode(nightMode: Boolean) {
        this.nightMode = nightMode
    }

    companion object {
        private const val SHINE_DARK_OPACITY = 0.2f
        private const val SHINE_LIGHT_OPACITY = 1f
    }
}