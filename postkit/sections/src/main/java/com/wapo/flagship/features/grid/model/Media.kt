package com.wapo.flagship.features.grid.model

import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.grid.ScalingStrategyType
import kotlin.collections.LinkedHashMap

data class Media(
    val promoImageURL: String?,
    var mediaType: MediaType?,
    val width: Int,
    val height: Int,
    var aspectRatio: Float = 1.5f,
    val artPosition: ArtPosition?,
    var artWidth: ArtWidth? = ArtWidth.SMALL,
    val overlay: Overlay?,
    val caption: String?,
    val url: String?,
    val video: Video?,
    val liveImage: LiveImage?,
    val link: Link?,
    var dynamicReplacement: SubItemType?,
    val altText: String?,
    val bleed: Bleed,
    val makeItRound: Boolean?
) {
    fun getVideoId(): String? {
        val isYouTube = mediaType == MediaType.YOUTUBE
        return if (isYouTube) video?.youtubeId else video?.getStreamUrl()
    }
}

data class SlideShow(
    var scalingStrategy: ScalingStrategyType?,
    var images: List<SlideShowImagesModel?>?,
    val link: Link?,
    val aspectRatio: Float? = 1.5F,
    val overlay: Overlay?,
    val bleed: Bleed
)

class SlideShowImagesModel(
    val url: String?,
    val caption: String?
)

class Video(val youtubeId: String?,
            @Deprecated("old pagebuilder")
            val pagebuilderStreamUrl: String?,
            val streams: List<VideoStream>?,
            val isLive: Boolean,
            val isLooping: Boolean,
            val autoplay: Boolean,
            val promo: Promo?,
            val omniture: VideoTracking?,
            val relatedContent: VideoRelatedContent?,
            val adConfig: AdConfig?,
            val duration: Long? = -1,
            val mobileMaxBitRate: Int,
            val tabletMaxBitRate: Int
) {

    private var streamUrl: String? = null

    fun getStreamUrl(): String? {
        streams ?: return null

        streamUrl?.let { return it }

        //TODO move this to config
        val preferredStreams = arrayOf("_master.m3u8", "_mobile.m3u8", ".m3u8", ".mp4", ".webm")
        val maxBitRate = if (AppContextUtils.isTablet()) tabletMaxBitRate else mobileMaxBitRate
        if (isLive) {
            return streams[0].url
        } else {
            val preferredMap: LinkedHashMap<String, MutableList<VideoStream>> = LinkedHashMap()
            for (preferredStream in preferredStreams) {
                if (preferredMap[preferredStream] == null) {
                    preferredMap[preferredStream] = mutableListOf()
                }

                //group urls by preferred streams && reject streams that exceed max bit rate && sort by max bit rate or greatest width
                preferredMap[preferredStream]!!.addAll(
                        streams.filter {
                            it.url.endsWith(preferredStream) && it.bitRate?.let { bitRate -> bitRate <= maxBitRate } ?: false
                        }.sortedByDescending {
                            it.bitRate ?: it.width!!
                        }.toList())

                Logger.d("GridAdapter", "$preferredStream: ${preferredMap[preferredStream]}")
            }

            val preferredStreamFormat = preferredMap.keys.firstOrNull { preferredMap[it]?.isNotEmpty() ?: false }
            Logger.d("GridAdapter", "preferred url: ${preferredMap[preferredStreamFormat]?.firstOrNull()?.url}")

            streamUrl = preferredMap[preferredStreamFormat]?.firstOrNull()?.url ?: streams[0].url //adding a safeguard for bad json
            return streamUrl
        }
    }

    fun shouldPlayAds(): Boolean {
        val adConfig = adConfig ?: return false
        return adConfig.playVideoAds
    }
}

data class Promo(
    val isLooping: Boolean,
    val url: String?
)

data class VideoTracking(
        val pageName: String?,
        val videoSource: String?,
        val videoSection: String?,
        val videoName: String?,
        val videoCategory: String?,
        val contentId: String?
)

data class VideoRelatedContent(
    val link: VideoRelatedContentLink? = null,
)

data class VideoRelatedContentLink(
    val url: String? = null,
    val lastModified: String? = null,
)

data class VideoStream(
        val bitRate: Int?,
        val fileSize: Int?,
        val height: Int?,
        val width: Int?,
        val provider: String?,
        val stream_type: String?,
        val url: String
)

data class AdConfig(
    val commercialAdNode: String?,
    val playVideoAds: Boolean = false,
    val forceAd: Boolean = false,
    val allowPrerollOnDomain: Boolean = false,
    val enableAutoPreview: Boolean = false,
    val playAds: Boolean = false,
    val autoPlayPreroll: Boolean = false,
    val enableAdInsertion: Boolean = false,
    val videoAdZone: String?,
    val adSetUrl: String?,
    val primarySectionId: String?
)

data class AdSetUrls(
        var apps: String? = null
)

data class AdSetConfig(
        val adSetZone: String?,
        val adSetUrls: AdSetUrls?
)

enum class MediaType {
    POST_TV,
    YOUTUBE,
    FEATURED,
    LIVE_IMAGE
}

enum class ArtPosition {
    HIGH,
    LOW,
    LEFT,
    RIGHT,
    LEFT_OF_BLURB,
    LEFT_OF_HEADLINE,
    RIGHT_OF_HEADLINE,
    RIGHT_OF_BLURB,
    BELOW_HEADLINE
}

enum class ArtWidth(@Deprecated("Old Pagebuilder value") val imageFraction: Float) {
    MINI(0.07f),
    TINY(0.12f),
    XXSMALL(0.17f),
    XSMALL(0.25f),
    SMALL(0.33f),
    MEDIUM(0.41f),
    LARGE(0.49f),
    XLARGE(0.57f),
    XXLARGE(0.65f),
    FULL_WIDTH(0.0F) //ignore value
}

data class Overlay(
        val text: String?,
        val style: OverlayStyle?,
        val suffixIcon: ArtOverlayIcon?,
        val prefixIcon: ArtOverlayIcon?,
        val prefixImage: Media?,
        val secondaryText: String?,
        val secondaryStyle: OverlayStyle?
)

enum class OverlayStyle {
    DEFAULT,
    SECONDARY,
    LIVE,
    COMPACT
}

enum class ArtOverlayIcon {
    ARROW,
    CAMERA,
    PLAY,
    MOBILE,
    WAVEFORM
}

enum class Bleed {
    FULL,
    CONTAINER,
    NONE
}

enum class BleedItemType {
    MEDIA,
    SLIDESHOW,
    OLYMPICS,
    NONE
}