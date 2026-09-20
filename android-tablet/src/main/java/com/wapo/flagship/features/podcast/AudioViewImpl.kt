package com.wapo.flagship.features.podcast

import android.content.Context
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.FusionActivity
import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.views.PodcastSectionView
import com.wapo.flagship.features.grid.model.Audio
import com.wapo.flagship.features.pagebuilder.AudioView
import com.wapo.view.RippleHelper

class AudioViewImpl(
    context: Context,
) : AudioView(context) {
    private val sectionAudioView = PodcastSectionView(context)

    init {
        layoutParams =
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            )
        addView(sectionAudioView)
    }

    override fun setAudio(audio: Audio) {
        if (audio.mediaId != null && audio.streamUrl != null && audio.duration != null) {
            val activity = context.findActivityOfType<FusionActivity>()
            val audioMediaConfig = audio.toAudioMediaConfig(
                audioTracker = AudioTrackerImpl(
                    tabName = activity?.activeTabName,
                    appSection = activity?.getCurrentSectionDisplayName(),
                    duration = audio.duration ?: 0L,
                    feed = audio.tracking?.seriesSlug,
                    avName = audio.tracking?.audioName,
                )
            )
            sectionAudioView.setAudioMediaConfig(audioMediaConfig)
        }
    }

    override fun setRippleEffect() {
        RippleHelper.addRippleEffectToView(sectionAudioView)
    }
}
