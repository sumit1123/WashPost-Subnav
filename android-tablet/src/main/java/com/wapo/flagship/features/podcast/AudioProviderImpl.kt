package com.wapo.flagship.features.podcast

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.AppContextUtils.showToast
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.FlagshipApplication.Companion.getInstance
import com.wapo.flagship.Utils
import com.wapo.flagship.common.getSectionsAdTargetingValues
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaSubscriptionLinks
import com.wapo.flagship.features.audio.models.MediaItemData
import com.wapo.flagship.features.audio.models.PlaybackVoice
import com.wapo.flagship.features.audio.playlist.AudioTrackingInfo
import com.wapo.flagship.features.audio.service2.media.FOR_YOU_SECTION_ID
import com.wapo.flagship.features.audio.service2.media.FOR_YOU_SECTION_NAME
import com.wapo.flagship.features.audio.service2.media.PODCAST_SECTION_ID
import com.wapo.flagship.features.audio.service2.media.PODCAST_SECTION_NAME
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.grid.VoiceUtils
import com.wapo.flagship.features.grid.model.AudioArticleTracking
import com.wapo.flagship.features.grid.model.CarouselAudio
import com.wapo.flagship.features.grid.model.CarouselAudioItem
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.model.PageModelMapper
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.ImageServiceConfig
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import java.net.MalformedURLException
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.toDateLong
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.ads.model.AdRequestContext
import com.wapo.flagship.features.audio.playlist.AudioSubtype
import com.wapo.flagship.features.deeplinks.OneLinkGeneratedStatus
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.ONBOARDING
import com.wapo.flagship.util.Share
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.features.ccpa.getCCPAAdsPrivacyString
import com.washingtonpost.android.paywall.features.ccpa.isCCPAOptedOut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.roundToLong

class AudioProviderImpl : AudioProvider {

    var podcastTracking: Tracking? = null
    val config get() = ConfigManager.getInstance().config
    val contentPacksRepo get() = getInstance().contentPacksRepo

    override fun onPodcastEvent(
        type: AudioProvider.EventType,
        mediaItemData: MediaItemData?,
        value: Any?,
        audioTracker: AudioTracker?,
        duration: Long?,
        progressThreshold: Int,
    ) {
        var persoPodTrackingData = audioTracker?.getPersoPodTrackingInfo()
        val audioTracking = audioTracker as? AudioTrackerImpl
        val isPersoPod =
            PersonalizedPodcastHelper.isPersonalizedPodcastItem(mediaItemData?.audioType)
        val formattedDate =
            if (mediaItemData != null) {
                if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(mediaItemData.audioType)) {
                    mediaItemData.displayDate
                } else {
                    Utils.getDisplayDate(
                        mediaItemData.displayDate,
                        "yyyyMMdd"
                    )
                }
            } else {
                null
            }
        val durationSeconds =
            duration
                ?.toFloat()
                ?.plus(999f)
                ?.div(1000f)
                ?.roundToLong()
        Measurement.setProgressThreshold(Measurement.getDefaultMap(), progressThreshold + 1)
        val avName = audioTracking?.avName?.takeIf { it.isNotBlank() }
        when (type) {
            AudioProvider.EventType.ON_PLAY_STARTED -> {
                if (mediaItemData?.seriesSlug is String &&
                    (mediaItemData.podcastSlug is String || (!isPersoPod && avName != null)) &&
                    formattedDate != null
                ) {
                    val currentPersoData = persoPodTrackingData
                    if (currentPersoData?.first?.isRollThrough == true) {
                        currentPersoData.second.let { secondInfo ->
                            audioTracker?.setPersoPodTrackingInfo(
                                Pair(
                                    PersoPodTrackingInfo(
                                        date = secondInfo?.date,
                                        touchpoint = secondInfo?.touchpoint,
                                        podcastType = ONBOARDING,
                                        tags = secondInfo?.tags,
                                        id = secondInfo?.id,
                                        title = secondInfo?.title,
                                        transcriptUrl = secondInfo?.transcriptUrl,
                                        sources = secondInfo?.sources
                                    ),
                                    null
                                )
                            )
                            persoPodTrackingData =
                                Pair(currentPersoData.second?.copy(podcastType = ONBOARDING), null)
                        }
                    }
                    Measurement.trackPodcastPlay(
                        mediaItemData.seriesSlug as String,
                        mediaItemData.podcastSlug.orEmpty(),
                        avName,
                        formattedDate,
                        audioTracking?.appSection,
                        audioTracking?.isFlexAudio ?: false,
                        audioTracking?.isAudioCarousel ?: false,
                        durationSeconds,
                        persoPodTrackingData?.first,
                        isPersoPod
                    )
                }
            }

            AudioProvider.EventType.ON_PERCENTAGE_PLAYED ->
                if (mediaItemData?.seriesSlug is String &&
                    (mediaItemData.podcastSlug is String || (!isPersoPod && avName != null)) &&
                    formattedDate != null &&
                    value is Byte
                ) {
                    Measurement.trackPodcastProgress(
                        mediaItemData.seriesSlug as String,
                        mediaItemData.podcastSlug.orEmpty(),
                        avName,
                        formattedDate,
                        value,
                        audioTracking?.appSection,
                        audioTracking?.isFlexAudio ?: false,
                        audioTracking?.isAudioCarousel ?: false,
                        durationSeconds,
                        persoPodTrackingData?.first,
                        isPersoPod
                    )
                }

            AudioProvider.EventType.ON_PROGRESS ->
                if (mediaItemData?.seriesSlug is String &&
                    (mediaItemData.podcastSlug is String || (!isPersoPod && avName != null)) &&
                    formattedDate != null &&
                    value is Int
                ) {
                    Measurement.trackPodcastProgressIncrement(
                        if (isPersoPod) "perso-${mediaItemData.audioType as String}" else mediaItemData.seriesSlug as String,
                        if (isPersoPod) mediaItemData.primaryLabel as String else mediaItemData.podcastSlug.orEmpty(),
                        avName,
                        formattedDate,
                        value,
                        audioTracking?.appSection,
                        audioTracking?.isFlexAudio ?: false,
                        audioTracking?.isAudioCarousel ?: false,
                        durationSeconds,
                        persoPodTrackingData?.first,
                        isPersoPod
                    )
                }

            AudioProvider.EventType.ON_SUBSCRIBE ->
                if (mediaItemData != null && value is String) {
                    Measurement.trackPodcastSubscribe(mediaItemData.subtitle as String, value)
                }

            AudioProvider.EventType.ON_ERROR ->
                if (value is Throwable) {
                    if (mediaItemData != null) {
                        CrashWrapper.logExtras(mediaItemData.toString())
                    }
                    CrashWrapper.sendException(value as Throwable?)
                    Logger.d(TAG, "Podcast error", value as Throwable?)
                }
        }
    }

    override fun getCurrentActivity(): Activity? = FlagshipApplication.getInstance().currentActivity

    override fun getAudioApiBaseUrl(): String = config.audioConfig.audioApiBaseUrl

    override fun saveUserPersoPodConfig(body: RequestBody) {
        CoroutineScope(Dispatchers.IO).launch {
            contentPacksRepo.setUserPersoPodConfig(body)
        }
    }

    override fun getAudioAdInterval(): Long = config.audioConfig.audioAdInterval

    override fun getThumbnailImageRequestURL(url: String?): String? =
        getImageResizerUrl(config.audioConfig.thumbnail, url)

    override fun getFullWidthImageRequestURL(url: String?): String? =
        getImageResizerUrl(config.audioConfig.fullWidth, url)

    override fun getDisabledAudioUrls(): List<String> = config.audioConfig.audioDisabledUrls

    override fun getImageLoader(): AnimatedImageLoader =
        FlagshipApplication.getInstance().animatedImageLoader

    override fun isRainbow(): Boolean = false

    override fun openSubscriptionLink(
        url: String,
        context: Context,
    ) {
        Utils.startWebChromeCustomTab(url, context)
    }

    override fun openArticles(
        context: Context?,
        appSection: String,
        url: String,
        sectionDisplayName: String,
    ) {
        val intent =
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(url)
                .setSectionDisplayName(sectionDisplayName)
                .setAppSection(appSection)
                .setOmniturePathToView(appSection)
                .buildIntent(context)
        context?.startActivity(intent)
    }

    private fun getImageResizerUrl(
        imageServiceConfig: ImageServiceConfig,
        url: String?,
    ): String? {
        try {
            if (url == null) return null
            val finalUrl: String =
                ConfigManager.getInstance().config.createImageRequestUrl(
                    url,
                    imageServiceConfig,
                )
            Logger.d("ImageService", "Original URL: $url, Final URL: $finalUrl")
            return finalUrl
        } catch (e: MalformedURLException) {
            Logger.e("ImageService", "Error imageURL $url", e)
        }
        return null
    }

    override fun getImageResizerUrlForAuto(
        url: String?
    ): String? {
        try {
            if (url == null) return null
            val finalUrl: String =
                ConfigManager.getInstance().config.createImageRequestUrlForAuto(
                    url,
                    ImageServiceConfig(512, 512),
                )
            return finalUrl
        } catch (e: MalformedURLException) {
            Logger.e("ImageService", "Error imageURL $url", e)
        }
        return null
    }

    override fun debugLog(
        context: Context,
        eventLogBuilder: EventLog.Builder,
    ) {
        RemoteLog.d(context, eventLogBuilder.build())
    }

    override fun onError(
        context: Context,
        eventLogBuilder: EventLog.Builder,
    ) {
        RemoteLog.e(context, eventLogBuilder.build())
    }

    override fun getPodcastItems(): rx.Observable<List<AudioMediaConfig>> {
        val contentManager = getInstance().contentManager
        return contentManager.listenToPage("podcasts", false)
            .map { it.fusionPage }
            .map { pageLayout ->
                if (pageLayout == null) {
                    EventLog.Builder().apply {
                        setMessage("Page layout is null for Podcasts")
                        setModule(LogModules.AUTO)
                    }.run {
                        RemoteLog.e(AppContextUtils.appContext, build())
                    }
                    return@map emptyList<AudioMediaConfig>()
                }
                val grid =
                    PageModelMapper.getGrid(pageLayout, pageConfig = PageConfig.build(config))
                podcastTracking = grid.tracking
                val audioItems = extractLatestPodcastItems(grid)
                audioItems.mapNotNull { item ->
                    generateAudioMediaConfig(item, "Podcasts")
                }
            }
    }

    override suspend fun getPersonalizedPodcasts(): AudioMediaConfig? = coroutineScope {
        val personalizedPodcast = getInstance().personalizedPodcastRepository.getPodcasts()
            ?.maxByOrNull { toDateLong(it.createdAt) }
        personalizedPodcast?.let { personalizedPodcast ->
            if (!isValidPodcast(personalizedPodcast)) return@coroutineScope null
            toAudioMediaConfig(personalizedPodcast)
        }
    }

    override suspend fun generatePersonalizedPodcast(): AudioMediaConfig? = coroutineScope {
        val body = getRequestBody()
        val podcast = getInstance().personalizedPodcastRepository.generatePodcast(body)
        podcast?.let {
            if (!isValidPodcast(it)) return@coroutineScope null
            toAudioMediaConfig(it)
        }
    }

    private fun getRequestBody(): RequestBody {
        val jsonObj = PersonalizedPodcastHelper.createJsonObject(mutableStateMapOf())
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonObj.first.toRequestBody(mediaType)
        return body
    }

    private fun toAudioMediaConfig(personalizedPodcast: PersonalizedPodcast): AudioMediaConfig =
        AudioMediaConfig(
            mediaId = personalizedPodcast.id,
            playerType = PlayerType.PERSO_PODCAST,
            streamUrl = personalizedPodcast.audioFilePath,
            title = personalizedPodcast.title,
            primaryLabel = personalizedPodcast.kicker,
            date = toDateLong(personalizedPodcast.createdAt),
            imageUrl = getImageResizerUrlForAuto(personalizedPodcast.image),
            audioTracking = AudioTrackerImpl(
                tabName = PODCASTS_SECTION_NAME,
                appSection = "carplay_$PODCASTS_SECTION_NAME",
                trackingInfo = TrackingInfo().apply {
                    pagePath = Measurement.AUDIO_TYPE_PODCAST
                    contentType = PODCASTS_SECTION_NAME
                },
                duration = personalizedPodcast.audioDuration?.toLong() ?: 0L,
                isAudioCarousel = true,
                isAuto = true,
                avName = personalizedPodcast.title
            ),
            duration = personalizedPodcast.audioDuration?.toLong(),
            audioType = personalizedPodcast.itemType
        )

    private fun isValidPodcast(personalizedPodcast: PersonalizedPodcast): Boolean {
        return (personalizedPodcast.id != null && personalizedPodcast.title != null && personalizedPodcast.image != null)
    }

    override fun trackCarPlayOpenEvent(
        section: String,
        trackingInfo: AudioTrackingInfo?,
        isTopRibbon: Boolean
    ) {
        val tracking = when (section) {
            FOR_YOU_SECTION_ID, FOR_YOU_SECTION_NAME -> {
                trackingInfo?.toTracking()
            }
            PODCAST_SECTION_ID, PODCAST_SECTION_NAME -> podcastTracking
            else -> trackingInfo?.toTracking()
        }
        if (tracking == null) {
            Logger.w(TAG, "Skipping Android Auto open event; tracking is unavailable for section=$section")
            return
        }

        Measurement.enableAutoOrigination()
        if (isTopRibbon) {
            Measurement.enableTopRibbon()
        }
        Measurement.enableIsAutoOpen()
        Measurement.trackAsTrackingInfo(
            tracking,
            section,
            "carplay_$section",
            0L,
            ""
        )
    }

    override fun trackPersoEvents(
        avName: String?,
        touchpoint: String?,
        miscellany: String?,
        avTags: String?,
        id: String?
    ) {
        if (!avTags.isNullOrEmpty()) {
            Measurement.trackAudioInteraction(avName, touchpoint, miscellany, avTags)
        } else {
            Measurement.trackAudioInteraction(avName, touchpoint, miscellany)
        }
    }

    override fun trackPersoShare(avName: String?, touchpoint: String?, miscellany: String?) {
        Measurement.trackPersoPodShare(avName, touchpoint, miscellany)
    }

    override fun trackPersoMenuOpen(avName: String?, touchpoint: String?, miscellany: String?) {
        Measurement.trackPersoMenuOpen(avName, touchpoint, miscellany)
    }

    override fun createPodcastTracker(
        duration: Long,
        persoPodTrackingInfo: Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>?
    ): AudioTracker {
        return AudioTrackerImpl(
            tabName = "Podcasts",
            duration = duration,
            persoPodTrackingInfo = persoPodTrackingInfo
        )
    }

    override fun getUserHeaders(): HashMap<String, String> {
        return hashMapOf(
            Pair("wapo-login-id", PaywallService.getInstance()?.loginId ?: ""),
        )
    }

    override fun shouldSuppressAds(): Boolean {
        return getInstance().shouldSuppressAds()
    }

    private fun AudioTrackingInfo.toTracking(): Tracking =
        Tracking(
            pageName = this.pageName ?: "For You",
            platform = "",
            site = "",
            pageType = this.interfaceType ?: "",
            section = "carplay_for_you",
            channel = this.channel ?: "",
            subsection = this.contentSubsection,
            hierarchy = this.hierarchy ?: "",
            contentType = this.contentType ?: "",
            storyType = "",
            headline = this.headline ?: "",
            author = this.contentAuthor ?: "",
            source = this.source ?: "",
            contentID = this.contentId ?: "",
            pageNum = this.pageNumber ?: "",
            opRanking = "",
            columnName = "",
            blogName = this.blogName ?: "",
            published = "",
            newsOrCommercial = "",
            commercialNode = this.commercialNode ?: "",
            contentCategory = this.contentCategory ?: "",
            sectionFront = "",
            trackScrolling = "",
            contentTopics = this.contentTopics ?: "",
            pageTitle = this.title ?: "",
            pagePath = "",
        )

    private fun generateAudioMediaConfig(
        carouselAudioItem: CarouselAudioItem,
        sectionName: String?,
    ): AudioMediaConfig? {
        return if (carouselAudioItem.audioArticle != null) {
            val audioArticle = carouselAudioItem.audioArticle ?: return null
            AudioMediaConfig(
                humanAdsUrl = audioArticle.humanVoice?.adsUrl,
                humanRawUrl = audioArticle.humanVoice?.rawUrl,
                titlePrefix = audioArticle.titlePrefix,
                titleSeparator = audioArticle.titleSeparator,
                title = audioArticle.title,
                primaryLabel = audioArticle.label?.text,
                secondaryLabel = audioArticle.label?.secondaryText,
                date = toDateLong(audioArticle.displayDate),
                imageUrl = getImageResizerUrlForAuto(
                    carouselAudioItem.media?.url ?: audioArticle.playerMedia?.url
                ),
                imageCaption = audioArticle.playerMedia?.caption,
                contentUrl = audioArticle.contentUrl?.let { getUrlWithoutParameters(it) },
                sectionName = audioArticle.tracking?.section,
                caption = audioArticle.caption,
                duration = VoiceUtils.getPreferredVoiceDuration(audioArticle),
                voices =
                    audioArticle.voices
                        ?.mapNotNull {
                            if (it.voiceId != null && it.rawUrl != null && it.label != null) {
                                PlaybackVoice(
                                    it.voiceId!!,
                                    it.label!!,
                                    it.rawUrl!!,
                                    it.adsUrl,
                                    it.duration
                                )
                            } else {
                                null
                            }
                        }?.toMutableList(),
                arcId = audioArticle.tracking?.arcId,
                audioTracking = AudioTrackerImpl(
                    "Podcasts",
                    "carplay_$sectionName",
                    audioArticle.tracking?.let { getCurrentTrackingInfo(it) },
                    false,
                    1f,
                    true,
                    audioArticle.feed,
                    isAuto = true,
                    duration = VoiceUtils.getPreferredVoiceDuration(audioArticle) ?: 0L,
                ),
                adsCustomTargeting = getSectionsAdTargetingValues(null, null, audioArticle.contentUrl),
                labelStyle = null,
                audioType = PersonalizedPodcastHelper.PersonalizedPodcastItemType.AUDIO_PODCAST,
                adConfig = audioArticle.adConfig,
            )
        } else if (carouselAudioItem.audio != null) {
            val audio = carouselAudioItem.audio ?: return null
            AudioMediaConfig(
                mediaId = audio.mediaId,
                streamUrl = audio.streamUrl,
                streamUrlNoAds = audio.streamUrlNoAds,
                title = audio.title,
                primaryLabel = audio.displayLabel,
                date = toDateLong(audio.displayDate),
                imageUrl = getImageResizerUrlForAuto(
                    audio.playerMediaEntity?.url ?: audio.coverImage ?: carouselAudioItem.media?.url
                ),
                contentUrl = carouselAudioItem.link?.url,
                audioTracking = AudioTrackerImpl(
                    tabName = "Podcasts",
                    appSection = "carplay_$sectionName",
                    trackingInfo = TrackingInfo().apply {
                        pageName = audio.tracking?.audioName
                        pagePath = "audio_article_carplay"
                        contentType = "Podcasts"
                    },
                    duration = audio.duration ?: 0L,
                    feed = audio.tracking?.seriesSlug,
                    isAudioCarousel = true,
                    isAuto = true,
                    avName = audio.tracking?.audioName,
                ),
                duration = audio.duration,
                adsCustomTargeting = getSectionsAdTargetingValues(null, null, carouselAudioItem.link?.url),
                audioType = PersonalizedPodcastHelper.PersonalizedPodcastItemType.AUDIO_PODCAST,
                series = carouselAudioItem.audio?.series,
                adConfig = carouselAudioItem.audio?.adConfig,
                seriesSlug = audio.tracking?.seriesSlug,
                podcastSlug = audio.slug,
                subscriptionLinks = audio.subscriptionLinks?.let { links ->
                    AudioMediaSubscriptionLinks(
                        alexa = links.alexa,
                        applePodcasts = links.applePodcasts,
                        googlePlay = links.googlePlay,
                        iheartRadio = links.iheartRadio,
                        radioPublic = links.radioPublic,
                        rss = links.rss,
                        spotify = links.spotify,
                        stitcher = links.stitcher,
                        tuneIn = links.tuneIn,
                    )
                },
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

    private fun extractLatestPodcastItems(grid: Grid): List<CarouselAudioItem> {
        val regions = grid.regions
        val items = regions.flatMap { it.items }
        val tables = items.map { it.label }
        return grid.regions
            .flatMap { it.items }
            .flatMap { it.items }
            .flatMap { it.items }
            .filterIsInstance<CarouselAudio>()
            .flatMap { it.items }
    }

    override fun getUserPrivacyConsentForAds(context: Context): AdRequestContext.UserPrivacyConsent {
        return AdRequestContext.UserPrivacyConsent(
            gdpr = if (OneTrustHelper.isEURegion()) "1" else "0",
            gdprConsent = OneTrustHelper.getGDPRConsent(context),
            usPrivacy = getCCPAAdsPrivacyString(context),
            rdp = if (isCCPAOptedOut()) "1" else "0",
            gpp = OneTrustHelper.getGppString(context),
            gppSid = OneTrustHelper.getGppSid(context)
        )
    }

    override suspend fun sharePodcast(context: Context, podcastId: String) {
        val result =
            getInstance().oneLinkRepository.generateAudioOneLink(
                subtype = AudioSubtype.PERSONALIZED_PODCAST.value,
                id = podcastId
            )

        when (result) {
            is OneLinkGeneratedStatus.Success -> {
                val title = context.getString(R.string.share_personalized_podcast_text)

                Share.Builder()
                    .shareUrl(result.oneLink)
                    .headline(title)
                    .fromPush(false)
                    .isVerticalVideoShare(false)
                    .build()
                    .shareItem(context)
            }

            is OneLinkGeneratedStatus.Error -> {
                showToast(com.wapo.flagship.features.audio.R.string.podcast_error_message)
            }
        }
    }

    companion object {
        private val TAG: String = AudioProviderImpl::class.java.simpleName
        val PODCASTS_SECTION_NAME: String = "Podcasts"
    }
}
