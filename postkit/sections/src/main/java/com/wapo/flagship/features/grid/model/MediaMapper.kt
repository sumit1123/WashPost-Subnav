package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.*

object MediaMapper {

    fun getMedia(mediaEntity: MediaEntity?, pageConfig: PageConfig) : Media? {
        mediaEntity ?: return null

        val media = mediaEntity.run {
            Media(promoImageURL, getMediaType(mediaType), width, height, aspectRatio, null,
                null, getOverlay(overlay, pageConfig), caption, url, getVideo(video, pageConfig), getLiveImage(liveImage),
                HomepageStoryMapper.getLink(link), getDynamicReplacement(dynamicReplacement), altText, getBleed(bleed), makeItRound
            )
        }

        media.liveImage?.let {
            //set media aspect ratio with very first live image for easy access to measure live image
            media.aspectRatio = it.tabs.first().images.first().aspectRatio
        }

        return media
    }

    fun getSlideShow(slideshowEntity: SlideShowEntity?, pageConfig: PageConfig): SlideShow?{
        slideshowEntity ?: return null
        val slideShow = slideshowEntity.run {
            SlideShow(slideshowEntity.scalingStrategy, getSlideShowImages(slideshowEntity), HomepageStoryMapper.getLink(link), aspectRatio, getOverlay(slideshowEntity.overlay, pageConfig), getBleed(slideshowEntity.bleed))
        }
        return slideShow
    }

    private fun getSlideShowImages(slideShow: SlideShowEntity): List<SlideShowImagesModel>? {
        slideShow.images ?: return null
        return slideShow.images.map {
            SlideShowImagesModel(it?.url, it?.caption)
        }
    }

    private fun getMediaType(mediaType: MediaTypeEntity?): MediaType {
        mediaType ?: return MediaType.FEATURED

        return when (mediaType) {
            MediaTypeEntity.FEATURED -> MediaType.FEATURED
            MediaTypeEntity.POST_TV -> MediaType.POST_TV
            MediaTypeEntity.YOUTUBE -> MediaType.YOUTUBE
            MediaTypeEntity.LIVE_IMAGE -> MediaType.LIVE_IMAGE
        }
    }

    private fun getArtPosition(artPosition: ArtPositionEntity?) : ArtPosition {

        artPosition ?: return ArtPosition.HIGH

        return when(artPosition) {

            ArtPositionEntity.BELOW_HEADLINE -> ArtPosition.BELOW_HEADLINE
            ArtPositionEntity.HIGH -> ArtPosition.HIGH
            ArtPositionEntity.LEFT -> ArtPosition.LEFT
            ArtPositionEntity.RIGHT -> ArtPosition.RIGHT
            ArtPositionEntity.LEFT_OF_BLURB -> ArtPosition.LEFT_OF_BLURB
            ArtPositionEntity.RIGHT_OF_BLURB -> ArtPosition.RIGHT_OF_BLURB
            ArtPositionEntity.LEFT_OF_HEADLINE -> ArtPosition.LEFT_OF_HEADLINE
            ArtPositionEntity.RIGHT_OF_HEADLINE -> ArtPosition.RIGHT_OF_HEADLINE
        }
    }

    fun getArtWidth(artWidth: ArtWidthEntity?) : ArtWidth {

        artWidth ?: return ArtWidth.MEDIUM

        return when(artWidth) {
            ArtWidthEntity.MINI -> ArtWidth.MINI
            ArtWidthEntity.SMALL -> ArtWidth.SMALL
            ArtWidthEntity.MEDIUM -> ArtWidth.MEDIUM
            ArtWidthEntity.LARGE -> ArtWidth.LARGE
            ArtWidthEntity.XLARGE -> ArtWidth.XLARGE
            ArtWidthEntity.XXLARGE -> ArtWidth.XXLARGE
            ArtWidthEntity.XSMALL -> ArtWidth.XSMALL
            ArtWidthEntity.TINY -> ArtWidth.TINY
            ArtWidthEntity.FULL_WIDTH -> ArtWidth.FULL_WIDTH
        }
    }

    private fun getOverlay(overlay: OverlayEntity?, pageConfig: PageConfig) : Overlay? {

        overlay ?: return null

        val prefixIcon = getArtOverlayIcon(overlay.prefixIcon)
        val suffixIcon = getArtOverlayIcon(overlay.suffixIcon)
        val style = getOverlayStyle(overlay.style)
        val secondaryStyle = getOverlayStyle(overlay.secondaryStyle)
        return Overlay(overlay.text, style, suffixIcon, prefixIcon, getMedia(overlay.prefixMedia, pageConfig), overlay.secondaryText, secondaryStyle)
    }

    private fun getOverlayStyle(overlayStyle: OverlayStyleEntity?): OverlayStyle? {

        overlayStyle ?: return null

        return when(overlayStyle) {
            OverlayStyleEntity.DEFAULT -> OverlayStyle.DEFAULT
            OverlayStyleEntity.LIVE -> OverlayStyle.LIVE
            OverlayStyleEntity.SECONDARY -> OverlayStyle.SECONDARY
            OverlayStyleEntity.COMPACT -> OverlayStyle.COMPACT
        }
    }

    private fun getArtOverlayIcon(artOverlayIcon: ArtOverlayIconEntity?): ArtOverlayIcon? {

        artOverlayIcon ?: return null

        return when(artOverlayIcon) {
            ArtOverlayIconEntity.ARROW -> ArtOverlayIcon.ARROW
            ArtOverlayIconEntity.CAMERA -> ArtOverlayIcon.CAMERA
            ArtOverlayIconEntity.PLAY -> ArtOverlayIcon.PLAY
            ArtOverlayIconEntity.WAVEFORM -> ArtOverlayIcon.WAVEFORM
        }
    }

    private fun getVideo(video: VideoEntity?, pageConfig: PageConfig) : Video? {

        video ?: return null

        return Video(
            youtubeId = video.youTubeId,
            pagebuilderStreamUrl = null,
            streams = getStreams(video.streams),
            isLive = video.isLive,
            isLooping = video.isLooping,
            autoplay = video.autoplay,
            promo = getPromo(video.promo),
            omniture = getVideoTracking(video.omniture),
            relatedContent = getRelatedContent(video.relatedContent),
            adConfig = getAdConfig(video.adConfig),
            duration = video.duration,
            mobileMaxBitRate = pageConfig.videoMaxBitRateMobile,
            tabletMaxBitRate = pageConfig.videoMaxBitRateTablet
        )
    }

    private fun getPromo(promoEntity: PromoEntity?) : Promo? {
        promoEntity ?: return null

        return Promo(promoEntity.isLooping, promoEntity.url)
    }

    private fun getVideoTracking(videoTracking: VideoTrackingEntity?) : VideoTracking? {

        videoTracking ?: return null

        return VideoTracking(videoTracking.pageName, videoTracking.videoSource, videoTracking.videoSection, videoTracking.videoName, videoTracking.videoCategory, videoTracking.contentId)
    }

    private fun getRelatedContent(videoRelatedContent: List<RelatedContent>?) : VideoRelatedContent? {

        return videoRelatedContent?.let {
            VideoRelatedContent(link = getRelatedContentLink(videoRelatedContent.first().link))
        }
    }

    private fun getRelatedContentLink(relatedContentLink: RelatedContentLink?) : VideoRelatedContentLink? {

        return relatedContentLink?.let {
            VideoRelatedContentLink(
                url = relatedContentLink.url,
                lastModified = relatedContentLink.lastModified
            )
        }
    }

    private fun getAdConfig(adConfig: AdConfigEntity?) : AdConfig? {

        adConfig ?: return null

        return AdConfig(adConfig.commercialAdNode, adConfig.playVideoAds, adConfig.forceAd, adConfig.allowPrerollOnDomain, adConfig.enableAutoPreview, adConfig.playAds, adConfig.autoPlayPreroll, adConfig.enableAdInsertion, adConfig.videoAdZone, adConfig.adSetUrl, adConfig.primarySectionId)
    }

    private fun getStreams(streamsEntity: List<StreamEntity>?) : List<VideoStream>? {

        streamsEntity ?: return null

        return streamsEntity.map {
            VideoStream(it.bitRate, it.fileSize, it.height, it.width, it.provider, it.streamType, it.url)
        }
    }

    private fun getLiveImage(liveImageEntity: LiveImageEntity?) : LiveImage? {
        liveImageEntity ?: return null

        val tabs = liveImageEntity.tabs.map { liveImageTabEntity ->
            LiveImageTab(liveImageTabEntity.images.map {
                LiveImageInfo(it.url, it.darkModeUrl, it.aspectRatio, it.alternateText)
            }.toMutableList(), liveImageTabEntity.text)
        }.toMutableList()


        val liveImageType = when(liveImageEntity.type) {
                    LiveImageTypeEntity.CAROUSEL -> LiveImageType.CAROUSEL
                    LiveImageTypeEntity.TAB -> LiveImageType.TAB
                    LiveImageTypeEntity.SEGMENT -> LiveImageType.SEGMENT
                    else -> LiveImageType.TAB
        }

        return LiveImage(tabs, liveImageType, liveImageEntity.cta)
    }

    private fun getDynamicReplacement(subItemTypeEntity: SubItemTypeEntity?) : SubItemType? {
        subItemTypeEntity ?: return null

        return HomepageStoryMapper.getSubItemType(subItemTypeEntity)
    }

    private fun getBleed(bleedEntity: BleedEntity?): Bleed {
        return when (bleedEntity) {
            BleedEntity.FULL -> Bleed.FULL
            BleedEntity.CONTAINER -> Bleed.CONTAINER
            else -> Bleed.NONE
        }
    }

    enum class ScalingType{
        FIT,
        CENTER
    }
}