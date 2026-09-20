package com.wapo.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView
import java.io.InputStream

class CircleImageView : NetworkAnimatedImageView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, src: InputStream?) : super(context, src)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun setSource(source: Any?) {
        var result: Any? = source
        if (source is Bitmap) {
            result = RoundedBitmapDrawableFactory.create(context.resources, source).apply {
                isCircular = true
                setAntiAlias(true)
                background = ContextCompat.getDrawable(context, R.drawable.image_bg_circle)
            }
        }
        super.setSource(result)
    }
}