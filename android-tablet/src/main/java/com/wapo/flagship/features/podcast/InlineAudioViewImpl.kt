package com.wapo.flagship.features.podcast

import android.content.Context
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.model.Audio
import com.wapo.flagship.features.grid.model.AudioArticle
import com.wapo.flagship.features.pagebuilder.InlineAudioView
import com.wapo.view.RippleHelper
import com.washingtonpost.android.R

class InlineAudioViewImpl(
    context: Context,
) : InlineAudioView(context) {
    private val sectionAudioView = InlineAudioSectionView(context)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(sectionAudioView)
    }

    override fun setAudio(
        podcast: Audio?,
        audioArticle: AudioArticle?,
        appSection: String?,
    ) {
        var audioMediaConfig: AudioMediaConfig? = null
        var listenText: String? = null
        if (podcast?.mediaId != null) {
            audioMediaConfig = podcast.toAudioMediaConfig(
                AudioTrackerImpl(
                    isFlexAudio = true,
                    appSection = appSection,
                    duration = podcast.duration ?: 0L,
                    feed = podcast.tracking?.seriesSlug,
                    avName = podcast.tracking?.audioName,
                ),
            )
            listenText = podcast?.inlinePlayer?.listen ?: context.resources.getString(
                R.string.listen_to_podcast,
            )
        } else if (audioArticle != null) {
            audioMediaConfig =
                context.findActivityOfType<GridActivity>()?.getGridEnvironment()?.generateAudioMediaConfig(
                    audioArticle,
                    isFlexFeature = true,
                    isActionButton = false,
                )
            listenText = audioArticle.inlinePlayer?.listen
        }
        audioMediaConfig?.let {
            sectionAudioView.init(
                it,
                listenText ?: context.resources.getString(R.string.listen_to_article),
            )
        }
    }

    override fun setRippleEffect() {
        RippleHelper.addRippleEffectToView(sectionAudioView)
    }
}
