package com.wapo.flagship.features.pagebuilder

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.grid.model.Audio
import com.wapo.flagship.features.grid.model.AudioArticle

open class InlineAudioView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int): LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    open fun setAudio(podcast: Audio?, audioArticle: AudioArticle?, appSection: String?) {
    }

    open fun setRippleEffect() {
    }
}