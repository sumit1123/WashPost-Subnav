package com.wapo.flagship.features.posttv.model

import android.os.Parcelable
import androidx.media3.common.C
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Video(
    val id: String,
    val contentUrl: String,
    val startPos: Long,
    val isYouTube: Boolean,
    val isVimeo: Boolean,
    val isLive: Boolean,
    val isLooping: Boolean,
    val autoplay: Boolean,
    val promoIsLooping: Boolean?,
    val promoUrl: String?,
    val duration: Long,
    val shareUrl: String?,
    val headline: String?,
    val pageName: String?,
    val videoName: String?,
    val videoSection: String?,
    val videoSource: String?,
    val videoCategory: String?,
    val contentId: String?,
    val fallbackUrl: String,
    var adTagUrl: String?,
    val shouldPlayAds: Boolean,
    val subtitleUrl: String?,
    val mediaUrl: String?,
    val aspectRatio: Float,
    var playbackPosition: Long,
    val avArcId: String?,
    @IgnoredOnParcel
    val source: @RawValue Any? = null,
    val height: Float = 0.0f,
    val width: Float = 0.0f,
    val altText: String? = null,
    val relatedLink: String? = null,
    val relatedLinkLastModified: String? = null,
    val avPlayerType: String? = null,
    val arcId: String? = null,
    val commentCount: Int? = null
): Parcelable {

    var mDuration: Long = 0
    var adStatus: Int
    var playType: Int = PLAY_TYPE_NORMAL
        private set

    init {
        if (duration < 1) {
            mDuration = C.TIME_UNSET / US_PER_MS
        } else {
            mDuration = duration
        }
        adStatus = AD_NOT_STARTED
    }

    fun shouldPlayAd(): Boolean {
        return shouldPlayAds && adStatus == AD_NOT_STARTED || adStatus == AD_IN_PROGRESS
    }

    class Builder {
        private var id: String? = null
        private var contentUrl: String? = null
        private var startPos: Long = 0
        private var isYouTube = false
        private var isVimeo = false
        private var isLive = false
        private var isLooping = false
        private var autoplay = false
        private var promoIsLooping: Boolean? = null
        private var promoUrl: String? = null
        private var duration: Long = 0
        private var shareUrl: String? = null
        private var headline: String? = null
        private var pageName: String? = null
        private var videoName: String? = null
        private var videoSection: String? = null
        private var videoSource: String? = null
        private var videoCategory: String? = null
        private var contentId: String? = null
        private var fallbackUrl: String? = null
        private var adTagUrl: String? = null
        private var shouldPlayAds = false
        private var subtitleUrl: String? = null
        private var mediaUrl: String? = null
        private var aspectRatio: Float = 0.0f
        private var playbackPosition: Long = -1
        private var avArcId: String? = null
        private var source: Any? = null
        private var playType: Int = PLAY_TYPE_NORMAL
        private var height: Float = 0.0f
        private var width: Float = 0.0f
        private var altText: String? = null
        private var relatedLink: String? = null
        private var avPlayerType: String? = null
        private var relatedLinkLastModified: String? = null
        private var arcId: String? = null
        private var commentCount: Int? = null

        fun setId(id: String?): Builder {
            this.id = id
            return this
        }

        fun setContentUrl(contentUrl: String?): Builder {
            this.contentUrl = contentUrl
            return this
        }

        fun setStartPos(startPos: Long): Builder {
            this.startPos = startPos
            return this
        }

        fun setIsYouTube(isYouTube: Boolean): Builder {
            this.isYouTube = isYouTube
            return this
        }

        fun setIsVimeo(isVimeo: Boolean): Builder {
            this.isVimeo = isVimeo
            return this
        }

        fun setIsLive(isLive: Boolean): Builder {
            this.isLive = isLive
            return this
        }

        fun setIsLooping(isLooping: Boolean): Builder {
            this.isLooping = isLooping
            return this
        }

        fun setAutoplay(autoplay: Boolean): Builder {
            this.autoplay = autoplay
            return this
        }

        fun setPromoIsLooping(promoIsLooping: Boolean?): Builder {
            this.promoIsLooping = promoIsLooping
            return this
        }

        fun setPromoUrl(promoUrl: String?): Builder {
            this.promoUrl = promoUrl
            return this
        }

        fun setDuration(duration: Long): Builder {
            this.duration = duration
            return this
        }

        fun setShareUrl(shareUrl: String?): Builder {
            this.shareUrl = shareUrl
            return this
        }

        fun setHeadline(headline: String?): Builder {
            this.headline = headline
            return this
        }

        fun setPageName(pageName: String?): Builder {
            this.pageName = pageName
            return this
        }

        fun setVideoName(videoName: String?): Builder {
            this.videoName = videoName
            return this
        }

        fun setVideoSection(videoSection: String?): Builder {
            this.videoSection = videoSection
            return this
        }

        fun setVideoSource(videoSource: String?): Builder {
            this.videoSource = videoSource
            return this
        }

        fun setVideoCategory(videoCategory: String?): Builder {
            this.videoCategory = videoCategory
            return this
        }

        fun setContentId(contentId: String?): Builder {
            this.contentId = contentId
            return this
        }

        fun setFallbackUrl(fallbackUrl: String?): Builder {
            this.fallbackUrl = fallbackUrl
            return this
        }

        fun setAdTagUrl(adTagUrl: String?): Builder {
            this.adTagUrl = adTagUrl
            return this
        }

        fun setShouldPlayAds(shouldPlayAds: Boolean): Builder {
            this.shouldPlayAds = shouldPlayAds
            return this
        }

        fun setSubtitleUrl(subtitleUrl: String?): Builder {
            this.subtitleUrl = subtitleUrl
            return this
        }

        fun setMediaUrl(mediaUrl: String?): Builder {
            this.mediaUrl = mediaUrl
            return this
        }

        fun setAspectRatio(aspectRatio: Float): Builder {
            this.aspectRatio = aspectRatio
            return this
        }

        fun setPlaybackPosition(playbackPosition: Long): Builder {
            this.playbackPosition = playbackPosition
            return this
        }

        fun setAvArcId(avArcId: String?): Builder {
            this.avArcId = avArcId
            return this
        }

        fun setSource(source: Any?): Builder {
            this.source = source
            return this
        }

        fun setPlayType(playType: Int): Builder {
            this.playType = playType
            return this
        }

        fun setHeight(height: Float): Builder {
            this.height = height
            return this
        }

        fun setWidth(width: Float): Builder {
            this.width = width
            return this
        }

        fun setAltText(altText: String?): Builder {
            this.altText = altText
            return this
        }

        fun setRelatedLink(relatedLink: String?, relatedLinkLastModified: String?): Builder {
            this.relatedLink = relatedLink
            this.relatedLinkLastModified = relatedLinkLastModified
            return this
        }

        fun setAvPlayerType(avPlayerType: String?): Builder {
            this.avPlayerType = avPlayerType
            return this
        }

        fun setArcId(arcId: String?): Builder {
            this.arcId = arcId
            return this
        }

        fun setCommentCount(commentCount: Int?): Builder {
            this.commentCount = commentCount
            return this
        }

        fun build(): Video {
            return Video(
                id ?: "",
                contentUrl ?: "",
                startPos,
                isYouTube,
                isVimeo,
                isLive,
                isLooping,
                autoplay,
                promoIsLooping,
                promoUrl,
                duration,
                shareUrl,
                headline,
                pageName,
                videoName,
                videoSection,
                videoSource,
                videoCategory,
                contentId,
                fallbackUrl ?: "",
                adTagUrl,
                shouldPlayAds,
                subtitleUrl,
                mediaUrl,
                aspectRatio,
                playbackPosition,
                avArcId,
                source,
                height,
                width,
                altText,
                relatedLink,
                relatedLinkLastModified,
                avPlayerType,
                arcId,
                commentCount
            ).also {
                it.playType = playType
            }
        }
    }

    companion object {
        const val US_PER_MS = 1000
        const val AD_NOT_STARTED = 0
        const val AD_IN_PROGRESS = 1
        const val AD_COMPLETED = 2
        const val PLAY_TYPE_NORMAL = 0
        const val PLAY_TYPE_NORMAL_MUTED = 1
        const val PLAY_TYPE_AUTOPLAY = 2
    }
}