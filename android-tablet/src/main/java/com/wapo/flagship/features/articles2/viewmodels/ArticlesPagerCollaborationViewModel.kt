package com.wapo.flagship.features.articles2.viewmodels

import android.content.Context
import androidx.collection.ArrayMap
import androidx.lifecycle.*
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles.ArticleLinkType
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.articles2.events.Article2Events
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.navigation_models.*
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.tracking.*
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.articles2.utils.toTrackingInfo
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.playlist.toAudioMediaConfig
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.flagship.features.gifting.tracking.GiftTrackingDetails
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.save.database.model.MetadataModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STATE_PAGE = "STATE_PAGE"
private const val USER_HISTORY_PREFS = "user_history_prefs"

/**
 * This is a collaboration view model that is responsible for dispatching events that trigger page change events.
 */
@HiltViewModel
class ArticlesPagerCollaborationViewModel
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
        private val state: SavedStateHandle,
    ) : ViewModel() {
        /**
         * Currently selected page. This is set using [selectPage] function by [Articles2Fragment].
         */
        private val _currentPage: LiveEvent<ArticlePage> =
            LiveEvent()
        val currentPage: LiveEvent<ArticlePage> = _currentPage

        private val _pageRemoved: LiveEvent<Int> = LiveEvent()
        val pageRemoved: LiveEvent<Int> = _pageRemoved

        private val _currentPageContentState: LiveEvent<ArticleContentState> = LiveEvent()
        val currentPageContentState: LiveEvent<ArticleContentState> = _currentPageContentState

        private val activeArticlesMap = mutableMapOf<String, ArticleContentState>()
        private val _screenState = MediatorLiveData<ScreenState>()
        val screenState: LiveData<ScreenState> = _screenState

        private val _articleEvent = MutableSharedFlow<Article2Events>(
            replay = 0,
            extraBufferCapacity = 1
        )
        val articleEvent = _articleEvent.asSharedFlow()

        /**
         * Represents the [currentPage]'s toolbar state
         */
        var toolbarIconsState: ArticleToolbarIconsState = ArticleToolbarIconsState.NonNative

        private val liveMap = ArrayMap<String, MutableLiveData<ActionsOnIndividualArticles>>()

        /**
         * [FirebaseAnalyticsTracker] is used to track article data. This property is initialized as soon as the activity is created with individual/list of articles.
         */
        lateinit var firebaseAnalyticsTracker: FirebaseAnalyticsTracker

        /**
         * This is used to propagate the click on any authors (e.g. from by line) all the way to the activity to handle it.
         */
        private val _authorClicked: LiveEvent<Author> =
            LiveEvent()
        val authorClicked: LiveEvent<Author> = _authorClicked

        /**
         * This is used to propagate the click on any questions underline inside articles.
         */
        private val _askThePostClicked: LiveEvent<List<Question>> =
            LiveEvent()
        val askThePostClicked: LiveEvent<List<Question>> = _askThePostClicked

        /**
         * This is used to propagate the click on links inside the articles.
         */
        private val _linkClicked: LiveEvent<String> =
            LiveEvent()
        val linkClicked: LiveEvent<String> = _linkClicked

        /**
         * This is used to propagate the click on luf outcome posts inside the articles.
         */
        private val _lufOutcomePostClicked: LiveEvent<String> =
            LiveEvent()
        val lufOutcomePostClicked: LiveEvent<String> = _lufOutcomePostClicked

        /**
         * This is used to propagate the click on images inside the articles.
         */
        private val _imageTapEvent: LiveEvent<Image> =
            LiveEvent()
        val imageTapEvent: LiveEvent<Image> = _imageTapEvent

        /**
         * Represents the current Article content's link type. Value is being set based on 200 and 415 responses in [ArticleContentFragment].
         */
        private val _contentLinkType: LiveEvent<ArticleLinkType> = LiveEvent()
        val contentLinkType: LiveData<ArticleLinkType> = _contentLinkType

        /**
         * This is used to propagate bookmark button tap events happened on specific articles.
         */
        private val _bookmarkTapEvent: LiveEvent<Article2> = LiveEvent()
        val bookmarkTapEvent: LiveData<Article2> = _bookmarkTapEvent

        private val _articleContentState: MediatorLiveData<ArticleContentState> = MediatorLiveData()
        val articleContentState: LiveData<ArticleContentState> = _articleContentState

        /**
         * This is used to propagate bookmark button tap events happened on specific articles.
         */
        private val _giftTrackingEvent: LiveEvent<Pair<String, GiftTrackingDetails>> = LiveEvent()
        val giftTrackingEvent: LiveData<Pair<String, GiftTrackingDetails>> = _giftTrackingEvent

        /**
         * A map that tracks [UserBehaviorTrackingModel] so that tracking events can be dispatched appropriately.
         */
        val trackingMap: ArrayMap<String, UserBehaviorTrackingModel> = ArrayMap()

        /**
         * This is used to dispatch save event for reading history..
         */
        private val _readingHistorySaveEvent: LiveEvent<MetadataModel> = LiveEvent()
        val readingHistorySaveEvent: LiveData<MetadataModel> = _readingHistorySaveEvent

        /**
         * A map that stores the meta info for saving article to the reading history list.
         */
        val metaDataModelMap: ArrayMap<String, MetadataModel> = ArrayMap()

        /**
         * This is used to propagate bookmark button tap events happened on specific articles.
         */
        private val _trackUserBehaviorEvent: LiveEvent<String> = LiveEvent()
        val trackUserBehaviorEvent: LiveData<String> = _trackUserBehaviorEvent

        /**
         * This is used to propagate invalidate menu options events.
         */
        private val _invalidateOptionsMenu: LiveEvent<Any> = LiveEvent()
        val invalidateOptionsMenu: LiveData<Any> = _invalidateOptionsMenu

        /**
         * A job that will run in the background to track time spent on individual article.
         */
        var job: Job? = null

        /**
         * This is used to dispatch the event for tracking user behavior which is eventually used to show the subscribe to alerts dialog.
         */
        fun dispatchTrackUserBehaviorEvent(sectionId: String) {
            _trackUserBehaviorEvent.value = sectionId
        }

        /**
         * This map stores all the required information for firebase analytics tracking. These items are accessed and used when page change events are triggered using
         * [_currentPage] above.
         */
        val firebaseAnalyticsTrackingMap = ArrayMap<String, FirebaseTrackingInfo>()

        /**
         * This is used to propagate analytics tracking events when an article is loaded and when page is changed.
         */
        private val _firebaseAnalyticsTrackingEvent: LiveEvent<FirebaseAnalyticsTrackingEvent> =
            LiveEvent()
        val firebaseAnalyticsTrackingEvent: LiveData<FirebaseAnalyticsTrackingEvent> =
            _firebaseAnalyticsTrackingEvent

        /**
         * Dispatches the tracking event with the tracking info for firebase analytics tracking.
         */
        fun dispatchFirebaseAnalyticsTrackingEvent(event: FirebaseAnalyticsTrackingEvent) {
            _firebaseAnalyticsTrackingEvent.value = event
        }

        private val _articleMetricsEvents: LiveEvent<ArticleMetricsEvent> = LiveEvent()
        val articleMetricsEvents: LiveData<ArticleMetricsEvent> = _articleMetricsEvents

        /**
         * Dispatched when page expand event is fired. True if should expand False otherwise.
         */
        private val _pageExpandEvent: LiveEvent<Boolean> = LiveEvent()
        val pageExpandEvent: LiveData<Boolean> = _pageExpandEvent

        var pausedVideo: Video? = null

        // This is purely used for analytics purpose as we send the gift article info in analytics event irrespective of the status of the token validation
        var isGiftArticle = false

        var isNewsprint = false

        /**
         * Currently selected page Audio items availability.
         */
        private val _currentPageAudioAvailability: LiveEvent<Boolean> = LiveEvent()
        val currentPageAudioAvailability: LiveData<Boolean> = _currentPageAudioAvailability

        private val _currentPageSummaryAvailability: LiveEvent<Pair<Boolean, Boolean>> = LiveEvent()
        val currentPageSummaryAvailability: LiveData<Pair<Boolean, Boolean>> = _currentPageSummaryAvailability

        /**
         * This is used to deliver an event when article is ready to show summary tooltip.
         * As per designs, tooltip should not overlap with headlines until user touches the screen once
         * article is loaded.
         */
        private val _summaryTooltipEvent: LiveEvent<Boolean> = LiveEvent()
        val summaryTooltipEvent: LiveData<Boolean> = _summaryTooltipEvent

        private val _audioClickEvent: LiveEvent<AudioMediaConfig> = LiveEvent()
        val audioClickEvent: LiveData<AudioMediaConfig> = _audioClickEvent

        private val _summaryClickEvent: LiveEvent<Article2> = LiveEvent()
        val summaryClickEvent: LiveData<Article2> = _summaryClickEvent

        private val _talkToThePostClickEvent: LiveEvent<Boolean> = LiveEvent()
        val talkToThePostClickEvent: LiveData<Boolean> = _talkToThePostClickEvent
    
        private val _pagerScrollToggleEvent: LiveEvent<Boolean> = LiveEvent()
        val pagerScrollToggleEvent: LiveData<Boolean> = _pagerScrollToggleEvent

        private val _currentPageCommentsAvailability: LiveEvent<Boolean> = LiveEvent()
        val currentPageCommentsAvailability: LiveData<Boolean> = _currentPageCommentsAvailability

        private val _scrollProgress = MutableStateFlow(0f)
        val scrollProgress: StateFlow<Float> = _scrollProgress.asStateFlow()

        fun updateScrollProgress(progress: Float) {
            _scrollProgress.value = progress
                .takeIf { it.isFinite() }
                ?.coerceIn(0f, 1f)
                ?: 0f
        }

        init {
            _currentPage.value = state.get<ArticlePage>(STATE_PAGE)
            AirshipAnalytics.pauseMessages(true, AirshipAnalytics.PauseReason.ARTICLE_READ_WAIT)
        }

        /**
         * Expands the page
         */
        fun expandPage() {
            _pageExpandEvent.value = true
        }

        /**
         * Collapses the page
         */
        fun collapsePage() {
            _pageExpandEvent.value = false
        }

        /**
         * This function is used to toggle the pager scroll.
         */
        fun togglePagerScroll(toggle: Boolean) {
            _pagerScrollToggleEvent.value = toggle
        }

        /**
         * This posts the value of the selected page on  live data. Whoever is observing this live data will get information about this page selectiion.
         * In this case, it will be [Articles2Activity] since this activity makes decision about showing UI content based on type of the article and various
         * other properties associated with that article. E.g. different toolbar options are determined by Articles2Activity when page within the view pager changes.
         */
        fun selectPage(articlePage: ArticlePage) {
            state.set(STATE_PAGE, articlePage)
            _currentPage.value?.let {
                // don't post value if the same page
                if (it.articleMeta.id == articlePage.articleMeta.id) {
                    return
                }
            }
            _currentPage.postValue(articlePage)
            sendScreenState()
        }

        /**
         * This function sets the current page's [ArticleToolbarIconsState]. Whenever page is changed, the caller set the icon state by providing
         * a boolean flag [bookmarked] which is one of the factor that determines the state of the toolbar. Other factor is [ArticleLinkType] which can be
         * inferred from the [_currentPage] that's selected.
         */
        fun setToolbarIconsState(
            bookmarked: Boolean,
            linkType: ArticleLinkType,
        ) {
            toolbarIconsState =
                when {
                    linkType == ArticleLinkType.WEB -> ArticleToolbarIconsState.NonNative
                    else -> ArticleToolbarIconsState.Native
                }
        }

        /**
         * Any toolbar click events are initialized using this function.
         */
        fun initToolbarAction(
            url: String,
            actionsOnIndividualArticles: ActionsOnIndividualArticles,
        ) {
            liveMap[url]?.postValue(actionsOnIndividualArticles)
        }

        /**
         * This fucntion sets the livedata that is observed on in ArticleContentFragment with the url of the article as the key
         */
        fun setLiveMapLiveData(
            url: String,
            mutableLiveData: MutableLiveData<ActionsOnIndividualArticles>,
        ) {
            liveMap[url] = mutableLiveData
        }

        /**
         * Submits the value to the live event [authorClicked] when author name is clicked from e.g. by-line.
         */
        fun authorClicked(author: Author) {
            _authorClicked.postValue(author)
        }

        /**
         * Submits the value to the live event [askThePostClicked] when Ask The Post underline is clicked from sanitized_html.
         */
        fun askThePostClicked(questions: List<Question>) {
            _askThePostClicked.postValue(questions)
        }

        /**
         * Submits the value to the live event [linkClicked] when link is clicked from within the article.
         */
        fun linkClicked(url: String) {
            _linkClicked.postValue(url)
        }

        /**
         * Submits the value to the live event [lufOutcomePostClicked] when luf outcome post is clicked from within the article.
         */
        fun lufOutcomePostClicked(url: String) {
            _lufOutcomePostClicked.postValue(url)
        }

        /**
         * Submits the value to the live event [imageTapEvent] when image is tapped on from within the article.
         */
        fun imageTapped(image: Image) {
            _imageTapEvent.postValue(image)
        }

        /**
         * Dispatches event to proceed with bookmark save or delete functionality.
         */
        fun dispatchBookmarkTapEvent(article: Article2) {
            _bookmarkTapEvent.value = article
        }

        /**
         * This is used to dispatch the event to invalidate the options menu.
         */
        fun dispatchCurrentPageAudioAvailability(available: Boolean) {
            _currentPageAudioAvailability.value = available
        }

        fun dispatchCurrentPageCommentsAvailability(available: Boolean) {
            _currentPageCommentsAvailability.value = available
        }

        /**
         * Safety check to see if [firebaseAnalyticsTracker] is initialized before making the function call.
         */
        fun startFirebaseAnalyticsTracking(
            firebaseAnalyticsTrackingEvent: FirebaseAnalyticsTrackingEvent,
            context: Context,
        ) {
            if (::firebaseAnalyticsTracker.isInitialized) {
                when (firebaseAnalyticsTrackingEvent) {
                    is FirebaseAnalyticsTrackingEvent.ArticleTracking ->
                        firebaseAnalyticsTracker.startTracking(
                            firebaseTrackingInfo = firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                            context = context,
                            jTid = firebaseAnalyticsTrackingEvent.jTid,
                            isGiftArticle,
                            isNewsprint
                        ) {
                            _articleEvent.tryEmit(it)
                        }
                    is FirebaseAnalyticsTrackingEvent.BackPressTracking ->
                        firebaseAnalyticsTracker.backPressTrack()
                    is FirebaseAnalyticsTrackingEvent.BookmarkTracking ->
                        firebaseAnalyticsTracker.bookmarkTrack(
                            firebaseTrackingInfo = firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                            isSaving = firebaseAnalyticsTrackingEvent.isSaving,
                        )
                    is FirebaseAnalyticsTrackingEvent.ViewCommentsTracking ->
                        firebaseAnalyticsTracker.trackCommentsClick(
                            firebaseTrackingInfo = firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                        )
                    is FirebaseAnalyticsTrackingEvent.ArticleScrollTracking ->
                        firebaseAnalyticsTracker.trackScrollEvents(
                            firebaseTrackingInfo = firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                            articleScrollEventType = firebaseAnalyticsTrackingEvent.articleScrollEventType,
                        )
                    is FirebaseAnalyticsTrackingEvent.AuthorFollowCardShownTracking ->
                        firebaseAnalyticsTracker.trackAuthorInfoDialogShownEvent(
                            firebaseTrackingInfo = firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                        )
                    is FirebaseAnalyticsTrackingEvent.GiftSenderTracking ->
                        firebaseAnalyticsTracker.giftClickTrack(
                            contentUrl = firebaseAnalyticsTrackingEvent.contentUrl,
                            firebaseTrackingInfo = firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                            details = firebaseAnalyticsTrackingEvent.details,
                        )
                    is FirebaseAnalyticsTrackingEvent.ArticleSummaryTracking ->
                        firebaseAnalyticsTracker.trackArticleSummaryEvent(
                            firebaseTrackingInfo = firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                            summaryScreenSeen = firebaseAnalyticsTrackingEvent.summaryScreenSeen,
                            feedbackScreenSeen = firebaseAnalyticsTrackingEvent.feedbackScreenSeen,
                            feedbackSubmit = firebaseAnalyticsTrackingEvent.feedbackSubmit,
                            navigationBehavior = firebaseAnalyticsTrackingEvent.navigationBehavior,
                            miscellanySuffix = firebaseAnalyticsTrackingEvent.miscellanySuffix,
                        )
                    is FirebaseAnalyticsTrackingEvent.OnPageTap -> {
                        firebaseAnalyticsTracker.trackOnPageTap(
                            firebaseAnalyticsTrackingEvent.firebaseTrackingInfo,
                            firebaseAnalyticsTrackingEvent.miscellany,
                            firebaseAnalyticsTrackingEvent.genEventDimension
                        )
                    }
                }
            }
        }

        fun isVisibleArticle(contentUrl: String): Boolean {
            val urlNoParams = getUrlWithoutParameters(contentUrl)
            var metaIdNoParams: String? = null
            currentPage.value?.articleMeta?.id?.apply {
                metaIdNoParams = getUrlWithoutParameters(this)
            }
            return urlNoParams == metaIdNoParams
        }

        fun setContentLinkType(articleLinkType: ArticleLinkType) {
            _contentLinkType.value = articleLinkType
        }

        fun setContentState(contentState: ArticleContentState) {
            _currentPageContentState.value = contentState
        }

        fun setPageRemoved(position: Int) {
            _pageRemoved.value = position
        }

        /**
         * Returns true if the pager only has one article.
         */
        fun isSinglePage(): Boolean {
            return activeArticlesMap.size == 1
        }

        /**
         * Returns helper data for firebase analytics tracking to components that might need it.
         */
        fun getFirebaseTrackingHelperData(): FirebaseTrackingHelperData? {
            if (::firebaseAnalyticsTracker.isInitialized) {
                return firebaseAnalyticsTracker.firebaseTrackingHelperData
            }
            return null
        }

        /**
         * Retrieves the tracking info for the current selected article.
         */
        fun getCurrentTrackingInfo(): FirebaseTrackingInfo? {
            val articleMeta = _currentPage.value?.articleMeta
        /*
            Do nothing if it's a webview article.
         */
            if (articleMeta?.articleLinkType == ArticleLinkType.WEB) {
                return null
            }
            val currentPageUrl = articleMeta?.id
            currentPageUrl?.let {
                getUrlWithoutParameters(it).let { key ->
                    firebaseAnalyticsTrackingMap[key]?.let { trackingInfo ->
                        return trackingInfo
                    }
                }
            }
            return null
        }

        /**
         * Retrieves tracker object for capturing polly audio events.
         */
        fun getAudioTracker(
            isActionAudio: Boolean,
            speed: Float,
            trackingId: String? = null,
            trackingName: String? = null,
        ): AudioTracker =
            AudioTrackerImpl(
                getFirebaseTrackingHelperData()?.currentTabName,
                getFirebaseTrackingHelperData()?.sectionDisplayName,
                getCurrentTrackingInfo()?.omnitureX?.toTrackingInfo(),
                isActionAudio,
                speed,
                false,
                avArcId = trackingId,
                avName = trackingName,
            )

        /**
         * Starts the timer to dispatch [readingHistorySaveEvent] event.
         * As the new timer is started, timer that was previously started is automatically stopped.
         * This ensures that articles with time spent less than the required time to add to reading history are NOT added.
         * [urlKey] - Key to find the related [MetadataModel] from the [metaDataModelMap].
         */
        fun startTimer(urlKey: String) {
            AirshipAnalytics.pauseMessages(true, AirshipAnalytics.PauseReason.ARTICLE_READ_WAIT)
            job?.cancel()
            job =
                viewModelScope.launch(dispatcherProvider.io) {
            /*
                Wait fot 5 sec before adding the article to the reading history.
             */
                    delay(5000L)
                    _readingHistorySaveEvent.postValue(metaDataModelMap[urlKey])
                    AirshipAnalytics.pauseMessages(false, AirshipAnalytics.PauseReason.ARTICLE_READ_WAIT)
                }
        }

        fun assembleConfigWithChildren(
            rootAudio: Audio,
            article: Article2,
            context: Context,
            isActionAudio: Boolean = false
        ): AudioMediaConfig {
            val tracker = getAudioTracker(
                isActionAudio,
                AudioPreferences.getAudioPlaybackSpeed(context),
                rootAudio.tracking?.id,
                rootAudio.tracking?.name,
            )

            val containerConfig = rootAudio.toAudioMediaConfig(
                article = article,
                audioTracker = tracker,
                children = null,
                transitions = null
            )

            val childConfigs = rootAudio.children.orEmpty().filter { !it.url.isNullOrBlank() }.map { childAudio ->
                childAudio.toAudioMediaConfig(
                    article = article,
                    audioTracker = tracker,
                    children = null,
                    transitions = null
                )
            }

            val transitionConfigs = rootAudio.transitions.orEmpty().filter { !it.url.isNullOrBlank() }.map { transitionAudio ->
                transitionAudio.toAudioMediaConfig(
                    article = article,
                    audioTracker = tracker,
                    children = null,
                    transitions = null
                )
            }

            return containerConfig.copy(
                children = childConfigs.toMutableList(),
                transitions = transitionConfigs.toMutableList(),
            )
        }

        override fun onCleared() {
            super.onCleared()
            liveMap.clear()
            trackingMap.clear()
            metaDataModelMap.clear()
            firebaseAnalyticsTrackingMap.clear()
            job?.cancel()
            AirshipAnalytics.pauseMessages(false, AirshipAnalytics.PauseReason.ARTICLE_READ_WAIT)
            activeArticlesMap.clear()
        }

        fun dispatchArticleMetricsEvent(articleMetricsEvent: ArticleMetricsEvent) {
            _articleMetricsEvents.value = articleMetricsEvent
        }

        fun dispatchAudioClickEvent(audioMediaConfig: AudioMediaConfig) {
            _audioClickEvent.value = audioMediaConfig
        }

        fun dispatchArticleTapEvent(articleContentState: ArticleContentState) {
            _articleContentState.value = articleContentState
        }

        fun dispatchCurrentPageSummaryAvailability(
            approvedAvailable: Boolean,
            showFallbackSummary: Boolean,
        ) {
            _currentPageSummaryAvailability.value = Pair(approvedAvailable, showFallbackSummary)
        }

        fun dispatchSummaryTooltipEvent(available: Boolean) {
            _summaryTooltipEvent.value = available
        }

        fun dispatchSummaryClickEvent(article: Article2) {
            _summaryClickEvent.value = article
        }

        fun dispatchTalkToThePostClickEvent() {
            _talkToThePostClickEvent.value = true
        }

        /**
         * Propagate an individual article state to be visible in [screenState]
         */
        fun addToScreenState(
            id: String,
            state: ArticleContentState,
        ) {
            activeArticlesMap[id] = state
            sendScreenState()
        }

        /**
         * Remove an individual article state from [screenState]
         */
        fun removeFromScreenState(meta: ArticleMeta?) {
            if (meta?.id != null) activeArticlesMap.remove(meta.id)
            sendScreenState()
        }

        private fun sendScreenState() {
            val articlePage = this.state.get<ArticlePage>(STATE_PAGE)
            if (articlePage != null) {
                val value =
                    ScreenState(
                        articlePage.articleMeta.id,
                        articlePage.position,
                        activeArticlesMap.toMap(),
                    )
                _screenState.value = value
            }
        }

        /**
         * Send "New updates" button click event to the current article
         */
        fun dispatchLufRefresh() {
            val currentPageId = _screenState.value?.currentPageId ?: return
            liveMap[currentPageId]?.postValue(ActionsOnIndividualArticles.ActionLUFRefresh)
        }

        /**
         * Represents the current state of the pager with articles
         * @param currentPageId - currently displayed article URL in the pager
         * @param currentPageIndex - currently displayed article index in the pager
         * @param activePages - map of active articles in the pager (current + offset pages) and their state
         */
        class ScreenState(
            val currentPageId: String,
            val currentPageIndex: Int,
            val activePages: Map<String, ArticleContentState>,
        ) {
            override fun toString(): String =
                StringBuilder()
                    .append("ScreenState:")
                    .appendLine()
                    .append("currentPageId ")
                    .append(currentPageId)
                    .appendLine()
                    .append("currentPageIndex ")
                    .append(currentPageIndex)
                    .appendLine()
                    .also {
                        activePages.forEach { entry ->
                            it.append(entry.key).append(" = ").append(entry.value)
                            it.appendLine()
                        }
                    }.toString()
        }
    }
