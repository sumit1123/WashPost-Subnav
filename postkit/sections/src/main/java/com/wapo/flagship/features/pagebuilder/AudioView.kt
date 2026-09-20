package com.wapo.flagship.features.pagebuilder

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

open class AudioView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int): LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    open fun setAudio(audio: com.wapo.flagship.features.grid.model.Audio) {
    }

    open fun setRippleEffect() {
    }
}