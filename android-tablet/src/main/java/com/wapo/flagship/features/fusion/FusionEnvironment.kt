package com.wapo.flagship.features.fusion

import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import com.wapo.android.commons.util.Logger
import android.widget.Adapter
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.URLParser
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.toDateLong
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.FusionActivity
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.common.getSectionsAdTargetingValues
import com.wapo.flagship.features.articles.recirculation.CarouselEnvironment
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.articles2.utils.appendTrackingParams
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.BuildConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfigList
import com.wapo.flagship.features.audio.config2.AudioMediaSubscriptionLinks
import com.wapo.flagship.features.audio.models.PlaybackVoice
import com.wapo.flagship.features.audio.playlist.Playlist
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.grid.BarEntity
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.grid.VoiceUtils
import com.wapo.flagship.features.grid.model.AudioArticle
import com.wapo.flagship.features.grid.model.AudioArticleTracking
import com.wapo.flagship.features.grid.model.Carousel
import com.wapo.flagship.features.grid.model.CarouselAudio
import com.wapo.flagship.features.grid.model.CarouselAudioItem
import com.wapo.flagship.features.grid.model.CarouselAudioPlaylist
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.model.SectionInlineMessage
import com.wapo.flagship.features.grid.model.HomepageStory
import com.wapo.flagship.features.grid.model.Link
import com.wapo.flagship.features.grid.model.LinkType
import com.wapo.flagship.features.grid.model.RelatedLinkItem
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.features.grid.model.Video
import com.wapo.flagship.features.grid.views.vote.VoteGuideService
import com.wapo.flagship.features.newsprint.NewsprintHelper
import com.wapo.flagship.features.pagebuilder.AdViewFactory
import com.wapo.flagship.features.sections.SectionsPagerView
import com.wapo.flagship.features.sections.SubscribeButton
import com.wapo.flagship.features.sections.model.TargetingContent
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.video.VerticalVideosParcel
import com.wapo.flagship.features.video.VideoActivity
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.newsprintEngagedStatus
import com.wapo.flagship.util.newsprintHasViewed
import com.wapo.flagship.util.newsprintReaderType
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.view.habittiles.Tile
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import kotlinx.coroutines.flow.Flow

class FusionEnvironment(
    private val fusionActivity: FusionActivity,
) : GridEnvironment {
    private val breakingNewsTracker = BreakingNewsTracker(fusionActivity)
    private var reportedStackPositions = mutableMapOf<Carousel, Int>()

    // we only need to report carousel scroll event one time
    private var reportedCarouselEvents = mutableSetOf<Carousel>()

    private val config get() = ConfigManager.getInstance().config

    override fun getAdViewFactory(): AdViewFactory = fusionActivity.adViewFactory

    override fun isNightModeEnabled(): Boolean = fusionActivity.isNightMode()

    override fun isPhone(): Boolean = fusionActivity.isPhone

    override fun isPortraitPhone(): Boolean =
        fusionActivity.isPhone && fusionActivity.resources.configuration.orientation == ORIENTATION_PORTRAIT

    override fun bookMarkClicked(
        ellipseActionItem: EllipsisActionItem,
        view: ImageView,
        isStatusChecked: Boolean,
    ) {
        fusionActivity.recipeCarouselBookmarkClicked(ellipseActionItem, view, isStatusChecked)
    }

    override fun openHabitTileLink(
        link: String?,
        sectionDisplayName: String,
        trackingSection: String?,
        trackingSubsection: String?,
    ) {
        link ?: return
        FlagshipApplication.getInstance().releaseVideoManager()
        FlagshipApplication.getInstance().releaseVideoManager2()
        fusionActivity.openHabitTileLink(link)
        Measurement.setHabitTilesNavigationBehavior(trackingSection, trackingSubsection)
    }

    override fun trackHabitTileClicked(link: String?) {
        fusionActivity.habitTilesPlugin.addClickedToHabitTileViewedItem(link)
    }

    override fun openWatchVideoCard(
        postTvVideos: List<com.wapo.flagship.features.posttv.model.Video>,
        sectionDisplayName: String,
        tabName: String,
        position: Int,
        offset: Int
    ) {
        val intent =
            VerticalVideosParcel
                .Builder()
                .setPostTvVideos(postTvVideos)
                .setPosition(position)
                .setOffset(offset)
                .setSourceScreen(sectionDisplayName)
                .setTabName(tabName)
                .setVerticalVideosConfig(ConfigManager.getInstance().config.verticalVideosConfig)
                .buildIntent(fusionActivity)
        fusionActivity.startActivity(intent)
    }

    override fun openForYouVideoCard(
        video: com.wapo.flagship.features.posttv.model.Video,
        sectionDisplayName: String
    ) {
        val intent =
            VerticalVideosParcel.Builder()
                .setPostTvVideos(listOf(video))
                .setPosition(0)
                .setOffset(0)
                .setSourceScreen(sectionDisplayName)
                .setTabName(BottomTab.Home.trackingName)
                .setVerticalVideosConfig(ConfigManager.getInstance().config.verticalVideosConfig)
                .buildIntent(fusionActivity)
        fusionActivity.startActivity(intent)
    }

    override fun isLowDataModeEnable() = fusionActivity.isLowDataModeEnable
    override fun isPremiumAccount() = fusionActivity.isPremiumAccount

    override fun openArticle(
        story: HomepageStory,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
        position: Int,
    ) {
        val isArticleFallbackMode = !ReachabilityUtil.isConnected(fusionActivity)
        val link = story.getLink(isArticleFallbackMode)
        if (link != null) {
            openLink(
                link.url,
                link.type,
                grid,
                sectionDisplayName,
                bundleId,
                true,
                itId = story.itId,
            )
        }
    }

    override fun openArticleUrl(
        url: String?,
        story: HomepageStory,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
        position: Int
    ) {
        if (!url.isNullOrEmpty()) {
            openLink(
                url,
                LinkType.ARTICLE,
                null,
                sectionDisplayName,
                bundleId,
                true,
                itId = story.itId,
            )
        }
    }

    override fun openMedia(
        story: HomepageStory,
        link: Link,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
    ) {
        openLink(link.url, link.type, grid, sectionDisplayName, bundleId, true, story.itId)
    }

    override fun openRelatedLink(
        relatedLink: RelatedLinkItem,
        story: HomepageStory,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
    ) {
        openLink(
            relatedLink.link ?: return,
            relatedLink.type,
            grid,
            sectionDisplayName,
            bundleId,
            true,
            story.itId,
        )
    }

    override fun openLabel(
        label: CompoundLabel,
        itId: String?,
    ) {
        val link = label.link ?: return
        openLink(link.url, link.type, null, "", null, false, pathToView = itId, itId = itId)
    }

    override fun getPager(): SectionsPagerView? = fusionActivity.pager

    override fun openLiveBlog(
        link: String,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
        itId: String?
    ) {
        openLink(link, LinkType.ARTICLE, grid, sectionDisplayName, bundleId, true, itId = itId, isLiveBlogOriginated = true)
    }

    override fun getLiveBlogProxyUrl(): String = config.liveBlogServiceURL

    override fun remoteLogError(
        tag: String,
        throwable: Throwable,
    ) {
        val log = tag + ":" + throwable.message
        EventLog
            .Builder()
            .apply {
                setMessage("Fusion Environment Error")
                setModule(LogModules.SECTIONS)
                setErrorMessage(log)
            }.run {
                RemoteLog.e(fusionActivity.applicationContext, build())
            }
    }

    override fun isConnected(): Boolean = ReachabilityUtil.isConnected(fusionActivity)

    override fun getCarouselNetworkRequestsHelper(): CarouselProvider = CarouselEnvironment(fusionActivity, null)

    override fun getAdTagUrl(
        video: Video,
        story: HomepageStory,
        targetingContent: TargetingContent?
    ): String? =
        com.wapo.flagship.common
            .getAdTagUrl(video, story, targetingContent)

    override fun getSubscribeButton(sectionDisplayName: String?): SubscribeButton? = fusionActivity.getSubscribeButton(sectionDisplayName)

    override fun onRefresh(
        bundleName: String,
        sectionDisplayName: String,
    ) {
        FlagshipApplication
            .getInstance()
            .contentManager
            .updateConfigs()
            .take(1)
            .subscribe(
                {
                    Logger.d("FusionEnvironment", "configs updated")
                },
                { e ->
                    if (BuildConfig.DEBUG) {
                        Logger.d("FusionEnvironment", "configs update failed", e)
                    }
                },
            )
    }

    override fun getVoteGuideService(): VoteGuideService =
        FlagshipApplication
            .getInstance()
            .contentManager

    override fun openLiveVideoBar(link: String) {
        fusionActivity.openLiveVideo(link)
    }

    private fun openLink(
        link: String,
        linkType: LinkType,
        grid: Grid?,
        sectionDisplayName: String?,
        bundleId: String? = null,
        isArticle: Boolean,
        pathToView: String? = null,
        positionInBrights: Int = 0,
        itId: String? = null,
        isLiveBlogOriginated: Boolean = false,
    ) {
        FlagshipApplication.getInstance().releaseVideoManager()
        val isArticleFallbackMode = !ReachabilityUtil.isConnected(fusionActivity)

        Measurement.setNavigationBehavior(
            Measurement.getDefaultMap(),
            if (willOpenWeb(linkType, isArticle)) null else pathToView,
        )

        when (linkType) {
            LinkType.ARTICLE -> {
                openNativeArticleFromCarousel(
                    grid,
                    isArticleFallbackMode,
                    link,
                    linkType,
                    sectionDisplayName,
                    bundleId,
                    pathToView,
                    positionInBrights,
                    itId = itId,
                    isLiveBlogOriginated
                )
            }
            LinkType.WEB, LinkType.GALLERY -> {
                val url = appendTrackingParams(link, itId, fusionActivity.activeTabName)
                if (DeepLinksProcessor.canProcessWebTypeLink(link)) {
                    DeepLinksProcessor.processAsync(link, scope = fusionActivity.lifecycleScope)
                } else {
                    //open all other web/gallery links in SimpleWebviewActivity
                    fusionActivity.openWeb(url)
                }
            }
            LinkType.VIDEO -> openVideo(link)
            LinkType.NONE -> {
                openUnknownLink(link)
            }
        }
    }

    private fun willOpenWeb(
        linkType: LinkType,
        isArticle: Boolean,
    ): Boolean = (linkType == LinkType.WEB || linkType == LinkType.GALLERY)

    private fun openUnknownLink(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.parse(link)
            intent.data = uri
            intent.putExtra(DeepLinksProcessor.ARG_URL_PARSER, URLParser(link))
            IntentHelper().offer(intent, fusionActivity)
        } catch (t: Throwable) {
            Logger.e("FusionEnvironment", "Failed to open unknown link: $link", t)
        }
    }

    private fun openVideo(link: String) {
        fusionActivity.startActivity(
            Intent(fusionActivity, VideoActivity::class.java)
                .putExtra(VideoActivity.VideoInfoUrlExtraParamName, link),
        )
    }

    private fun openNativeArticleFromCarousel(
        grid: Grid?,
        isArticleFallbackMode: Boolean,
        link: String,
        linkType: LinkType?,
        sectionDisplayName: String?,
        bundleId: String? = null,
        pathToView: String? = null,
        positionInCarousel: Int,
        itId: String? = null,
        isLiveBlogOriginated: Boolean = false
    ) {
        val articles =
            grid?.getArticles(
                isArticleFallbackMode,
                config.webArticleConfig,
            )
        val index = articles?.indexOfFirst { it.id == link } ?: -1
        if (index > -1) {
            ArticlesParcel
                .builder()
                .setArticleMetas(articles, index)
                .setArticleSingleUrl(link)
                .setLinkType(linkType)
                .setSectionDisplayName(sectionDisplayName)
                .setTabName(fusionActivity.activeTabName)
                .setAppSection(sectionDisplayName)
                .setOmniturePathToView(pathToView)
                .setPositionInCarousel(positionInCarousel)
                .setItId(itId)
                .setArticleOpenedFromSectionFront(true)
                .liveBlogOriginated(isLiveBlogOriginated)
                .buildIntent(fusionActivity)
                .also { fusionActivity.startActivity(it) }
        } else {
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(link)
                .setLinkType(linkType)
                .setSectionDisplayName(sectionDisplayName)
                .setTabName(fusionActivity.activeTabName)
                .setAppSection(sectionDisplayName)
                .setOmniturePathToView(pathToView)
                .setPositionInCarousel(positionInCarousel)
                .setItId(itId)
                .setArticleOpenedFromSectionFront(true)
                .buildIntent(fusionActivity)
                .also { fusionActivity.startActivity(it) }
        }
    }

    private fun openNativeArticleFromCarousel(
        links: List<String>,
        linkType: List<LinkType?>,
        sectionDisplayName: String?,
        pathToView: String? = null,
        positionInCarousel: Int,
        itId: String? = null,
    ) {
        ArticlesParcel
            .builder()
            .setArticleUrls(links, positionInCarousel)
            .setArticleSingleUrl(links[positionInCarousel])
            .setLinkType(linkType[positionInCarousel])
            .setSectionDisplayName(sectionDisplayName)
            .setNewsprintOriginated(
                NewsprintHelper.isNewsprintSection(sectionDisplayName.orEmpty()),
            ).setTabName(fusionActivity.activeTabName)
            .setAppSection(sectionDisplayName)
            .setOmniturePathToView(pathToView)
            .setCarouselOriginated(true)
            .setPositionInCarousel(positionInCarousel)
            .setItId(itId)
            .setArticleOpenedFromSectionFront(true)
            .setShouldNotSuppressPageView(sectionDisplayName == BottomTab.Games.title)
            .buildIntent(fusionActivity)
            .also { fusionActivity.startActivity(it) }
    }

    override fun openBreakingNewsBar(link: String) {
        fusionActivity.openBreakingNews(link)
    }

    override fun onBreakingNewsBarClosed(barEntity: BarEntity) {
        breakingNewsTracker.setBarClosed(barEntity)
    }

    override fun shouldShowBreakingNewsBar(barEntity: BarEntity): Boolean = !breakingNewsTracker.isBarClosed(barEntity)

    override fun onVoteGuideClicked(url: String) {
        fusionActivity.openWeb(url)
    }

    override fun openLink(url: String) {
        fusionActivity.openWeb(url)
    }

    override fun setNavigationBehaviorInDefaultMap(navigationBehavior: String) {
        Measurement.setNavigationBehaviorInDefaultMap(navigationBehavior)
    }

    override fun openCarouselCard(
        links: List<Link>,
        sectionDisplayName: String,
        position: Int,
    ) {
        openNativeArticleFromCarousel(
            links.map { it.url },
            links.map { it.type },
            sectionDisplayName,
            Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL,
            position,
            itId = Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL,
        )
    }

    override fun openCarouselImmersionCard(
        links: List<Link>,
        sectionDisplayName: String,
        position: Int,
    ) {
        openNativeArticleFromCarousel(
            links.map { appendTrackingParams(it.url, it.itId, fusionActivity.activeTabName) },
            links.map { it.type },
            sectionDisplayName,
            Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL_IMMERSION,
            position,
            itId = links[position].itId,
        )
    }

    override fun openCarouselSevenLiveCard(
        links: List<Link>,
        sectionDisplayName: String,
        position: Int,
    ) {
        openNativeArticleFromCarousel(
            links.map { appendTrackingParams(it.url, it.itId, fusionActivity.activeTabName) },
            links.map { it.type },
            sectionDisplayName,
            Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL_SEVEN_LIVE,
            position,
            itId = links[position].itId,
        )
    }

    override fun openCarouselRecipeCard(
        links: List<Link>,
        sectionDisplayName: String,
        position: Int,
    ) {
        links.getOrNull(position) ?: return
        val itId = links.getOrNull(position)?.itId
        val isWebTypeLink = links.getOrNull(position)?.type == LinkType.WEB
        val link = links.getOrNull(position)?.url
        val url = appendTrackingParams(link!!, itId, fusionActivity.activeTabName)
        if (isWebTypeLink) {
            //open all other web/gallery links in SimpleWebviewActivity
            fusionActivity.openWeb(url)
        } else {
            openNativeArticleFromCarousel(
                links.map { it.url },
                links.map { it.type },
                sectionDisplayName,
                Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL_RECIPE,
                position,
                itId = itId,
            )
        }
    }

    override fun openStackCard(
        links: List<Link>,
        sectionDisplayName: String,
        position: Int,
    ) {
        openNativeArticleFromCarousel(
            links.map { it.url },
            links.map { it.type },
            sectionDisplayName,
            Measurement.PATH_TO_VIEW_FRONTS_STACK,
            position,
            itId = Measurement.PATH_TO_VIEW_FRONTS_STACK,
        )
    }

    override fun openCarouselVideoCard(
        postTvVideos: List<com.wapo.flagship.features.posttv.model.Video>,
        grid: Grid,
        sectionDisplayName: String,
        position: Int,
    ) {
        val intent =
            VerticalVideosParcel
                .Builder()
                .setPostTvVideos(postTvVideos)
                .setOffset(0)
                .setPosition(position)
                .setSourceScreen(sectionDisplayName)
                .setVerticalVideosConfig(config.verticalVideosConfig)
                .buildIntent(fusionActivity)
        fusionActivity.startActivity(intent)
    }

    override fun openCarouselCommentsCard(link: Link, sectionDisplayName: String, position: Int) {
        val url = appendTrackingParams(link.url, link.itId, fusionActivity.activeTabName)
        openLink(url)
    }

    override fun openCarouselExternalCard(
        links: List<Link>,
        sectionDisplayName: String,
        position: Int,
    ) {
        openNativeArticleFromCarousel(
            links.map { appendTrackingParams(it.url, it.itId, fusionActivity.activeTabName) },
            links.map { it.type },
            sectionDisplayName,
            Measurement.PATH_TO_VIEW_FRONTS_CAROUSEL_EXTERNAL,
            position,
            itId = links[position].itId,
        )
    }

    override fun playAudioCarouselAudioArticleItem(
        carouselAudio: CarouselAudio,
        position: Int,
        sectionDisplayName: String?,
        audioTracker: AudioTracker?
    ) {
        val clickedItem = carouselAudio.items.getOrNull(position) ?: return
        val audioMediaConfigList = generateAudioMediaConfigs(carouselAudio, sectionDisplayName)
        if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(clickedItem.carouselItemType)) {
            val singleAudioConfig = generateAudioMediaConfig(clickedItem, sectionDisplayName, audioTracker)
            if (singleAudioConfig != null) {
                playAudioIfHasAccess(singleAudioConfig)
            }
        } else {
            var newPosition = position
            if (carouselAudio.items.any { PersonalizedPodcastHelper.isPersonalizedPodcastItem(it.carouselItemType) }) {
                newPosition = audioMediaConfigList.list.indexOfFirst {
                    getUrlWithoutParameters(it.contentUrl ?: "") ==
                            getUrlWithoutParameters(clickedItem.link?.url ?: "")
                }
                if (newPosition == -1) {
                    return
                }
            }
            fusionActivity.playCarouselAudio(
                audioMediaConfigList,
                newPosition,
                carouselAudio.items[position]
                    .audioArticle
                    ?.tracking
                    ?.pageName,
                carouselAudio.items[position]
                    .audioArticle
                    ?.tracking
                    ?.arcId,
            )
        }
    }

    override fun playAudioCarouselAudioPlaylistArticleItem(
        carouselAudioPlaylist: CarouselAudioPlaylist,
        playListItems: List<Playlist>,
        position: Int,
        sectionDisplayName: String?,
    ) {
        val audioMediaConfigs = playListItems.map { it.toAudioMediaConfig() }
        fusionActivity.playCarouselAudio(
            AudioMediaConfigList(
                id = carouselAudioPlaylist.id,
                list = audioMediaConfigs as MutableList<AudioMediaConfig>,
            ),
            position,
            "",
            "",
        )
    }

    override fun playAudioIfHasAccess(audioMediaConfig: AudioMediaConfig) {
        fusionActivity.playAudioArticle(audioMediaConfig, audioMediaConfig.arcId)
    }

    override fun generateAudioMediaConfig(
        audioArticle: AudioArticle,
        isFlexFeature: Boolean,
        isActionButton: Boolean,
    ): AudioMediaConfig =
        AudioMediaConfig(
            humanAdsUrl = audioArticle.humanVoice?.adsUrl,
            humanRawUrl = audioArticle.humanVoice?.rawUrl,
            titlePrefix = audioArticle.titlePrefix,
            titleSeparator = audioArticle.titleSeparator,
            title = audioArticle.title,
            primaryLabel = audioArticle.label?.text,
            secondaryLabel = audioArticle.label?.secondaryText,
            date = toDateLong(audioArticle.displayDate),
            imageUrl = audioArticle.playerMedia?.url,
            imageCaption = audioArticle.playerMedia?.caption,
            contentUrl = audioArticle.contentUrl?.let { getUrlWithoutParameters(it) },
            sectionName = audioArticle.tracking?.section,
            caption = audioArticle.caption,
            duration = VoiceUtils.getPreferredVoiceDuration(audioArticle),
            voices =
                audioArticle.voices
                    ?.mapNotNull {
                        if (it.voiceId != null && it.rawUrl != null && it.label != null) {
                            PlaybackVoice(it.voiceId!!, it.label!!, it.rawUrl!!, it.adsUrl, it.duration)
                        } else {
                            null
                        }
                    }?.toMutableList(),
            arcId = audioArticle.tracking?.arcId,
            audioTracking =
                AudioTrackerImpl(
                    fusionActivity.activeTabName,
                    fusionActivity.getCurrentSectionDisplayName(),
                    audioArticle.tracking?.let { getCurrentTrackingInfo(it) },
                    false,
                    AudioPreferences.getAudioPlaybackSpeed(fusionActivity.applicationContext),
                    false,
                    audioArticle.feed,
                    isFlexFeature,
                    isActionButton,
                ),
            adsCustomTargeting = getSectionsAdTargetingValues(null, null, audioArticle.contentUrl),
            labelStyle = audioArticle.label?.style?.let { getLabelStyle(it) },
            adConfig = audioArticle.adConfig,
        )

    private fun getLabelStyle(label: CompoundLabel.LabelStyle): String? =
        when (label) {
            CompoundLabel.LabelStyle.OPINIONS -> "opinions"
            else -> null
        }

    private fun generateAudioMediaConfigs(
        carouselAudio: CarouselAudio?,
        sectionName: String?,
    ): AudioMediaConfigList {
        val audioMediaConfigs =
            carouselAudio?.items?.mapNotNull { item ->
                if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(item.carouselItemType)) {
                    null
                } else {
                generateAudioMediaConfig(item, sectionName)
            }} ?: emptyList()

        return AudioMediaConfigList(carouselAudio?.id, audioMediaConfigs.toMutableList())
    }

    override fun generateAudioMediaConfig(
        carouselAudioItem: CarouselAudioItem?,
        sectionName: String?,
        audioTracker: AudioTracker?,
    ): AudioMediaConfig? {
        return if (carouselAudioItem?.audioArticle != null) {
            val audioArticle = carouselAudioItem.audioArticle ?: return null
            audioArticle.toAudioMediaConfig(
                audioTracker = AudioTrackerImpl(
                    fusionActivity.activeTabName,
                    sectionName,
                    audioArticle.tracking?.let { getCurrentTrackingInfo(it) },
                    false,
                    AudioPreferences.getAudioPlaybackSpeed(fusionActivity.applicationContext),
                    true,
                    audioArticle.feed,
                ),
                adsCustomTargeting = getSectionsAdTargetingValues(null, null, audioArticle.contentUrl),
                labelStyle = audioArticle.label?.style?.let { getLabelStyle(it) },
                audioType = carouselAudioItem.carouselItemType,
            )
        } else if (carouselAudioItem?.audio != null) {
            val audio = carouselAudioItem.audio ?: return null
            audio.toAudioMediaConfig(
                imageUrlFallback = carouselAudioItem.media?.url,
                contentUrl = carouselAudioItem.link?.url,
                audioTracker = audioTracker ?: AudioTrackerImpl(
                    tabName = fusionActivity.activeTabName,
                    appSection = sectionName,
                    duration = audio.duration ?: 0L,
                    feed = audio.tracking?.seriesSlug,
                    isAudioCarousel = true,
                    avName = audio.tracking?.audioName,
                ),
                adsCustomTargeting = getSectionsAdTargetingValues(null, null, carouselAudioItem.link?.url),
                audioType = carouselAudioItem.carouselItemType,
            )
        } else {
            null
        }
    }

    private fun getCurrentTrackingInfo(tracking: AudioArticleTracking): TrackingInfo =
        TrackingInfo().also {
            it.arcId = tracking.arcId
            it.authorId = tracking.authorId
            it.contentAuthor = tracking.authorName
            it.newsroomDesk = tracking.authorDesk
            it.newsroomSubdesk = tracking.authorSubdesk
            it.trackingTags = tracking.trackingTags
            it.commercialNode = tracking.commercialNode
            it.contentCategory = tracking.contentCategory
            it.contentType = tracking.contentType
            it.pageName = tracking.pageName
            it.audioFirstPublishDate = tracking.firstPublishDate
            it.headline = tracking.headline
            it.hierarchy = tracking.hierarchy
            it.primarySection = tracking.section
            it.subSection = tracking.subsection
            it.source = tracking.source
            it.pageName = tracking.pageName
            it.title = tracking.pageName
        }

    override fun provideStackViewAdapter(carousel: Carousel): Adapter = StackViewAdapter(carousel)

    override fun onStackViewCardChanged(
        carousel: Carousel,
        pos: Int,
    ) {
        super.onStackViewCardChanged(carousel, pos)
        val reportedPos = reportedStackPositions[carousel] ?: -1
        if (reportedPos != -1) {
            if (pos > reportedPos) {
                Measurement.trackBrightInteraction(
                    Measurement.NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_FORWARD,
                    pos,
                )
            }
            if (pos < reportedPos) {
                Measurement.trackBrightInteraction(
                    Measurement.NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_BACK,
                    pos,
                )
            }
        }
        reportedStackPositions[carousel] = pos
    }

    override fun onCarouselScrollStateChanged(
        carousel: Carousel,
        newState: Int,
    ) {
        super.onCarouselScrollStateChanged(carousel, newState)
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            if (!reportedCarouselEvents.contains(carousel)) {
                reportedCarouselEvents.add(carousel)
                Measurement.trackBrightInteraction(
                    Measurement.NAVIGATION_BEHAVIOR_BRIGHT_SWIPE_CAROUSEL,
                    -1,
                )
            }
        }
    }

    override fun getHabitTiles(): List<Tile> {
//         return tilesPreviewList
        return fusionActivity.getHabitTiles()
    }

    override fun isLoggedInUser(): Boolean =
        PaywallService.initialized() && PaywallService.getInstance().isWpUserLoggedIn

    override fun isSaveEnabled(): Boolean = config.actionButtonsConfig.saveEnabled

    override fun getNewsprintEngagedStatus(): Flow<String> = newsprintEngagedStatus

    override fun getNewsprintHasViewed(): Flow<Boolean> = newsprintHasViewed

    override fun getNewsprintReaderType(): Flow<String> = newsprintReaderType

    override fun showSaveRegwall(context: Context) {
        context.findActivityOfType<BaseActivity>()?.getPaywallSheetHelper()?.showWall(
            wallName = PaywallConstants.WALL_NAME_SAVE_REGWALL,
            wallType = PaywallConstants.WallType.SAVE_REGWALL,
            wallReason = PaywallConstants.WallType.SAVE_REGWALL.ordinal,
        )
    }

    override fun isAdsContentContextualTargetingEnabled(): Boolean {
        return config.adsConfig.contextualTargeting.content.enabled
    }

    override fun getSectionInlineMessage(): SectionInlineMessage? = fusionActivity.sectionInlineMessage
    override fun shouldSuppressAds(): Boolean = fusionActivity.shouldSuppressAds()
    override fun getPageConfig(): PageConfig = PageConfig.build(config)
}
