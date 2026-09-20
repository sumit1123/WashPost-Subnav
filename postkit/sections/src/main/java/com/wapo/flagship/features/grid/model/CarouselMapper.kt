package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.grid.CarouselBaseItemEntity
import com.wapo.flagship.features.grid.CarouselItem
import com.wapo.flagship.features.grid.CarouselItemEntity
import com.wapo.flagship.features.grid.CompoundLabelTypeEntity
import com.wapo.flagship.features.grid.ItemType
import com.wapo.flagship.features.grid.LabelStyleEntity
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.grid.model.PageModelMapper.getLayoutAttributes
import com.wapo.flagship.features.grid.views.carousel.CarouselAudioHolder.Companion.PERSONALIZED_PODCAST_PLACEHOLDER
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.wapo.view.habittiles.ArticleSource
import com.washingtonpost.android.recirculation.carousel.models.StyleEntity
import com.wapo.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CarouselMapper {

    fun getCarouselItem(carouselItemEntity: CarouselItemEntity, pageConfig: PageConfig): Carousel {
        return Carousel(getBrights(carouselItemEntity.items, pageConfig)).apply {
            layoutAttributes = PageModelMapper.getLayoutAttributes(carouselItemEntity.layoutAttributes)
            //full bleed is default for the sake of animation/padding
            forceFullBleed = true
        }
    }

    fun getCarouselBaseItem(carouselBaseItemEntity: CarouselBaseItemEntity, pageConfig: PageConfig): Carousel {
        return Carousel(getBrights(carouselBaseItemEntity.items, pageConfig)).apply {
            layoutAttributes = PageModelMapper.createDefaultLayoutAttributes()
            //full bleed is default for the sake of animation/padding
            forceFullBleed = true
        }
    }

    fun getBrights(carouselItemsList: List<CarouselItem?>?, pageConfig: PageConfig): List<Bright> {
        return carouselItemsList?.mapNotNull {
            val media = MediaMapper.getMedia(it?.media, pageConfig)
            val link = HomepageStoryMapper.getLink(it?.link)
            val excerpt = HomepageStoryMapper.getExcerpt(it?.excerpt)
            if (it != null && media != null && link != null)
                Bright(media, link , excerpt)
            else
                null
        } ?: emptyList()
    }

    /**
     * Method to prepare [CarouselVideo] from given [CarouselItemEntity] data
     */
    fun getCarouselVideoItem(item: CarouselItemEntity?, layoutAttributes: LayoutAttributes, pageConfig: PageConfig): CarouselVideo? {
        item ?: return null
        val carouselVideoItems = mutableListOf<CarouselVideoItem>()
        item.items?.forEach {
            val media = MediaMapper.getMedia(it?.media, pageConfig)
            val video = media?.video
            video ?: return@forEach
            val videoId: String? = video.getStreamUrl()
            val isLive: Boolean = video.isLive
            val pageName: String? = video.omniture?.pageName
            val videoName: String? = video.omniture?.videoName
            val videoSection: String? = video.omniture?.videoSection
            val videoSource: String? = video.omniture?.videoSource
            val videoCategory: String? = video.omniture?.videoCategory
            val contentUrl = it?.link?.url
            val shareUrl = it?.link?.url
            val headline = it?.headline?.text
            val mediaUrl = media.url
            val aspectRatio = media.aspectRatio
            val contentId = video.omniture?.contentId
            val relatedLink = video.relatedContent?.link?.url
            val relatedLinkLastModified = video.relatedContent?.link?.lastModified
            val adTagUrl = if (video.adConfig?.playVideoAds == false) null else video.adConfig?.adSetUrl
            val postTvVideo = Video.Builder()
                .setId(videoId)
                .setContentUrl(contentUrl)
                .setIsLive(isLive)
                .setShareUrl(shareUrl)
                .setHeadline(headline)
                .setPageName(pageName)
                .setVideoName(videoName)
                .setVideoSection(videoSection)
                .setVideoSource(videoSource)
                .setVideoCategory(videoCategory)
                .setMediaUrl(mediaUrl)
                .setAdTagUrl(adTagUrl)
                .setAspectRatio(aspectRatio)
                .setDuration(video.duration ?: -1)
                .setContentId(contentId)
                .setRelatedLink(relatedLink, relatedLinkLastModified)
                .build()
            carouselVideoItems.add(CarouselVideoItem(media, postTvVideo))
        }
        return CarouselVideo(carouselVideoItems).apply {
            id = item.id
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    /**
     * Method to prepare [CarouselAudio] from given [CarouselItemEntity] data
     */
    fun getCarouselAudioItem(item: CarouselItemEntity?, layoutAttributes: LayoutAttributes, pageConfig: PageConfig): CarouselAudio? {
        item ?: return null
        val carouselAudioItems = mutableListOf<CarouselAudioItem>()
        item.items?.forEach {
            if (it?.carouselItemType == PERSONALIZED_PODCAST_PLACEHOLDER) {
                carouselAudioItems.add(CarouselAudioItem(null, null, "", null, null, it.carouselItemType))
            } else {
                val audioArticle =
                    AudioArticleMapper.getAudioArticle(it?.audioArticle, it?.tracking?.feed, pageConfig)
                val audio = AudioMapper.getAudio(it?.audio, pageConfig)
                val headline = it?.headline?.text
                if (!headline.isNullOrEmpty() && (audioArticle != null || audio != null)) {
                    val media = MediaMapper.getMedia(it.media, pageConfig)
                    val link = HomepageStoryMapper.getLink(it.link)
                    carouselAudioItems.add(
                        CarouselAudioItem(
                            audioArticle,
                            audio,
                            headline,
                            media,
                            link
                        )
                    )
                }
            }
        }
        return CarouselAudio(carouselAudioItems, item.cardify).apply {
            id = item.id
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    /**
     * Method to prepare [CarouselAudio] from given [CarouselItemEntity] data for Personalized Podcasts
     */
    fun getCarouselPersonalizedPodcastItem(
        itemEntity: CarouselItemEntity?,
        layoutAttributes: LayoutAttributes
    ): CarouselAudio? {
        itemEntity ?: return null
        val carouselAudioItems = mutableListOf<CarouselAudioItem>()
        carouselAudioItems.add(CarouselAudioItem(null, null, "", null, null, PLACEHOLDER))
        return CarouselAudio(carouselAudioItems, itemEntity.cardify).apply {
            id = itemEntity.id
            carouselType = ItemType.CAROUSEL_PERSONALIZED_PODCAST
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    /**
     * Method to prepare [CarouselAudioPlaylist] from given [CarouselItemEntity] data
     */
    fun getCarouselAudioPlaylistItem(item: CarouselItemEntity?, layoutAttributes: LayoutAttributes): CarouselAudioPlaylist? {
        item ?: return null

        val headlines = item.items?.mapNotNull {
            it?.headline?.text
        } ?: emptyList()

        val cta = item.cta?.let { HomepageStoryMapper.getLabel(it) }

        return CarouselAudioPlaylist(
            headlines = headlines,
            cta = cta,
            item.cardify
        ).apply {
            id = item.id
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    /**
     * Method to prepare [CarouselAudio] from given [CarouselItemEntity] data
     */
    fun getCarouselImmersionItem(itemEntity: CarouselItemEntity?, layoutAttributes: LayoutAttributes, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): CarouselImmersion? {
        itemEntity ?: return null
        val carouselImmersionItems = mutableListOf<CarouselImmersionItem>()
        itemEntity.items?.forEachIndexed { index, item ->
            val headline = item?.headline?.let { headline ->
                ImmersionHeadline(headline.text, headline.prefix?.let { "$it | " } ?: "")
            }
            val itId = HomepageStoryMapper.getItId(item?.link?.url, null, (index + 1).toString(), parentLinkGroup, tracking)
            var link = HomepageStoryMapper.getLink(item?.link)
            link = link?.copy(itId = itId)
            val media = MediaMapper.getMedia(item?.media, pageConfig)
            val signature = item?.signature?.byLine
            val kicker = if (item?.label?.type == CompoundLabelTypeEntity.KICKER)
                HomepageStoryMapper.getLabel(item.label)?.text else null
            val kickerStyle = item?.label?.style
            val byline = item?.label?.labelSecondary?.text
            if (media?.mediaType == MediaType.LIVE_IMAGE) {
                val itId = HomepageStoryMapper.getItId(media.link?.url, null, (index + 1).toString(), parentLinkGroup, tracking)
                val mediaWithItId = media.copy(link = media.link?.copy(itId = itId))
                if (!mediaWithItId.liveImage?.tabs.isNullOrEmpty() && !mediaWithItId.liveImage?.tabs?.get(0)?.images.isNullOrEmpty()) {
                    carouselImmersionItems.add(
                        CarouselImmersionItem(
                            headline ?: ImmersionHeadline(""),
                            mediaWithItId,
                            link ?: Link(LinkType.NONE, ""),
                            signature,
                            kicker,
                            true,
                            byline,
                            kickerStyle?.let { getStyle(it) }
                        )
                    )
                }
            } else {
                if (headline != null && link != null) {
                    carouselImmersionItems.add(
                        CarouselImmersionItem(
                            headline,
                            media,
                            link,
                            signature,
                            kicker,
                            false,
                            byline,
                            kickerStyle?.let { getStyle(it) }
                        )
                    )
                }
            }
        }
        val label = HomepageStoryMapper.getLabel(itemEntity.label)
        val cta = HomepageStoryMapper.getLabel(itemEntity.cta)
        val ctaItId = HomepageStoryMapper.getItId(cta?.link?.url, null, null, parentLinkGroup, tracking)
        cta?.link = cta?.link?.copy(itId = ctaItId)
        return CarouselImmersion(carouselImmersionItems, label, cta, itemEntity.cardify).apply {
            id = itemEntity.id
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    fun getCarouselRecipeItem(itemEntity: CarouselItemEntity?, layoutAttributes: LayoutAttributes, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): CarouselRecipe? {
        itemEntity ?: return null
        val carouselRecipeItems = mutableListOf<CarouselRecipeItem>()
        itemEntity.items?.forEachIndexed { index, item ->
            val headline = item?.headline
            val itId = HomepageStoryMapper.getItId(item?.link?.url, null, (index + 1).toString(), parentLinkGroup, tracking)
            var link = HomepageStoryMapper.getLink(item?.link)
            link = link?.copy(itId = itId)
            // Headline and Link objects should not be null for items to be considered
            if (headline != null && link != null) {
                val immersionHeadline = ImmersionHeadline(headline.text, headline.prefix?.let { "$it | " } ?: "")
                val media = MediaMapper.getMedia(item.media, pageConfig)
                val recipe = item.recipeInfo
                val rating = item.rating
                carouselRecipeItems.add(
                    CarouselRecipeItem(
                        immersionHeadline,
                        media,
                        link,
                        recipe,
                        rating
                    )
                )
            }
        }
        val label = itemEntity.label?.let { HomepageStoryMapper.getLabel(it) }
        val cta = HomepageStoryMapper.getLabel(itemEntity.cta)
        return CarouselRecipe(carouselRecipeItems, label, cta, itemEntity.cardify).apply {
            id = itemEntity.id
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    private fun getStyle(style: LabelStyleEntity): StyleEntity {
       return when (style) {
           LabelStyleEntity.OPINIONS -> StyleEntity.OPINIONS
           LabelStyleEntity.WP_INTELLIGENCE -> StyleEntity.WP_INTELLIGENCE
           LabelStyleEntity.THE_SEVEN_LIVE -> StyleEntity.THE_SEVEN_LIVE
       }
    }

    fun getCarouselComments(
        itemEntity: CarouselItemEntity?,
        layoutAttributes: LayoutAttributes,
        parentLinkGroup: String?,
        tracking: Tracking?
    ): CarouselComments? {
        itemEntity ?: return null
        val comments = mutableListOf<CarouselCommentsItem>()
        itemEntity.items?.forEachIndexed { index, item ->
            val itId = HomepageStoryMapper.getItId(
                item?.link?.url,
                null,
                (index + 1).toString(),
                parentLinkGroup,
                tracking
            )
            var link = HomepageStoryMapper.getLink(item?.link)
            link = link?.copy(itId = itId)
            if (item?.text != null && link != null) {
                comments.add(
                    CarouselCommentsItem(
                        link = link,
                        text = item.text,
                        authorName = item.author?.name,
                        authorRole = item.author?.role,
                        authorAvatarUrl = item.author?.avatar,
                        reactionCount = item.reactions?.count,
                        repliesCount = item.replies?.count,
                        repliesAvatarUrls = item.replies?.avatars?.toList(),
                    )
                )
            }
        }
        return CarouselComments(items = comments, cardify = itemEntity.cardify).apply {
            id = itemEntity.id
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    fun getCarouselExternalItems(itemEntity: CarouselItemEntity?, layoutAttributes: LayoutAttributes, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): CarouselExternal? {
        itemEntity ?: return null
        val carouselImmersionItems = getCarouselImmersionItem(
            itemEntity,
            getLayoutAttributes(itemEntity.layoutAttributes),
            parentLinkGroup,
            tracking,
            pageConfig
        )
        return carouselImmersionItems?.let {
            CarouselExternal(
                items = it.items.map {
                    CarouselExternalItem(
                        headline = it.headline.let {
                            ExternalHeadline(text = it.text, prefix = it.prefix)
                        },
                        media = it.media,
                        link = it.link,
                        signature = it.signature,
                        kicker = it.kicker,
                        liveImage = it.liveImage,
                        secondaryText = it.secondaryText,
                        style = it.style,
                    )
                },
                label = it.label,
                cta = it.cta,
                cardify = it.cardify,
            ).apply {
                id = itemEntity.id
                forceFullBleed = true
                this.layoutAttributes = layoutAttributes
            }
        }
    }

    fun getCarouselSevenLive(itemEntity: CarouselItemEntity?, layoutAttributes: LayoutAttributes, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): CarouselSevenLive? {
        itemEntity ?: return null
        val carouselImmersionItems = getCarouselImmersionItem(
            itemEntity,
            getLayoutAttributes(itemEntity.layoutAttributes),
            parentLinkGroup,
            tracking,
            pageConfig
        ) ?: return null

        return CarouselSevenLive(
            items = carouselImmersionItems.items.mapIndexed { index, immersionItem ->
                val rawTimestamp = itemEntity.items?.getOrNull(index)?.signature?.timestamp
                val timestamp = normalizeTimestamp(rawTimestamp)
                CarouselSevenLiveItem(
                    headline = ExternalHeadline(
                        text = immersionItem.headline.text,
                        prefix = immersionItem.headline.prefix
                    ),
                    media = immersionItem.media,
                    link = immersionItem.link,
                    signature = immersionItem.signature,
                    kicker = immersionItem.kicker,
                    liveImage = immersionItem.liveImage,
                    secondaryText = immersionItem.secondaryText,
                    style = immersionItem.style,
                    timestamp = timestamp,
                )
            },
            label = carouselImmersionItems.label,
            cta = carouselImmersionItems.cta,
            cardify = carouselImmersionItems.cardify,
        ).apply {
            id = itemEntity.id
            forceFullBleed = true
            this.layoutAttributes = layoutAttributes
        }
    }

    fun mapPersoPodToAudioItem(persoPod: PersonalizedPodcast): CarouselAudioItem? {
        val audioPath = persoPod.audioFilePath ?: "null"
        val imagePath = persoPod.image
        val audioDuration = persoPod.audioDuration?.toLong()
        val sources = persoPod.articlesUsed?.map { it ->
            ArticleSource(
                headline = it.headline,
                canonicalUrl = it.canonicalUrl,
                publishDate = it.displayDate,
                text = it.text,
                imageUrl = it.imageUrl
            )
        }

        val playerMedia = Media(
            promoImageURL = imagePath ?: "", mediaType = MediaType.FEATURED, aspectRatio = 1.5f, artWidth = ArtWidth.FULL_WIDTH, url = imagePath,
            bleed = Bleed.FULL, height = 0, width = 0, artPosition = null, overlay = null, caption = null, video = null, liveImage = null,
            link = null, dynamicReplacement = null, altText = null, makeItRound = null
        )

        val audio = Audio(
            mediaId = persoPod.id, streamUrl = audioPath, duration = audioDuration, title = persoPod.title, coverImage = imagePath,
            subscriptionLinks = SubscriptionLinks(), playerType = PlayerType.PODCAST, playerMediaEntity = playerMedia, tracking = AudioTracking(persoPod.kicker, persoPod.title),
            displayLabel = persoPod.kicker, displayDate = persoPod.createdAt, transcriptUrl = persoPod.transcript, sources = sources
        )

        val headline = persoPod.title
        val link = Link(LinkType.NONE, url = audioPath)

        if (!headline.isNullOrEmpty()) {
            return CarouselAudioItem(
                null,
                audio,
                headline,
                playerMedia,
                null,
                carouselItemType = persoPod.itemType
            )
        }
        return null
    }
    /**
     * Normalizes raw timestamp strings into the app's date format.
     *
     * Background:
     * Timestamps arrive in two different formats depending on the section:
     * - **Articles Section:** Timestamps are formatted client-side via [Utils.dateToString],
     *   producing millisecond-precision strings (e.g., `2026-07-23T11:00:00.000Z`).
     * - **Home Section :** Provides raw `signature.timestamp` strings directly from backend
     *   payloads that omit milliseconds (e.g., `2026-07-23T11:00:00Z`).
     *
     * To ensure UI holders ([CarouselSevenLiveHolder] and Compose views) can safely parse dates using
     * [Utils.getDefaultDateFormat] across both Home and Article contexts, this function normalizes raw
     * backend strings into canonical millisecond-precision ISO strings.
     *
     * @param rawTimestamp The unparsed ISO-8601 string from `signature.timestamp`.
     * @return Standardized ISO-8601 timestamp string containing milliseconds, or original/null if unparseable.
     */
    private fun normalizeTimestamp(rawTimestamp: String?): String? {
        if (rawTimestamp.isNullOrBlank()) return null

        val isoWithMillis = Utils.getDefaultDateFormat()
        val isoWithoutMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }

        val dateLong = Utils.toDateLong(rawTimestamp, isoWithMillis)
            .takeIf { it > 0L }
            ?: Utils.toDateLong(rawTimestamp, isoWithoutMillis)

        return if (dateLong > 0L) {
            Utils.dateToString(Date(dateLong))
        } else {
            rawTimestamp
        }
    }
}