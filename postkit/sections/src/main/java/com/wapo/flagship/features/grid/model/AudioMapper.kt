package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.grid.AudioEntity
import com.wapo.flagship.features.grid.AudioTrackingEntity
import com.wapo.flagship.features.grid.SubscriptionLinksEntity

object AudioMapper {

    fun getAudio(audioEntity: AudioEntity?, pageConfig: PageConfig): Audio? {
        audioEntity ?: return null
        return audioEntity.run {
            Audio(
                mediaId = mediaId,
                streamUrl = streamUrl,
                streamUrlNoAds = streamUrlNoAds,
                duration = duration,
                slug = slug,
                displayDate = displayDate,
                displayLabel = label?.label?.text,
                displayTransparency = label?.labelSecondary?.text,
                titlePrefix = titlePrefix,
                title = title,
                subscriptionLinks = getSubscriptionLinks(subscriptionLinks),
                tracking = getAudioTracking(tracking),
                playerType = PlayerType.PODCAST,
                playerMediaEntity = MediaMapper.getMedia(playerMediaEntity, pageConfig),
                series = series,
                adConfig = AudioAdMapper.getAudioAdConfig(adConfig),
            )
        }
    }

    private fun getSubscriptionLinks(subscriptionLinksEntity: SubscriptionLinksEntity?): SubscriptionLinks? {
        subscriptionLinksEntity ?: return null
        return subscriptionLinksEntity.run {
            SubscriptionLinks(
                alexa = alexa,
                applePodcasts = applePodcasts,
                googlePlay = googlePlay,
                iheartRadio = iheartRadio,
                radioPublic = radioPublic,
                rss = rss,
                spotify = spotify,
                stitcher = stitcher,
                tuneIn = tuneIn,
            )
        }
    }

    private fun getAudioTracking(audioTrackingEntity: AudioTrackingEntity?): AudioTracking? {
        audioTrackingEntity ?: return null
        return audioTrackingEntity.run {
            AudioTracking(
                seriesSlug,
                audioName
            )
        }
    }
}
