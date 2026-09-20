package com.wapo.flagship.features.audio.playlist

import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.models.PlaybackVoice
import com.wapo.flagship.json.TrackingInfo

fun AudioMediaConfig.toPlaylistAudio(): Playlist? {
    contentUrl ?: mediaId ?: return null
    return Playlist(
        playerType = getPlayerType().name,
        id = contentUrl ?: mediaId!!,
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
        voices = voices?.map { it.toPlaylistAudioVoice() },
        arcId = arcId,
        tracker = audioTracking?.toPlaylistAudioTracker(),
        primaryLabelStyle = labelStyle,
        series = series,
        adConfig = adConfig,
        seriesSlug = seriesSlug,
        podcastSlug = podcastSlug,
        subscriptionLinks = subscriptionLinks,
    )
}

fun PlaybackVoice.toPlaylistAudioVoice(): AudioVoice =
    AudioVoice(
        id = id,
        text = text,
        rawUrl = rawUrl,
        adsUrl = adsUrl,
        duration = duration,
    )

// TODO: No need to store all members and also isXXXXX members won't be needed once config is added to the playlist.
// TODO: Cleanup/Add members to [com.wapo.flagship.features.audio.playlist.AudioTracker] based on what we need while handling analytics.
fun AudioTracker.toPlaylistAudioTracker(): com.wapo.flagship.features.audio.playlist.AudioTracker? =
    if (this is AudioTrackerImpl) {
        AudioTracker(
            tabName = tabName,
            appSection = appSection,
            trackingInfo = trackingInfo?.toPlaylistAudioTrackingInfo(),
            speed = speed,
            feed = feed,
            isFlexAudio = isFlexAudio,
            isActionButton = isActionButton,
            isAudioCarousel = isAudioCarousel,
            isActionAudio = isActionAudio,
            isAudioPlaylist = isAudioPlaylist,
            avName = avName,
        )
    } else {
        null
    }

fun TrackingInfo.toPlaylistAudioTrackingInfo(isAutoOpen: Boolean = false, autoPageName: String? = null): AudioTrackingInfo =
    AudioTrackingInfo(
        pageName = if (isAutoOpen) {
            autoPageName ?: pageName
        } else {
            pageName
        },
        pageNumber = pageNumber,
        channel = channel,
        contentSubsection = contentSubsection,
        contentType = contentType,
        contentAuthor = contentAuthor,
        searchKeywords = searchKeywords,
        pageFormat = pageFormat,
        blogName = blogName,
        contentSource = contentSource,
        contentURL = contentURL,
        interfaceType = interfaceType,
        contentId = contentId,
        source = source,
        primarySection = primarySection,
        secondarySection = secondarySection,
        subSection = subSection,
        arcId = arcId,
        title = title,
        authorId = authorId,
        newsroomDesk = newsroomDesk,
        newsroomSubdesk = newsroomSubdesk,
        firstPublished = firstPublishedDate?.time,
        contentTopics = contentTopics,
        trackingTags = trackingTags,
        commercialNode = commercialNode,
        contentCategory = contentCategory,
        headline = headline,
        hierarchy = hierarchy,
        audioFirstPublishDate = audioFirstPublishDate,
    )
