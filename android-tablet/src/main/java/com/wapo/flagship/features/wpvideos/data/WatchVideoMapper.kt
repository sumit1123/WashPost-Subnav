package com.wapo.flagship.features.wpvideos.data

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.util.tracking.Measurement
import kotlin.collections.LinkedHashMap

object WatchVideoMapper {
    fun getWpVideoItemList(
        items: List<WpVideosItem?>?,
        shouldSuppressAds: Boolean = false,
        mobileMaxBitRate: Int,
        tabletMaxBitRate: Int
    ): List<Video> {
        if (items.isNullOrEmpty()) return emptyList()

        return items.mapNotNull { item ->
            try {
                val streamUrl = getStreamUrl(item?.streams, mobileMaxBitRate, tabletMaxBitRate)
                val mediaUrl = item?.promoImage?.url
                val relatedLink = item?.relatedContent?.first()?.link
                val adTagUrl = if (shouldSuppressAds || item?.adConfig?.playVideoAds == false) null else item?.adConfig?.adSetUrl
                if (streamUrl != null) {
                    Video.Builder()
                        .setId(streamUrl)
                        .setShareUrl(item?.canonicalUrl)
                        .setPlayType(Video.PLAY_TYPE_AUTOPLAY)
                        .setPageName(Measurement.PAGE_WATCH_VIDEO)
                        .setContentId(item?.contentId)
                        .setDuration(item?.duration?.toLong() ?: 0L)
                        .setAdTagUrl(adTagUrl)
                        .setVideoName(item?.tracking?.avName)
                        .setVideoSection(item?.tracking?.videoSection)
                        .setAvArcId(item?.contentId)
                        .setVideoSource(item?.tracking?.videoSource)
                        .setMediaUrl(mediaUrl)
                        .setRelatedLink(relatedLink?.url, relatedLink?.lastModified)
                        .setAvPlayerType(null)
                        .setCommentCount(item?.comments?.count)
                        .setHeadline(item?.tracking?.pageTitle ?: item?.tracking?.avName ?: "")
                        .build()
                } else {
                    null
                }
            } catch (e: Exception) {
                val eventLogBuilder =
                    EventLog
                        .Builder()
                        .setMessage("WatchVideoMapper FAILURE" + e.message)
                        .setModule(LogModules.WATCH_VIDEO)
                RemoteLog.e(AppContextUtils.appContext, eventLogBuilder.build())
                null
            }
        }
    }

    /**
     * Selects the most suitable video stream URL from a list of available streams.
     * - Applies different max bitrate limits for mobile (300 kbps) and tablet (600 kbps).
     * - Prioritizes stream formats in the following order: "_master.m3u8", "_mobile.m3u8", ".m3u8", ".mp4", ".webm".
     * - Returns the first available stream from the highest-priority format group.
     * - Falls back to the first stream in the list if no preferred matches are found.
     */

    private fun getStreamUrl(
        streams: List<VideoStream?>?,
        mobileMaxBitRate: Int,
        tabletMaxBitRate: Int
    ): String? {
        streams ?: return null
        val nonNullStreams = streams.filterNotNull()
        val preferredStreams = arrayOf("_master.m3u8", "_mobile.m3u8", ".m3u8", ".mp4", ".webm")
        val maxBitRate = if (AppContextUtils.isTablet()) tabletMaxBitRate else mobileMaxBitRate
        if (nonNullStreams.isNotEmpty()) {
            val preferredMap: LinkedHashMap<String, MutableList<VideoStream>> = LinkedHashMap()
            for (preferredStream in preferredStreams) {
                if (preferredMap[preferredStream] == null) {
                    preferredMap[preferredStream] = mutableListOf()
                }
                //group urls by preferred streams && reject streams that exceed max bit rate && sort by max bit rate or greatest width
                preferredMap[preferredStream]!!.addAll(
                    nonNullStreams.filter {
                        it.url?.endsWith(preferredStream) == true && it.bitRate?.let { bitRate -> bitRate <= maxBitRate } ?: false
                    }.sortedByDescending {
                        it.bitRate ?: it.width ?: 0
                    }.toList()
                )
                Logger.d("WpVideoMapper", "$preferredStream: ${preferredMap[preferredStream]}")
            }
            val preferredStreamFormat =
                preferredMap.keys.firstOrNull { preferredMap[it]?.isNotEmpty() ?: false }
            Logger.d(
                "WpVideoMapper",
                "preferred url: ${preferredMap[preferredStreamFormat]?.firstOrNull()?.url}"
            )
            return preferredMap[preferredStreamFormat]?.firstOrNull()?.url ?: nonNullStreams.firstOrNull()?.url
        }
        return null
    }
}