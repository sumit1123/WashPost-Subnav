package com.washingtonpost.foryou.utils

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.features.posttv.model.Video
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.data.VideoStream
import com.washingtonpost.foryou.data.getVideoShareUrl

class ForYouVideoUtil {

    companion object {

        private fun getStreamUrl(streams: List<VideoStream?>?, mobileMaxBitRate: Int, tabletMaxBitRate: Int): String? {
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
                    preferredMap[preferredStream]?.addAll(
                        nonNullStreams.filter {
                            it.url?.endsWith(preferredStream) == true && it.bitRate?.let { bitRate -> bitRate <= maxBitRate } ?: false
                        }.sortedByDescending {
                            it.bitRate ?: it.width ?: 0
                        }.toList()
                    )
                }
                val preferredStreamFormat =
                    preferredMap.keys.firstOrNull { preferredMap[it]?.isNotEmpty() ?: false }
                return preferredMap[preferredStreamFormat]?.firstOrNull()?.url ?: nonNullStreams.firstOrNull()?.url
            }
            return null
        }

        fun getVideo(recommendationsItem: RecommendationsItem, pageConfig: PageConfig): Video? {
            val video = try {
                val item = recommendationsItem.video
                val streamUrl = getStreamUrl(item?.streams, pageConfig.videoMaxBitRateMobile, pageConfig.videoMaxBitRateTablet)
                val mediaUrl = item?.promoImage?.url
                val relatedContent = item?.relatedContent?.firstOrNull()?.link
                if (streamUrl != null) {
                    Video.Builder()
                        .setId(streamUrl)
                        .setShareUrl(recommendationsItem.getVideoShareUrl())
                        .setHeadline(recommendationsItem.headline ?: recommendationsItem.headlines?.basic ?: item?.altText ?: "")
                        .setPlayType(Video.PLAY_TYPE_AUTOPLAY)
                        .setPageName("front - for-you")
                        .setContentId(item?.contentId)
                        .setVideoName(item?.tracking?.avName)
                        .setVideoSection(item?.tracking?.videoSection)
                        .setAvArcId(item?.contentId)
                        .setVideoSource(item?.tracking?.videoSource)
                        .setMediaUrl(mediaUrl)
                        .setAspectRatio(item?.aspectRatio ?: 1f)
                        .setDuration(item?.duration?.toLong() ?: 0L)
                        .setHeight(item?.streams?.get(0)?.height?.toFloat() ?: 1f)
                        .setWidth(item?.streams?.get(0)?.width?.toFloat() ?: 1f)
                        .setVideoCategory(recommendationsItem.label?.basic?.text ?: item?.tracking?.videoSection)
                        .setAltText(item?.altText)
                        .setRelatedLink(relatedContent?.url, relatedContent?.lastModified)
                        .setAutoplay(true)
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
            return video
        }
    }
}
