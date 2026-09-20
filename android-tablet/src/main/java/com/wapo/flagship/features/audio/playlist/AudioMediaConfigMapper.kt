package com.wapo.flagship.features.audio.playlist

import com.wapo.android.commons.util.toDateLong
import com.wapo.flagship.common.getArticlesAdTargetingValues
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.ArticleAudioAdBreak
import com.wapo.flagship.features.articles2.models.deserialized.ArticleAudioAdBreakType
import com.wapo.flagship.features.articles2.models.deserialized.ArticleAudioAdConfig
import com.wapo.flagship.features.articles2.models.deserialized.Audio as FeedsAudio
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast as FeedsPodcast
import com.wapo.flagship.features.articles2.models.deserialized.podcast.SubscriptionLinks as PodcastSubscriptionLinks
import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.config2.AudioMediaAdBreak
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaSubscriptionLinks
import com.wapo.flagship.features.audio.models.PlaybackVoice
import com.wapo.flagship.features.grid.AudioArticleVoiceType
import com.wapo.flagship.features.grid.VoiceUtils
import com.wapo.flagship.features.grid.model.Audio as FusionAudio
import com.wapo.flagship.features.grid.model.AudioArticle as FusionAudioArticle
import com.wapo.flagship.features.grid.model.SubscriptionLinks as FusionSubscriptionLinks
import com.wapo.flagship.json.TrackingInfo
import java.util.Date

//  region playlist models

fun Playlist.toAudioMediaConfig(): AudioMediaConfig =
    AudioMediaConfig(
        mediaId = mediaId,
        humanAdsUrl = humanAdsUrl,
        humanRawUrl = humanRawUrl,
        manifestUrl = manifestUrl,
        adsUrl = adsUrl,
        rawUrl = rawUrl,
        title = title,
        titlePrefix = titlePrefix,
        titleSeparator = titleSeparator,
        subtitle = subtitle,
        date = date,
        imageUrl = imageUrl,
        imageCaption = imageCaption,
        duration = duration,
        streamUrl = streamUrl,
        streamUrlNoAds = streamUrlNoAds,
        contentUrl = contentUrl,
        sectionName = sectionName,
        caption = caption,
        labelType = labelType,
        primaryLabel = primaryLabel,
        secondaryLabel = secondaryLabel,
        voices = voices?.mapNotNull { it.toPlaybackVoice() }?.toMutableList(),
        arcId = arcId,
        audioTracking = tracker?.toAudioTracking(),
        labelStyle = primaryLabelStyle,
        series = series,
        adConfig = adConfig,
        seriesSlug = seriesSlug,
        podcastSlug = podcastSlug,
        subscriptionLinks = subscriptionLinks,
    )

private fun AudioVoice.toPlaybackVoice(): PlaybackVoice? {
    if (id.isNullOrEmpty() || text.isNullOrEmpty()) return null
    return PlaybackVoice(
        id = id ?: "",
        text = text ?: "",
        rawUrl = rawUrl ?: "",
        adsUrl = adsUrl ?: "",
        duration = duration ?: 0,
    )
}

private fun com.wapo.flagship.features.audio.playlist.AudioTracker.toAudioTracking(): AudioTracker? {
    if (tabName.isNullOrEmpty()) return null
    return AudioTrackerImpl(
        tabName = tabName,
        appSection = appSection,
        trackingInfo = trackingInfo?.toTrackingInfo(),
        speed = speed,
        feed = feed,
        isFlexAudio = isFlexAudio,
        isActionButton = isActionButton,
        isAudioCarousel = isAudioCarousel,
        isActionAudio = isActionAudio,
        isAudioPlaylist = isAudioPlaylist,
        avName = avName,
    )
}

private fun AudioTrackingInfo.toTrackingInfo(): TrackingInfo =
    TrackingInfo().also {
        it.pageName = pageName
        it.pageNumber = pageNumber
        it.channel = channel
        it.contentSubsection = contentSubsection
        it.contentType = contentType
        it.contentAuthor = contentAuthor
        it.searchKeywords = searchKeywords
        it.pageFormat = pageFormat
        it.blogName = blogName
        it.contentSource = contentSource
        it.contentURL = contentURL
        it.interfaceType = interfaceType
        it.contentId = contentId
        it.source = source
        it.primarySection = primarySection
        it.secondarySection = secondarySection
        it.subSection = subSection
        it.arcId = arcId
        it.title = title
        it.authorId = authorId
        it.newsroomDesk = newsroomDesk
        it.newsroomSubdesk = newsroomSubdesk
        it.firstPublishedDate = firstPublished?.let { millis -> Date(millis) }
        it.contentTopics = contentTopics
        it.trackingTags = trackingTags
        it.commercialNode = commercialNode
        it.contentCategory = contentCategory
        it.headline = headline
        it.hierarchy = hierarchy
        it.audioFirstPublishDate = audioFirstPublishDate
    }

//  endregion

//  region jsonApp models

fun FusionAudio.toAudioMediaConfig(
    audioTracker: AudioTracker? = AudioTrackerImpl(
        feed = tracking?.seriesSlug,
        avName = tracking?.audioName,
    ),
    imageUrlFallback: String? = null,
    contentUrl: String? = null,
    adsCustomTargeting: Map<String, List<String>>? = null,
    audioType: String? = null,
): AudioMediaConfig {
    return AudioMediaConfig(
        mediaId = mediaId,
        streamUrl = streamUrl,
        streamUrlNoAds = streamUrlNoAds,
        title = title,
        titlePrefix = titlePrefix,
        primaryLabel = displayLabel,
        secondaryLabel = displayTransparency,
        date = toDateLong(displayDate).takeIf { it > 0L },
        imageUrl = playerMediaEntity?.url ?: coverImage ?: imageUrlFallback,
        imageCaption = playerMediaEntity?.caption,
        contentUrl = contentUrl,
        duration = duration,
        audioTracking = audioTracker,
        adsCustomTargeting = adsCustomTargeting,
        audioType = audioType,
        sectionName = tracking?.seriesSlug,
        series = series ?: seriesName,
        adConfig = adConfig,
        seriesSlug = tracking?.seriesSlug,
        podcastSlug = slug,
        subscriptionLinks = subscriptionLinks?.toAudioMediaSubscriptionLinks(),
    )
}

private fun FusionSubscriptionLinks.toAudioMediaSubscriptionLinks(): AudioMediaSubscriptionLinks {
    return AudioMediaSubscriptionLinks(
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

fun FusionAudioArticle.toAudioMediaConfig(
    audioTracker: AudioTracker? = null,
    adsCustomTargeting: Map<String, List<String>>? = null,
    labelStyle: String? = null,
    audioType: String? = null,
): AudioMediaConfig {
    return AudioMediaConfig(
        humanAdsUrl = humanVoice?.adsUrl,
        humanRawUrl = humanVoice?.rawUrl,
        titlePrefix = titlePrefix,
        titleSeparator = titleSeparator,
        title = title,
        primaryLabel = label?.text,
        secondaryLabel = label?.secondaryText,
        date = toDateLong(displayDate),
        imageUrl = playerMedia?.url,
        imageCaption = playerMedia?.caption,
        contentUrl = contentUrl?.let { getUrlWithoutParameters(it) },
        sectionName = tracking?.section,
        caption = caption,
        duration = VoiceUtils.getPreferredVoiceDuration(this),
        voices = voices
                ?.mapNotNull {
                    if (it.voiceId != null && it.rawUrl != null && it.label != null) {
                        PlaybackVoice(it.voiceId!!, it.label!!, it.rawUrl!!, it.adsUrl, it.duration)
                    } else {
                        null
                    }
                }?.toMutableList(),
        arcId = tracking?.arcId,
        audioTracking = audioTracker,
        adsCustomTargeting = adsCustomTargeting,
        labelStyle = labelStyle,
        audioType = audioType,
        adConfig = adConfig,
    )
}

// endregion

//  region feeds models

fun FeedsAudio.toAudioMediaConfig(
    playerType: PlayerType? = null,
    article: Article2? = null,
    audioTracker: AudioTracker? = null,
    children: List<AudioMediaConfig>? = null,
    transitions: List<AudioMediaConfig>? = null,
): AudioMediaConfig =
    AudioMediaConfig(
        playerType = playerType,
        mediaId = mediaId,
        manifestUrl = manifestUrl,
        humanAdsUrl = if (subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) adsUrl else null,
        humanRawUrl = if (subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) rawUrl else null,
        adsUrl = if (subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) adsUrl else null,
        rawUrl = if (subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) rawUrl else null,
        titlePrefix = title?.prefix,
        titleSeparator = title?.separator,
        title = title?.content ?: article?.title,
        primaryLabel = label?.displayLabel,
        secondaryLabel = label?.displayTransparency,
        date = (
                article?.items?.firstOrNull { item ->
                    item is com.wapo.flagship.features.articles2.models.deserialized.Date
                } as? com.wapo.flagship.features.articles2.models.deserialized.Date
                )?.content,
        imageUrl = article?.audio?.image?.imageURL,
        imageCaption = article?.audio?.image?.fullCaption,
        contentUrl = article?.contenturl?.let { getUrlWithoutParameters(it) },
        sectionName = article?.section,
        caption = article?.audio?.caption,
        voices = null,
        arcId = article?.arcId,
        audioTracking = audioTracker,
        adsCustomTargeting = getArticlesAdTargetingValues(article, null, null, article?.contenturl),
        duration = duration,
        labelStyle = label?.style,
        url = url,
        children = children,
        transitions = transitions,
        type = type,
        series = series,
        adConfig = adConfig?.toAudioAdConfig()
            ?: (if (this.type == article?.audio?.type) article?.audio?.adConfig?.toAudioAdConfig() else null),
    )

fun ArticleAudioAdConfig.toAudioAdConfig(): AudioMediaAdConfig {
    return AudioMediaAdConfig(
        adSetUrl = adSetUrl.orEmpty(),
        adBreaks = adBreaks?.mapNotNull { it.toAudioAdBreak() },
        primarySectionId = primarySectionId,
    )
}

private fun ArticleAudioAdBreak.toAudioAdBreak(): AudioMediaAdBreak? {
    return when (type) {
        ArticleAudioAdBreakType.PREROLL -> AudioMediaAdBreak.Preroll(
            maxAds = maxAds ?: 1
        )

        ArticleAudioAdBreakType.MIDROLL -> AudioMediaAdBreak.Midroll(
            maxAds = maxAds ?: 2,
            timeMs = time?.times(1000)?.toLong() ?: return null,
        )

        ArticleAudioAdBreakType.POSTROLL -> AudioMediaAdBreak.Postroll(
            maxAds = maxAds ?: 2
        )

        else -> null
    }
}

fun FeedsPodcast.toAudioMediaConfig(): AudioMediaConfig {
    return AudioMediaConfig(
        mediaId = mediaId,
        streamUrl = completeUrl,
        streamUrlNoAds = podtracUrl,
        title = episodeName,
        primaryLabel = seriesName,
        imageUrl = seriesImageUrl,
        duration = duration,
        series = seriesName,
        seriesSlug = seriesSlug,
        podcastSlug = episodeSlug,
        subscriptionLinks = subscriptionLinks?.toAudioMediaSubscriptionLinks(),
        adConfig = adConfig?.toAudioAdConfig(),
    )
}

private fun PodcastSubscriptionLinks.toAudioMediaSubscriptionLinks(): AudioMediaSubscriptionLinks {
    return AudioMediaSubscriptionLinks(
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

//  endregion
