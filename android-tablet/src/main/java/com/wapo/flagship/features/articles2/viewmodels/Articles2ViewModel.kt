package com.wapo.flagship.features.articles2.viewmodels

import androidx.core.util.forEach
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.wapo.Utils
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdRequestTargets
import com.wapo.adsinf.models.AdSlotType
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication.Companion.getInstance
import com.wapo.flagship.features.articles2.interfaces.ArticlesSaveRepo
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.ElementGroupItem
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.ItemState
import com.wapo.flagship.features.articles2.models.ItemTruncateState
import com.wapo.flagship.features.articles2.models.ItemTruncateType
import com.wapo.flagship.features.articles2.models.LiveEntry
import com.wapo.flagship.features.articles2.models.LiveOutcome
import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.articles2.models.QuestionSet
import com.wapo.flagship.features.articles2.models.Renderer
import com.wapo.flagship.features.articles2.models.SubNav
import com.wapo.flagship.features.articles2.models.deserialized.*
import com.wapo.flagship.features.articles2.models.deserialized.ByLine.SubType
import com.wapo.flagship.features.articles2.models.deserialized.podcast.Podcast
import com.wapo.flagship.features.articles2.models.deserialized.video.Video
import com.wapo.flagship.common.getArticlesAdTargetingValues
import com.wapo.flagship.features.articles2.adinjector.ClassicAdInjector2
import com.wapo.flagship.features.articles2.interfaces.ArticleRecirculationRepository
import com.wapo.flagship.features.articles2.models.AdItem
import com.wapo.flagship.features.articles2.models.FtsCarousel
import com.wapo.flagship.features.articles2.models.constants.AdKind
import com.wapo.flagship.features.articles2.models.deserialized.gallery.Gallery
import com.wapo.flagship.features.articles2.models.recirculation.AutoRecircResponse
import com.wapo.flagship.features.articles2.models.toAd
import com.wapo.flagship.features.articles2.navigation_models.ShareContent
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.states.AudioAvailableVoicesApiState
import com.wapo.flagship.features.articles2.utils.ArticleItemAdapterHelper
import com.wapo.flagship.features.articles2.utils.BreakPoints
import com.wapo.flagship.features.articles2.utils.CustomUrlPrefixes
import com.wapo.flagship.features.articles2.utils.FeedsVersionHelper
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.Companion.HUMAN_AUDIO_SUBTYPE
import com.wapo.flagship.features.articles2.interfaces.DisclaimerInfoRepo
import com.wapo.flagship.features.articles2.models.InlineMessageItem
import com.wapo.flagship.features.articles2.models.recirculation.Label
import com.wapo.flagship.features.articles2.models.recirculation.LabelProps
import com.wapo.flagship.features.articles2.utils.InlineAlertToggleHelper
import com.wapo.flagship.features.articles2.utils.InlineOfferHelper
import com.wapo.flagship.features.articles2.viewholders.DEFAULT_AD_POSITION
import com.wapo.flagship.features.articles3.views.PdfUiStyle
import com.wapo.flagship.features.articles3.models.ui.*
import com.wapo.flagship.features.articles3.views.*
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState as PlayerAudioPlaybackState
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.wapo.flagship.features.comments.model.SourceAnnotation
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.video.PostTvWarmUp
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.model.Status
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.querypolicies.BypassCacheQueryPolicy
import com.wapo.flagship.querypolicies.LMTQueryPolicy
import com.wapo.flagship.querypolicies.Query
import com.wapo.flagship.querypolicies.QueryPolicy
import com.wapo.flagship.util.AudioUtil
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.first
import kotlin.collections.isNullOrEmpty
import kotlin.collections.map

const val TAG = "Articles2ViewModel"

@HiltViewModel
class Articles2ViewModel @Inject constructor(
    private val repository: Articles2Repository,
    private val dispatcherProvider: DispatcherProvider,
    private val audioManager: ClassicAudioManager2,
    private val recircRepository: ArticleRecirculationRepository,
    private val articlesSaveRepo: ArticlesSaveRepo,
    private val disclaimerInfoRepo: DisclaimerInfoRepo
) : ViewModel() {
    /**
     * This live data represents the overall state of the fragment that is attached to this vm
     */
    private val _articleContentState: MediatorLiveData<ArticleContentState> = MediatorLiveData()
    val articleContentState: LiveData<ArticleContentState> = _articleContentState
    private lateinit var articleRepositoryData: LiveData<Status<out Article2>>
    private val shareContent: MutableLiveData<ShareContent> = MutableLiveData()

    /**
     * This live data represents the polly playback state.
     */
    private val _audioPlaybackState: MutableLiveData<PlayerAudioPlaybackState> = MutableLiveData()
    val audioPlaybackState: LiveData<PlayerAudioPlaybackState> = _audioPlaybackState

    private val _showCollapseFloating: MutableLiveData<Boolean> = MutableLiveData()
    val showCollapseFloating: LiveData<Boolean> = _showCollapseFloating

    val nowPlayingAudioItem: LiveData<NowPlayingAudioItem?> =
        audioManager.nowPlayingAudioItem.map { it }

    private var articleBannerMessage: BannerPaywallMessage? = null

    var savedGalleryId = ""

    /**
     * Tracks which element-group IDs the user has expanded (e.g. "Show more" on live-update
     * link-boxes).  Survives live-poll recompositions because it lives in the ViewModel rather
     * than in Compose `remember` state.
     */
    private val _expandedElementGroups = mutableSetOf<String>()

    /**
     * Tracks which video IDs are currently playing inline (keyed by arc ID).
     * Compose VideoView observes this to swap between thumbnail overlay and live player frame.
     */
    private val _activeVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val activeVideoIds: StateFlow<Set<String>> = _activeVideoIds

    /**
     * Tracks autoplay videos that have finished playing (Ended) so they are not re-triggered
     * when the user scrolls them back into view.
     */
    private val _completedAutoplayIds = mutableSetOf<String>()

    fun setVideoActive(videoId: String) {
        _activeVideoIds.value = _activeVideoIds.value + videoId
    }

    fun setVideoInactive(videoId: String) {
        _activeVideoIds.value = _activeVideoIds.value - videoId
    }

    fun clearActiveVideos() {
        _activeVideoIds.value = emptySet()
    }

    fun markAutoplayCompleted(videoId: String) {
        _completedAutoplayIds.add(videoId)
    }

    fun isAutoplayCompleted(videoId: String): Boolean {
        return _completedAutoplayIds.contains(videoId)
    }

    fun updateArticleBanner(banner: BannerPaywallMessage?) {
        articleBannerMessage = banner
    }

    /**
     * Look up the original [Video] domain item by its arc ID from the current article state.
     */
    fun findVideoItemById(videoId: String): Video? {
        val state = _articleContentState.value as? ArticleContentState.Success ?: return null
        return state.article.items?.filterIsInstance<Video>()?.firstOrNull { it.id == videoId }
    }

    /**
     * This live data represents the state of the api call made to download available voices for this article.
     */
    private val _availableVoices: MediatorLiveData<AudioAvailableVoicesApiState> =
        MediatorLiveData()
    val availableVoices: LiveData<AudioAvailableVoicesApiState> = _availableVoices

    /**
     * A job that will run in the background to track time spent on individual article network request.
     */
    var uiTimeOutTimer: Job? = null

    /**
     * To keep track of meta.anchorId behavior in [ArticleContentNativeViewHolder]
     */
    var handledAnchorIdScrollOnArticleOpen = false

    /**
     * Grab all the questions sets from article response
     */
    val questionsList: LiveData<List<QuestionSet>> =
        articleContentState.map { state ->
            if (state is ArticleContentState.Success) {
                state.article.atpQuestions?.questionSet ?: emptyList()
            } else {
                emptyList()
            }
        }

    private val _questions = MutableLiveData<List<Question>>()
    val questions: LiveData<List<Question>> = _questions


    private val _userEvent = LiveEvent<UserEvent>()
    val userEvent: LiveEvent<UserEvent> = _userEvent

    private var pushTopic: String? = null
    fun setPushTopic(topic: String) {
        pushTopic = topic
    }

    private var jTid: Long? = null
    fun setJTid(newJTid: Long) {
        jTid = newJTid
    }

    /**
     * Prepare a video items list from article items. [PostTvWarmUp] will use these video items
     * list to load directly.
     */
    val videoItems: LiveData<List<Video>> =
        articleContentState.map { state ->
            if (state is ArticleContentState.Success) {
                state.article.items
                    ?.filterIsInstance<Video>()
                    ?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }

    private var forYouResponse: ForYouResponse? = null
    private var mostReadRecircItems: List<CarouselViewItem> = emptyList()
    private var autoRecircResponse: AutoRecircResponse? = null

    fun setForYouResponse(response: ForYouResponse?) {
        if (response?.recommendations == forYouResponse?.recommendations) return
        forYouResponse = response
        refreshRecircUiItems()
    }

    fun setMostReadRecircItems(items: List<CarouselViewItem>) {
        if (mostReadRecircItems == items) return
        mostReadRecircItems = items
        refreshRecircUiItems()
    }

    fun setAutoRecircResponse(response: AutoRecircResponse?) {
        if (response?.collections == autoRecircResponse?.collections) return
        autoRecircResponse = response
        refreshRecircUiItems()
    }

    private fun refreshRecircUiItems() {
        val current = _articleContentState.value as? ArticleContentState.Success ?: return
        val updatedUiItems = mapResponseDataToUiItems(current.article)
        if (updatedUiItems == current.uiItems) return
        _articleContentState.postValue(
            ArticleContentState.Success(
                article = current.article,
                uiItems = updatedUiItems,
                source = current.source,
                pendingArticle = current.pendingArticle,
                updatesCount = current.updatesCount,
                isUpdate = true,
            )
        )
    }

    fun setShareContent(
        url: String,
        content: String?,
        byLine: String?,
    ) {
        shareContent.value = ShareContent(url, content, byLine)
    }

    fun getShareContent(): ShareContent? = shareContent.value

    private fun checkLiveUpdate(
        article: Article2,
        url: String,
    ) {
        Logger.d(TAG, "checkLiveUpdate $url")
        val pollFrequency = article.pollFrequency ?: return
        val isLive = article.isLive ?: return

        if (pollFrequency > 0 && isLive) {
            // setup polling
            Logger.d(TAG, "polling new data in $pollFrequency $url")
            val delayMs = pollFrequency * 1000L
            viewModelScope.launch {
                delay(delayMs)

                val articleRepositoryData =
                    repository.fetchData(
                        Query(url, BypassCacheQueryPolicy()),
                        viewModelScope,
                        dispatcherProvider.io,
                    )

                _articleContentState.addSource(articleRepositoryData) {
                    when (it) {
                        is Status.Network -> {
                            Logger.d(TAG, "polling update received $url")
                            if (checkForDifferences(it.data.tableOfContents?.children) != null) {
                                val processData =
                                    processData(it.data, ArticleContentState.Source.NETWORK)
                                _articleContentState.value = processData
                                Measurement.trackLiveUpdateNotification(it.data)
                            }
                            // HTTP 200, poll again
                            checkLiveUpdate(it.data, url)
                        }

                        is Status.Cache -> {
                            Logger.d(TAG, "polling update received from cache $url")
                            // HTTP 304, poll again
                            checkLiveUpdate(it.data, url)
                        }
                        is Status.Error -> {}
                        is Status.Error415 -> {}
                    }
                }
            }
        }
    }

    private fun getCurrentArticle(): Article2? {
        val value = _articleContentState.value
        if (value is ArticleContentState.Success) {
            return value.article
        }
        return null
    }

    private fun checkForDifferences(tableOfContents: List<LiveEntry>?): Int? {
        val lastReceivedArticle = getCurrentArticle()
        return tableOfContents
            ?.withIndex()
            ?.firstOrNull { (index, post) ->
                post.arcId !=
                    lastReceivedArticle
                        ?.tableOfContents
                        ?.children
                        ?.getOrNull(index)
                        ?.arcId
            }?.index
    }

    fun refreshWithNewContext() {
        val value = _articleContentState.value

        if (value is ArticleContentState.Success && value.pendingArticle != null) {
            val processData = processData(value.pendingArticle, value.source, isUpdate = true)
            _articleContentState.postValue(processData)
            Measurement.trackLiveUpdateOnClick(value.article)
        }
    }

    fun setQuestions(questionsList: List<Question>) {
        _questions.value = questionsList
    }

    fun startLoadingArticle(articleMeta: ArticleMeta) {
        uiTimeOutTimer?.cancel()
        _articleContentState.value = ArticleContentState.Loading
        if (::articleRepositoryData.isInitialized) {
            _articleContentState.removeSource(articleRepositoryData)
        }
        val policy =
            if (articleMeta.bypassCache) {
                BypassCacheQueryPolicy()
            } else {
                LMTQueryPolicy(
                    articleMeta.lastModified,
                )
            }
        startUiTimeoutTimer()
        articleRepositoryData =
            repository.fetchData(
                Query(articleMeta.id, policy),
                viewModelScope,
                dispatcherProvider.io,
            )
        // Attempt to fetch article from proxy API if enabled and within rate limits
        fetchArticleFromProxyIfEnabled(articleMeta.id, policy)

        _articleContentState.addSource(articleRepositoryData) {
            when (it) {
                is Status.Network -> {
                    _articleContentState.postValue(
                        processData(it.data, ArticleContentState.Source.NETWORK),
                    )
                    checkLiveUpdate(it.data, articleMeta.id)
                }
                is Status.Cache -> {
                    _articleContentState.postValue(
                        processData(it.data, ArticleContentState.Source.CACHE),
                    )
                    checkLiveUpdate(it.data, articleMeta.id)
                }
                is Status.Error -> {
                    _articleContentState.postValue(ArticleContentState.Failure)
                }
                is Status.Error415 -> {
                    _articleContentState.postValue(
                        ArticleContentState.Unsupported(article415 = it.article415),
                    )
                }
            }
        }
    }

    /**
     * Fetches an article from the proxy API if the feature is enabled and rate limits allow.
     */
    private fun fetchArticleFromProxyIfEnabled(
        articleId: String,
        policy: QueryPolicy<Article2>,
    ) {
        val config = ConfigManager.getInstance().config.proxyApiConfig
        val isEnabled = config.enabled
        if (isEnabled) {
            val hitLimit = config.apiHitCount
            val context = getInstance().applicationContext
            if (PrefUtils.incrementProxyApiHitCount(context, hitLimit)) {
                viewModelScope.launch(dispatcherProvider.io) {
                    repository.fetchDecryptedArticleFromProxy(Query(articleId, policy))
                }
            }
        }
    }

    /**
     * Depending on the state it returns the instance of [Article2] if audio action should proceed.
     */
    fun shouldProceedToAudio(): Article2? =
        when (val contentState = articleContentState.value) {
            is ArticleContentState.Success -> contentState.article
            else -> null
        }

    /**
     * Depending on the state it returns the instance of [Article2] if bookmark action should proceed.
     */
    fun shouldProceedToBookmark(): Article2? =
        when (val contentState = articleContentState.value) {
            is ArticleContentState.Success -> contentState.article
            else -> null
        }

    fun shouldProceedToSummary(): Article2? =
        when (val contentState = articleContentState.value) {
            is ArticleContentState.Success -> contentState.article
            else -> null
        }

    /**
     * Sets the playback state for polly
     * to [pollyPlaybackState]
     */
    fun setAudioPlaybackState(state: PlayerAudioPlaybackState) {
        _audioPlaybackState.value = state
    }

    fun updateAudioPlaybackUi(nowPlayingAudioItem: NowPlayingAudioItem?) {
        val current = _articleContentState.value as? ArticleContentState.Success ?: return
        val activeAudioIds = nowPlayingAudioItem.getActiveAudioIds()
        val activePlaybackState = mapPlayerToUiPlaybackState(nowPlayingAudioItem?.audioPlaybackState)
        var hasChanges = false

        val updatedUiItems = current.uiItems.map { item ->
            when (item) {
                is AudioUiModel -> {
                    val newPlaybackState = resolveUiPlaybackState(item.rawUrl, activeAudioIds, activePlaybackState)
                    val newStatusText = when (newPlaybackState) {
                        AudioPlaybackState.PLAYING -> AudioUiModel.AudioStatusText.NOW_PLAYING.value
                        AudioPlaybackState.BUFFERING -> "Buffering..."
                        AudioPlaybackState.READY -> AudioUiModel.AudioStatusText.LISTEN.value
                    }
                    if (item.playbackState == newPlaybackState && item.statusText == newStatusText) {
                        item
                    } else {
                        hasChanges = true
                        item.copy().apply {
                            playbackState = newPlaybackState
                            statusText = newStatusText
                        }
                    }
                }

                is HumanAudioUiModel -> {
                    val newPlaybackState = resolveUiPlaybackState(item.url, activeAudioIds, activePlaybackState)
                    if (item.playbackState == newPlaybackState) {
                        item
                    } else {
                        hasChanges = true
                        item.copy().apply {
                            playbackState = newPlaybackState
                        }
                    }
                }

                is PodcastUiModel -> {
                    val newPlaybackState = resolveUiPlaybackState(item.rawUrl, activeAudioIds, activePlaybackState)
                    if (item.playbackState == newPlaybackState) {
                        item
                    } else {
                        hasChanges = true
                        item.copy().apply {
                            playbackState = newPlaybackState
                        }
                    }
                }

                else -> item
            }
        }

        if (!hasChanges) return

        _articleContentState.postValue(
            ArticleContentState.Success(
                article = current.article,
                uiItems = updatedUiItems,
                source = current.source,
                pendingArticle = current.pendingArticle,
                updatesCount = current.updatesCount,
                isUpdate = true,
            )
        )
    }

    private fun processData(
        article: Article2,
        source: ArticleContentState.Source,
        isUpdate: Boolean = false,
    ): ArticleContentState =
        if (article.renderer == Renderer.WEB || article.type == "gallery") {
            ArticleContentState.Unsupported(article = article)
        } else {
            _articleContentState.value = ArticleContentState.Processing

            val preprocessedItems = preprocessItemsForCompose(article)
            val updatedArticle = article.copy(items = preprocessedItems)
            val updatedUiItems = mapResponseDataToUiItems(updatedArticle)

            val currentArticle = getCurrentArticle()

            // Populate share content for the article
            val bylineText = updatedArticle.items
                ?.filterIsInstance<ByLine>()
                ?.firstOrNull()
                ?.authors
                ?.joinToString(", ") { it.name ?: "" }
            setShareContent(
                url = updatedArticle.contenturl ?: "",
                content = updatedArticle.title,
                byLine = bylineText
            )

            if (currentArticle == null || isUpdate) {
                ArticleContentState.Success(
                    article = updatedArticle,
                    uiItems = updatedUiItems,
                    source = source,
                    isUpdate = isUpdate
                )
            } else {
                var updatesCount = 0
                val currentState = _articleContentState.value
                if (currentState is ArticleContentState.Success) {
                    updatesCount = currentState.updatesCount
                }
                ArticleContentState.Success(
                    article = currentArticle,
                    uiItems = updatedUiItems,
                    source = source,
                    pendingArticle = updatedArticle,
                    updatesCount = updatesCount + 1,
                )
            }
        }

    // Mirrors essential legacy updateItems behavior for Compose-backed rendering.
    private fun preprocessItemsForCompose(article: Article2): List<Item>? {
        val items = article.items?.toMutableList() ?: return null

        removeOutdatedItems(items, article)

        if (!getInstance().shouldSuppressAds()) {
            loadAds(items, article)
        }

        loadInlineAlertToggle(items, article)
        loadInlineOffer(items)
        loadCards(items)
        initializeExpandCollapseCardStates(items)
        initializeTruncateStates(items)
        loadRecirc(items, article)
        loadAutoRecirc(article)
        loadGallery(items)

        // Keep parity with legacy stream termination.
        items.add(Tagline())

        // Filter out audio items if audio is disabled for this article.
        items.removeAll { it is Audio && isAudioDisabled(article.contenturl) == true }

        updateStandaloneAudioChildren(items, article.audio)
        removeUnknownItems(items)
        updateAuthorInfo(items, article)

        return items
    }

    /**
     * Mirrors Article2ItemsViewModel.processExpandCardItems.
     * Sets up ItemState on each ExpandCollapseCard, removes grouped items into truncatedItems
     * so they are hidden until the user expands.
     */
    private fun updateStandaloneAudioChildren(items: MutableList<Item>, correctAudio: Audio?) {
        if (correctAudio == null) return
        val audioItemIndex = items.indexOfFirst { it is Audio }
        if (audioItemIndex == -1) return
        val audioInList = items[audioItemIndex] as Audio
        audioInList.children = correctAudio.children
        audioInList.transitions = correctAudio.transitions
    }

    private fun initializeExpandCollapseCardStates(items: MutableList<Item>) {
        var index = 0
        while (index < items.size) {
            val item = items[index]
            if (item is ExpandCollapseCard && item.state == null) {
                var currentIndex = index + 1
                val truncateItemsList = mutableListOf<Item>()
                while (currentIndex < items.size && items[currentIndex].group == item.group) {
                    truncateItemsList.add(items.removeAt(currentIndex))
                }
                item.state = ItemState(
                    Truncate(
                        truncatedLabel = item.truncatedLabel,
                        expandedLabel = item.expandedLabel,
                        truncateItemsList.size,
                    ),
                    ItemTruncateState.COLLAPSED,
                    ItemTruncateType.GROUP,
                    item,
                    truncateItemsList,
                )
                truncateItemsList.forEach { child ->
                    if (child.state != null) {
                        child.state?.parentItemState = item.state
                    } else {
                        child.state = item.state
                    }
                }
            }
            index++
        }
    }

    /**
     * Mirrors Article2ItemsViewModel.processTruncateItems.
     * Sets up ItemState on SanitizedHtml items that carry a truncate field, removing
     * the subsequent N items so they are hidden until the user taps "Show more".
     */
    private fun initializeTruncateStates(items: MutableList<Item>) {
        var index = 0
        while (index < items.size) {
            val item = items[index]
            if (item is SanitizedHtml && item.truncate != null && item.state == null) {
                val truncateItemsCount = item.truncate.itemsCount ?: 0
                var currentIndex = index + 1
                val truncateItemsList = mutableListOf<Item>()
                if (index + truncateItemsCount < items.size) {
                    repeat(truncateItemsCount) {
                        if (currentIndex < items.size && items[currentIndex] is GalleryExpandCollapse) {
                            truncateItemsList.add(items.removeAt(currentIndex))
                        }
                        if (currentIndex < items.size) {
                            truncateItemsList.add(items.removeAt(currentIndex))
                        }
                    }
                }
                item.state = ItemState(
                    Truncate(
                        truncatedLabel = item.truncate.truncatedLabel,
                        expandedLabel = item.truncate.expandedLabel,
                        truncateItemsList.size,
                    ),
                    ItemTruncateState.COLLAPSED,
                    ItemTruncateType.PARAGRAPH,
                    item,
                    truncateItemsList,
                )
                truncateItemsList.forEach { child ->
                    if (child.state != null) {
                        child.state?.parentItemState = item.state
                    } else {
                        child.state = item.state
                    }
                }
            }
            index++
        }
    }

    /**
     * Called after Article2ItemsViewModel.onItemClick mutates the items list.
     * Re-maps the updated items to UI models and posts a new Success state so Compose re-renders.
     */
    fun updateItemsAfterToggle(updatedItems: List<Item>) {
        val current = _articleContentState.value as? ArticleContentState.Success ?: return
        val updatedArticle = current.article.copy(items = updatedItems)
        val updatedUiItems = mapResponseDataToUiItems(updatedArticle)
        _articleContentState.postValue(
            ArticleContentState.Success(
                article = updatedArticle,
                uiItems = updatedUiItems,
                source = current.source,
                pendingArticle = current.pendingArticle,
                updatesCount = current.updatesCount,
                isUpdate = true,
            )
        )
    }

    private fun loadCards(items: MutableList<Item>) {
        val itemsWithCollapseCards = mutableListOf<Item>()
        var index = 0
        var lastExpandCard: ExpandCollapseCard? = null
        while (index < items.size) {
            if (lastExpandCard != null && items[index].group != lastExpandCard.group) {
                itemsWithCollapseCards.add(
                    ExpandCollapseCard(
                        lastExpandCard.type,
                        lastExpandCard.truncatedLabel,
                        lastExpandCard.expandedLabel,
                    ).apply {
                        group = lastExpandCard.group
                    },
                )
                lastExpandCard = null
            } else if (items[index] is ExpandCollapseCard) {
                lastExpandCard = items[index] as ExpandCollapseCard
            }
            itemsWithCollapseCards.add(items[index])
            index++
        }
        if (lastExpandCard != null) {
            itemsWithCollapseCards.add(
                ExpandCollapseCard(
                    lastExpandCard.type,
                    lastExpandCard.truncatedLabel,
                    lastExpandCard.expandedLabel,
                ).apply {
                    group = lastExpandCard.group
                },
            )
        }
        items.clear()
        items.addAll(itemsWithCollapseCards)
    }

    private fun loadGallery(items: MutableList<Item>) {
        val itemsWithExpandCollapseCards = mutableListOf<Item>()
        var index = 0
        while (index < items.size) {
            if (items[index] is Gallery) {
                val gallery = items[index] as Gallery
                val imageItems =
                    gallery.images?.map {
                        it.apply {
                            galleryId = gallery.id
                            group = gallery.group
                        }
                    }
                if (!imageItems.isNullOrEmpty()) {
                    itemsWithExpandCollapseCards.add(imageItems.first())
                    itemsWithExpandCollapseCards.add(
                        GalleryExpandCollapse(
                            type = "gallery_expand_card",
                            galleryId = gallery.id,
                        ).apply {
                            group = items[index].group
                        },
                    )
                    itemsWithExpandCollapseCards.addAll(imageItems.subList(1, imageItems.size))
                }
            } else {
                itemsWithExpandCollapseCards.add(items[index])
            }
            index++
        }
        items.clear()
        items.addAll(itemsWithExpandCollapseCards)
    }

    private fun loadInlineAlertToggle(
        items: MutableList<Item>,
        article: Article2,
    ) {
        val inlineAlertToggleHelper = InlineAlertToggleHelper()
        if (!FeedsVersionHelper(article).hasInlineAlertToggle) {
            val inlineAlertToggleItem =
                inlineAlertToggleHelper.getInlineAlertToggleItem(article, items)
            inlineAlertToggleItem?.let {
                items.add(inlineAlertToggleHelper.getToggleIndex(items, it, article), it)
            }
        } else {
            val index = items.indexOfFirst { it is Toggle && it.subtype == "alerts" }
            if (index != -1) {
                val toggle = items[index] as Toggle
                items.removeAt(index)
                if (inlineAlertToggleHelper.alertsDisabledForSegment(toggle.key)) {
                    inlineAlertToggleHelper.convertToggleToInlineAlertToggleItem(toggle)
                        ?.let { inlineAlertToggleItem ->
                            items.add(index, inlineAlertToggleItem)
                        }
                }
            }
        }
    }

    private fun loadInlineOffer(items: MutableList<Item>) {
        val inlineOfferHelper = InlineOfferHelper()
        items.filter { it is Toggle && it.subtype == "offer" }
            .toList()
            .forEach { toggleItem ->
                val index = items.indexOf(toggleItem)
                if (index != -1) {
                    val inlineMessageItem = inlineOfferHelper.getInlineOfferItem(articleBannerMessage)
                        ?: InlineMessageItem(
                            sectionInlineMessage = null,
                            articleInlineMessage = null,
                        )
                    items[index] = inlineMessageItem
                }
            }
    }

    private fun loadRecirc(
        items: MutableList<Item>,
        article: Article2,
    ) {
        if (article.showCarousel == true) {
            items.add(ForYouRecirculationItem(article, forYouResponse?.recommendations.orEmpty()))
            items.add(RecirculationItem(article, RecirculationType.MOST_READ))
        }
    }

    private fun loadAutoRecirc(article: Article2) {
        if (autoRecircResponse == null) {
            viewModelScope.launch {
                val response = recircRepository.getAutoRecirculation(article)
                if (response is APIResult.Success) {
                    setAutoRecircResponse(response.data)
                }
            }
        }
    }

    private fun removeOutdatedItems(
        items: MutableList<Item>,
        article: Article2,
    ) {
        val feedsVersionHelper = FeedsVersionHelper(article)
        val articleVersion = article.version ?: 0
        val omit: (Item) -> Boolean = { item ->
            val hideVersion = item.hideVersion
            val showVersion = item.showVersion

            val shouldHide = hideVersion != null && when (item) {
                is ElevatedByline -> hideVersion >= feedsVersionHelper.supportsElevatedByline
                is BlockQuote, is ElementGroupBlockQuote -> hideVersion >= feedsVersionHelper.supportsBlockQuote
                else -> false
            }

            shouldHide || (showVersion != null && showVersion > articleVersion)
        }
        items.removeAll { omit(it) }
    }

    private fun loadAds(
        items: MutableList<Item>,
        article: Article2,
    ) {
        if (!FeedsVersionHelper(article).hasInlineAds) {
            val adInjector = ClassicAdInjector2()
            val adPositions = adInjector.getAdPositions(article, article.items)
            adPositions.forEach { key, value ->
                items.add(key, AdItem(adKey = value).toAd())
            }
        } else {
            var floatAdsExist = false
            val shouldRemove: (Item) -> Boolean = {
                (it is Ad && it.kind == AdKind.FLOAT.value).let { isFloatAd ->
                    if (!floatAdsExist) floatAdsExist = isFloatAd
                    isFloatAd
                }
            }
            val itemsWithFinalAds = items.filterNot { shouldRemove(it) }.toMutableList()
            itemsWithFinalAds.forEach {
                if (it is Ad && floatAdsExist) {
                    it.breakpoints = null
                }
                if (it is Ad &&
                    (
                        it.kind.isNullOrEmpty() ||
                            it.size.isNullOrEmpty() ||
                            it.adPath.isNullOrEmpty() ||
                            it.breakpoints.isNullOrEmpty()
                        )
                ) {
                    RemoteLog.d(
                        getInstance().applicationContext,
                        EventLog.Builder()
                            .setMessage("Feed Ad item values are missing")
                            .setModule(LogModules.ARTICLES)
                            .set("version", article.version)
                            .set("breakpoints", it.breakpoints)
                            .set("kind", it.kind)
                            .set("size", it.size)
                            .set("ad_path", it.adPath)
                            .set("article_url", article.contenturl)
                            .build()
                    )
                }
            }
            if (itemsWithFinalAds.isNotEmpty()) {
                items.clear()
                items.addAll(itemsWithFinalAds)
            }
        }
    }

    private fun removeUnknownItems(items: MutableList<Item>) {
        val isKnown: (Item) -> Boolean = { item ->
            ArticleItemAdapterHelper.getItemViewType(item) > -1
        }
        items.removeAll { !isKnown(it) }
    }

    private fun updateAuthorInfo(
        items: MutableList<Item>,
        article: Article2,
    ) {
        if (article.isCardified()) {
            items.firstOrNull { it is AuthorInfo }?.let {
                (it as? AuthorInfo)?.showDivider = false
            }
        }
    }

    private fun mapResponseDataToUiItems(articleModel: Article2): List<ArticleItemUiModel> {
        val responseDataItems = articleModel.items
        val uiItems = mutableListOf<ArticleItemUiModel>()
        val currentLayout = BreakPoints.getLayoutSpec(AppContextUtils.getDeviceWidthInDp().toInt()).layout
        var pendingTruncateExpandCollapse: ExpandCollapseUiModel? = null
        var pendingTruncateLabelState: ItemState? = null
        responseDataItems?.forEach {
            if (isTruncatedControlInvisible(it)) return@forEach
            val uiItem: ArticleItemUiModel? = when (it) {
                is Ad -> {
                    // Filter out ads not intended for the current breakpoint (same logic as Article2ItemsViewModel.isItemInvisible)
                    if (it.breakpoints != null && !it.breakpoints!!.contains(currentLayout.value)) {
                        null
                    } else {
                        val adSlotType = getAdSlotType(it.size)
                        AdUiModel(
                            adConfig = AdConfig(
                                adUnitId = AdsUtil.getAdUnitId(
                                    context = getInstance().applicationContext,
                                    contentType = articleModel.contentType ?: "article",
                                    adKey = getAdKey(it.adPath).orEmpty(),
                                    adType = getAdPosition(it, currentLayout),
                                ),
                                adRequestTargets = getAdRequestTargets(
                                    item = it,
                                    articleModel = articleModel,
                                    pushTopic = pushTopic,
                                    jTid = jTid,
                                    currentLayout = currentLayout,
                                ),
                                networkExtras = AdsUtil.getDefaultNetworkExtras(),
                                adDimensions = mutableListOf<AdDimension>().apply {
                                    add(AdsUtil.findAdDimensionFromAdSlotType(adSlotType))
                                    add(AdDimension.Fluid)
                                    addAll(AdsUtil.findAdDimensionsFromDeviceWidth())
                                },
                                adSlotType = adSlotType,
                                section = "articles",
                            ),
                            breakpoints = it.breakpoints,
                            position = it.position,
                            uiStyle = AdUiStyle.DEFAULT,
                        )
                    }
                }
                is Audio -> {
                    val totalDuration = AudioUtil.calculateTotalDuration(it)
                    val durationText = totalDuration?.let { totalDuration ->
                        AudioViewUtils.getDurationText(totalDuration / 1000, getInstance().applicationContext)
                    }
                    if (it.subtype == HUMAN_AUDIO_SUBTYPE) {
                        HumanAudioUiModel(
                            url = it.rawUrl ?: return@forEach,
                            label = it.inlinePlayer?.label
                                ?: getInstance().applicationContext.getString(R.string.narrated_audio),
                            durationText = durationText,
                            authorImageUrl = it.inlinePlayer?.prefixImage?.imageUrl,
                            uiStyle = HumanAudioUiStyle.DEFAULT,
                        )
                    } else {
                        AudioUiModel(
                            rawUrl = it.rawUrl ?: return@forEach,
                            title = it.title?.content,
                            durationText = durationText ?: "Listen",
                            uiStyle = if (it.subtype == Audio.SubType.STANDALONE.value) {
                                AudioUiStyle.STANDALONE
                            } else {
                                AudioUiStyle.DEFAULT
                            }
                        )
                    }
                }
                is AuthorInfo -> {
                    AuthorInfoUiModel(
                        id = it.id ?: return@forEach,
                        name = it.name ?: return@forEach,
                        bio = it.bio,
                        expertise = null,
                        imageUrl = it.image,
                        showDivider = it.showDivider ?: true,
                        uiStyle = AuthorInfoUiStyle.DEFAULT
                    )
                }
                is BlockQuote -> {
                    BlockQuoteUiModel(
                        content = it.content.orEmpty(),
                        attribution = it.attribution.orEmpty(),
                        mime = mapMimeType(it.mime),
                        uiStyle = BlockQuoteUiStyle.DEFAULT
                    )
                }
                is ByLine -> {
                    val authors = mapAuthors(it.authors)
                    BylineUiModel(
                        text = it.content ?: return@forEach,
                        subtext = it.subtext,
                        authors = authors,
                        imageUrl = authors.firstOrNull()?.imageUrl,
                        bio = authors.firstOrNull()?.bio,
                        uiStyle = when {
                            it.subtype == SubType.LIVE_UPDATE.value -> BylineUiStyle.LIVE_UPDATE
                            it.subtype == SubType.LIVE_REPORTER_INSIGHT.value -> BylineUiStyle.LIVE_REPORTER_INSIGHT
                            else -> BylineUiStyle.DEFAULT
                        },

                    )
                }
                is ContextBox -> {
                    ContextBoxUiModel(
                        headline = it.headline ?: return@forEach,
                        pages = mapContentPages(it.contentPages),
                        uiStyle = ContextBoxUiStyle.DEFAULT
                    )
                }
                is Correction -> {
                    CorrectionUiModel(
                        correctionType = it.correctionType ?: return@forEach,
                        content = it.content ?: return@forEach,
                        uiStyle = CorrectionUiStyle.DEFAULT
                    )
                }
                is Date -> {
                    DateUiModel(
                        content = it.content ?: return@forEach,
                        recencyThreshold = it.recencyThreshold,
                        uiStyle = mapDateUiStyle(it)
                    )
                }
                is Deck -> {
                    DeckUiModel(
                        content = it.content ?: return@forEach,
                        uiStyle = DeckUiStyle.DEFAULT
                    )
                }
                is Divider -> {
                    DividerUiModel(uiStyle = DividerUiStyle.DEFAULT)
                }
                is ElementGroup -> {
                    when (it) {
                        is ElementGroupLinkBox -> {
                            ElementGroupUiModel.ElementGroupLinkBoxUiModel(
                                subtype = it.subtype,
                                kicker = it.additionalProperties?.kicker,
                                title = it.additionalProperties?.linkBoxTitle,
                                subheadline = it.additionalProperties?.linkBoxSubheadline,
                                displayDate = it.additionalProperties?.linkBoxDisplayDate?.toString(),
                                contentElements = mapContentElements(it.contentElements).filterIsInstance<SanitizedHtmlUiModel>(),
                                expandCollapseUiModel = it.group?.let { group ->
                                    getDefaultExpandCollapseUiModel(group)
                                },
                                uiStyle = ElementGroupUiStyle.LINK_BOX
                            )
                        }

                        is ElementGroupBlockQuote -> {
                            ElementGroupUiModel.ElementGroupBlockQuoteUiModel(
                                contentElements = mapContentElements(it.contentElements).filterIsInstance<SanitizedHtmlUiModel>(),
                                attribution = BlockQuoteAttributionUiModel(
                                    content = it.attribution,
                                    mime = mapMimeType(it.mime),
                                ),
                            )
                        }
                    }
                }
                is ElevatedByline -> {
                    ElevatedBylineUiModel(
                        kicker = it.kicker?.displayLabel ?: it.kicker?.content.orEmpty(),
                        byline = it.byLine?.content.orEmpty(),
                        authors = mapAuthors(it.byLine?.authors),
                        uiStyle = if (it.kicker?.style.equals("opinions", ignoreCase = true)) {
                            ElevatedBylineUiStyle.OPINIONS
                        } else {
                            ElevatedBylineUiStyle.DEFAULT
                        }
                    )
                }
                is ExpandCollapseCard -> {
                    ExpandCollapseUiModel(
                        isExpanded = it.state?.truncateState == ItemTruncateState.EXPANDED,
                        expandedLabel = it.expandedLabel ?: "Show less",
                        truncatedLabel = it.truncatedLabel ?: "Show more",
                        minItemsCount = 3,
                        group = it.group ?: return@forEach,
                        uiStyle = ExpandCollapseUiStyle.DEFAULT
                    )
                }
                is ForYouRecirculationItem -> {
                    val recommendations = forYouResponse?.recommendations ?: it.list
                    if (recommendations.isEmpty()) {
                        null
                    } else {
                        CarouselUiModel(
                            label = "For You",
                            items = recommendations.map { recItem ->
                                ForYouCarouselItemUiModel(
                                    credits = recItem.credits,
                                    headline = recItem.headline ?: recItem.headlines?.basic,
                                    imageUrl = recItem.imageUrl ?: recItem.promoItems?.basic?.url,
                                    sourceType = recItem.sourceType,
                                    label = recItem.label,
                                    secondaryLabel = getForYouCarouselSecondaryLabel(recItem),
                                    isOpinions = isOpinionsCarouselLabel(
                                        style = recItem.labelDisplay?.basic?.style ?: recItem.label?.basic?.style,
                                        label = recItem.labelDisplay?.basic?.text ?: recItem.label?.basic?.text,
                                    ),
                                    recReason = recItem.recReason,
                                    url = ARTICLE_BASE_URL + (recItem.canonicalUrl ?: recItem.url ?: recItem.normalizedUrl),
                                    authors = recItem.authors,
                                    articleId = recItem.articleId,
                                    contentType = recItem.contentType,
                                )
                            },
                            uiStyle = CarouselUiStyle.FOR_YOU,
                            requestId = forYouResponse?.requestId,
                            recipeId = forYouResponse?.recipeId,
                            testId = forYouResponse?.testId,
                        )
                    }
                }
                is AutoRecircCarousel -> {
                    val collection = autoRecircResponse?.collections
                        ?.firstOrNull { collection -> collection.collectionId == it.carouselId }
                    if (collection != null) {
                        val carouselUiStyle = when (it.subtype) {
                            AutoRecircCarousel.SUBTYPE_THE_7_LIVE ->
                                CarouselUiStyle.SEVEN_LIVE
                            else -> CarouselUiStyle.AUTO_RECIRC
                        }
                        CarouselUiModel(
                            requestId = autoRecircResponse?.requestId,
                            category = collection.category,
                            label = collection.title.orEmpty(),
                            items = collection.articles?.map { recircItem ->
                                RecircCarouselItemUiModel(
                                    headline = recircItem.headlines?.basic,
                                    imageUrl = recircItem.promoItems?.basic?.url,
                                    sourceType = recircItem.type,
                                    label = recircItem.label,
                                    secondaryLabel = getRecircCarouselSecondaryLabel(
                                        labelText = recircItem.label?.basic?.text,
                                        transparencyText = recircItem.label?.transparency?.text,
                                        fallbackByline = recircItem.credits?.by
                                            ?.mapNotNull { byItem -> byItem.name?.takeIf(String::isNotBlank) }
                                            ?.joinToString(", ")
                                    ),
                                    isOpinions = isOpinionsCarouselLabel(
                                        style = null,
                                        label = recircItem.label?.basic?.text,
                                    ),
                                    url = "https://www.washingtonpost.com${recircItem.canonicalUrl}",
                                    articleId = recircItem.arcId,
                                    timestamp = if (it.subtype == AutoRecircCarousel.SUBTYPE_THE_7_LIVE) {
                                        Utils.dateToString(recircItem.displayDate)
                                    } else null,
                                )
                            }.orEmpty(),
                            uiStyle = carouselUiStyle,
                        )
                    } else {
                        null
                    }
                }
                is GalleryExpandCollapse -> {
                    ExpandCollapseUiModel(
                        isExpanded = it.state?.truncateState == ItemTruncateState.EXPANDED,
                        expandedLabel = it.expandedLabel ?: "Show less",
                        truncatedLabel = it.truncatedLabel ?: "Show more",
                        minItemsCount = 3,
                        group = it.group ?: return@forEach,
                        uiStyle = ExpandCollapseUiStyle.DEFAULT
                    )
                }
                is Image -> {
                    ImageUiModel(
                        imageUrl = it.imageURL ?: return@forEach,
                        darkModeImageUrl = it.darkModeImageUrl ?: it.imageURL,
                        caption = it.fullCaption ?: it.blurb,
                        imageWidth = it.imageWidth,
                        imageHeight = it.imageHeight,
                        isLive = it.isLive,
                        refreshRateMs = if (it.isLive) 60000L else null,
                        widthFactor = parseWidthFactor(it.widthFactor),
                        uiStyle = ImageUiStyle.DEFAULT
                    )
                }
                is InlineCarousel -> {
                    CarouselUiModel(
                        label = it.headline.orEmpty(),
                        items = it.carouselItems
                            ?.mapNotNull { item ->
                                val url = item.contentUrl ?: return@mapNotNull null
                                InlineCarouselItemUiModel(
                                    headline = item.title?.content ?: item.title?.liveContent,
                                    imageUrl = item.image?.imageURL,
                                    sourceType = null,
                                    kicker = item.label,
                                    url = url
                                )
                            }
                            ?: emptyList(),
                        uiStyle = CarouselUiStyle.DEFAULT
                    )
                }
                is InterstitialLink -> {
                    InterstitialLinkUiModel(
                        content = it.content ?: return@forEach,
                        url = it.url ?: return@forEach,
                        uiStyle = InterstitialLinkUiStyle.DEFAULT
                    )
                }
                is Kicker -> {
                    // update kicker for LUFs
                    val resolvedLabel = if (it.liveText != null && it.coverageActive == true) {
                        it.liveText
                    } else {
                        it.displayLabel
                    }
                    val finalLabel = if (Kicker.SubType.getValue(resolvedLabel) == Kicker.SubType.LIVE) {
                        getInstance().applicationContext.getString(com.washingtonpost.android.articles.R.string.kicker_article_live_updates)
                    } else {
                        resolvedLabel ?: it.content ?: return@forEach
                    }
                    KickerUiModel(
                        displayLabel = finalLabel,
                        displayTransparency = it.displayTransparency,
                        isLive = it.coverageActive ?: false,
                        path = it.path,
                        alignment = it.alignment,
                        image = it.image,
                        uiStyle = mapKickerUiStyle(it)
                    )
                }
                is Link -> {
                    when (it) {
                        is Comments -> CommentsUiModel(uiStyle = CommentsUiStyle.DEFAULT)
                        is LinkButton -> {
                            LinkButtonUiModel(
                                label = it.label,
                                url = it.url,
                                showArrow = it.showArrow,
                                uiStyle = if (it.subtype == LinkButton.SubType.BUTTON_OUTCOME.value) {
                                    LinkButtonUiStyle.OUTCOME
                                } else {
                                    LinkButtonUiStyle.DEFAULT
                                }
                            )
                        }
                        else -> null
                    }
                }
                is ListItem -> {
                    ListUiModel(
                        items = it.content ?: return@forEach,
                        listType = if (it.subtype.equals("ordered", ignoreCase = true)) {
                            ListType.ORDERED
                        } else {
                            ListType.UNORDERED
                        },
                        arcId = it.arcId,
                        uiStyle = mapListUiStyle(it)
                    )
                }
                is LiveOutcome -> {
                    val headline = it.headline?.content
                    val subHeadline = it.subHeadline?.content
                    if (headline.isNullOrBlank() && subHeadline.isNullOrBlank()) {
                        null
                    } else {
                        LiveOutcomeUiModel(
                            headline = headline ?: return@forEach,
                            subHeadline = subHeadline,
                            imageUrl = it.image?.imageURL,
                            uiStyle = if (it.subHeadline?.subtype.equals("live-update", ignoreCase = true)) {
                                LiveOutcomeUiStyle.LIVE_UPDATE
                            } else {
                                LiveOutcomeUiStyle.DEFAULT
                            }
                        )
                    }
                }
                is OlympicsMedals -> {
                    return@forEach
                }
                is Pin -> {
                    PinUiModel(
                        content = it.content ?: return@forEach,
                        uiStyle = PinUiStyle.DEFAULT
                    )
                }
                is Podcast -> {
                    val uiStyle = if (it.subtype == Podcast.SubType.INLINE.value) {
                        PodcastUiStyle.INLINE
                    } else {
                        PodcastUiStyle.DEFAULT
                    }
                    PodcastUiModel(
                        rawUrl = it.podtracUrl ?: return@forEach,
                        seriesName = it.seriesName,
                        episodeName = it.episodeName,
                        seriesImageUrl = it.seriesImageUrl,
                        durationText = AudioViewUtils.getDurationText(it.duration / 1000, getInstance().applicationContext),
                        uiStyle = uiStyle
                    )
                }
                is PullQuote -> {
                    PullQuoteUiModel(
                        content = it.content.orEmpty(),
                        attribution = it.attribution.orEmpty(),
                        mime = mapMimeType(it.mime),
                        uiStyle = PullQuoteUiStyle.DEFAULT
                    )
                }
                is Quote -> {
                    QuoteUiModel(
                        content = it.content,
                        attribution = it.attribution,
                        mime = mapMimeType(it.mime),
                        uiStyle = QuoteUiStyle.DEFAULT
                    )
                }
                is RecirculationItem -> {
                    val filteredItems = mostReadRecircItems.filter { carouselItem ->
                        carouselItem.contentUrl != articleModel.contenturl
                    }
                    if (filteredItems.isEmpty()) {
                        null
                    } else {
                        CarouselUiModel(
                            label = it.recirculationType.sectionName,
                            items = filteredItems.map { carouselItem ->
                                val labelProps = LabelProps(
                                    text = carouselItem.storyType,
                                    url = null,
                                    display = true,
                                    additionalProperties = null,
                                )
                                RecircCarouselItemUiModel(
                                    headline = carouselItem.title,
                                    imageUrl = carouselItem.imageUrl,
                                    sourceType = carouselItem.storyType,
                                    label = Label(
                                        transparency = labelProps,
                                        basic = labelProps,
                                        storyLength = labelProps,
                                        userNeed = labelProps
                                    ),
                                    secondaryLabel = getRecircCarouselSecondaryLabel(
                                        labelText = carouselItem.storyType,
                                        transparencyText = null,
                                        fallbackByline = carouselItem.byline,
                                    ),
                                    isOpinions = isOpinionsCarouselLabel(
                                        style = carouselItem.style?.name,
                                        label = carouselItem.storyType,
                                    ),
                                    url = carouselItem.contentUrl,
                                )
                            },
                            uiStyle = CarouselUiStyle.MOST_READ
                        )
                    }
                }
                is SanitizedHtml -> {
                    val content = it.content
                    if (content.isNullOrBlank() && it.oembed.isNullOrBlank()) {
                        null
                    } else {
                        SanitizedHtmlUiModel(
                            content = content.orEmpty(),
                            oEmbed = it.oembed,
                            questionSets = articleModel.atpQuestions?.questionSet,
                            sourceAnnotations = it.sourceAnnotations,
                            arcId = it.arcId,
                            subheadLevel = it.subheadLevel,
                            uiStyle = mapSanitizedHtmlUiStyle(it, it.sourceAnnotations != null, articleModel.atpQuestions?.questionSet != null)
                        )
                    }
                }
                is TableItem -> {
                    val header = it.header.map { headerCell -> headerCell.content.orEmpty() }
                    val rows = it.row.map { rowCells -> rowCells.map { rowCell -> rowCell.content.orEmpty() } }
                    if (header.isEmpty() && rows.isEmpty()) {
                        null
                    } else {
                        TableUiModel(
                            header = header,
                            rows = rows,
                            uiStyle = TableUiStyle.DEFAULT
                        )
                    }
                }
                is Tagline -> {
                    TaglineUiModel(
                        uiStyle = TaglineUiStyle.DEFAULT
                    )
                }
                is Title -> {
                    TitleUiModel(
                        text = it.content ?: return@forEach,
                        uiStyle = mapTitleUiStyle(it)
                    )
                }
                is Toggle -> {
                    when (it.subtype) {
                        "offer" -> {
                            val offer = InlineOfferHelper().getInlineOfferItem(articleBannerMessage)
                            InlineMessageUiModel(
                                articleInlineMessage = offer?.articleInlineMessage,
                                uiStyle = InlineOfferUiStyle.DEFAULT,
                            )
                        }
                        "alerts" -> {
                            val key = it.key ?: return@forEach
                            InlineAlertToggleUiModel(
                                topicDisplayName = key,
                                topicKey = key,
                                isEnabled = AppContext.isTopicEnabled(key),
                                uiStyle = InlineAlertToggleUiStyle.DEFAULT
                            )
                        }
                        else -> {
                            // TODO handle topic follow here if needed
                            return@forEach
                        }
                    }

                }
                is InlineMessageItem -> {
                    InlineMessageUiModel(
                        articleInlineMessage = it.articleInlineMessage,
                        uiStyle = InlineOfferUiStyle.DEFAULT
                    )
                }
                is Video -> {
                    VideoUiModel(
                        contentUrl = it.contenturl ?: return@forEach,
                        id = it.id  ?: return@forEach,
                        thumbnailUrl = it.imageURL,
                        title = it.title,
                        durationMs = it.duration,
                        isAutoplay = it.autoplay ?: false,
                        isLive = it.isLive ?: false,
                        isLooping = it.isLooping ?: false,
                        hasPromo = !it.promo?.url.isNullOrEmpty(),
                        caption = it.fullcaption,
                        aspectRatio = getAspectRatio(it),
                        uiStyle = VideoUiStyle.DEFAULT
                    )
                }
                is WebEmbed -> {
                    WebEmbedUiModel(
                        url = it.url,
                        oembed = it.oembed,
                        subtype = it.subtype,
                        widthFactor = parseWidthFactor(it.widthFactor),
                        uiStyle = WebEmbedUiStyle.DEFAULT
                    )
                }
                is SubNav -> {
                    it.url?.takeIf { url -> url.isNotBlank() }?.let { url ->
                        SubNavUiModel(tabsUrl = url, uiStyle = SubNavUiStyle.DEFAULT)
                    }
                }
                is Pdf -> {
                    PdfUiModel(
                        url = it.url ?: "",
                        uiStyle = PdfUiStyle.DEFAULT
                    )
                }
                else -> {
                    null
                }
            }

            uiItem?.let {
                uiItems.add(it)
            }

            // For SanitizedHtml items that act as truncate labels, emit an ExpandCollapseUiModel
            // so the "Show more"/"Show less" button sits below the content and moves down as
            // items expand — same pattern as ExpandCollapseCard.
            if (it is SanitizedHtml && it.state != null && it.state?.truncatedLabelItem === it) {
                val isTruncated = it.state?.isTruncated() ?: true
                if (isTruncated) {
                    // Collapsed: no child items in list, emit button right after label
                    uiItems.add(
                        ExpandCollapseUiModel(
                            isExpanded = false,
                            expandedLabel = it.state?.truncate?.expandedLabel ?: "Show less",
                            truncatedLabel = it.state?.truncate?.truncatedLabel ?: "Show more",
                            minItemsCount = null,
                            group = "truncate-${it.content.hashCode()}",
                            uiStyle = ExpandCollapseUiStyle.DEFAULT
                        )
                    )
                } else {
                    // Expanded: defer the button until after the last truncated child item
                    pendingTruncateExpandCollapse = ExpandCollapseUiModel(
                        isExpanded = true,
                        expandedLabel = it.state?.truncate?.expandedLabel ?: "Show less",
                        truncatedLabel = it.state?.truncate?.truncatedLabel ?: "Show more",
                        minItemsCount = null,
                        group = "truncate-${it.content.hashCode()}",
                        uiStyle = ExpandCollapseUiStyle.DEFAULT
                    )
                    pendingTruncateLabelState = it.state
                }
            }

            // Check if this item is the last truncated child — emit the deferred button
            if (pendingTruncateExpandCollapse != null && pendingTruncateLabelState != null &&
                pendingTruncateLabelState!!.isExpandedLabelItem(it)) {
                uiItems.add(pendingTruncateExpandCollapse!!)
                pendingTruncateExpandCollapse = null
                pendingTruncateLabelState = null
            }
        }

        return uiItems
    }

    private fun isTruncatedControlInvisible(item: Item): Boolean {
        val itemState = item.state ?: return false
        return (
            item is ExpandCollapseCard &&
                item === itemState.truncatedLabelItem &&
                itemState.truncateState == ItemTruncateState.EXPANDED
            ) || (
            item is GalleryExpandCollapse &&
                itemState.truncateState == ItemTruncateState.EXPANDED
            )
    }

    private fun getAspectRatio(item: Video): Float {
        val imageWidth = item.imageWidth
        val imageHeight = item.imageHeight
        return if (imageHeight != null && imageWidth > 0 && imageHeight > 0) {
            imageWidth / imageHeight.toFloat()
        } else {
            // fallback if some of the sizes in unknown
            if (item.vertical == true) {
                9 / 16f
            } else {
                16 / 9f
            }
        }
    }

    fun updateSourceAnnotations(annotations: List<SourceAnnotation?>) {
        val current = _articleContentState.value as? ArticleContentState.Success ?: return
        val filtered = annotations.filterNotNull()

        val updatedRawItems = current.article.items?.map { item ->
            when {
                item is SanitizedHtml && item.content?.contains(
                    CustomUrlPrefixes.FROM_THE_SOURCE_PREFIX.value, ignoreCase = true
                ) == true -> item.also { it.sourceAnnotations = filtered }
                item is FtsCarousel -> item.also {
                    it.sourceComments = annotations.mapNotNull { a -> a?.sourceComments?.firstOrNull() }
                }
                else -> item
            }
        }

        val newArticle = current.article.copy(items = updatedRawItems)
        val newUiItems = mapResponseDataToUiItems(newArticle)

        _articleContentState.postValue(
            ArticleContentState.Success(
                article = newArticle,
                uiItems = newUiItems,
                source = current.source,
                pendingArticle = current.pendingArticle,
                updatesCount = current.updatesCount,
                isUpdate = true,
            )
        )
    }

//    fun updatePlaybackState(url: String, newState: AudioPlaybackState) {
//        val contentState = _articleContentState.value as? ArticleContentState.Success ?: return
//        val updatedUiItems = contentState.uiItems.map {
//            if (it is AudioUiModel && it.rawUrl == url) {
//                it.copy().apply { playbackState = newState }
//            }
//        }
//    }

    private fun resolveUiPlaybackState(
        uiItemId: String,
        activeAudioIds: Set<String>,
        activePlaybackState: AudioPlaybackState,
    ): AudioPlaybackState {
        if (uiItemId.isBlank()) return AudioPlaybackState.READY
        return if (activeAudioIds.contains(uiItemId)) activePlaybackState else AudioPlaybackState.READY
    }

    private fun mapPlayerToUiPlaybackState(state: PlayerAudioPlaybackState?): AudioPlaybackState {
        return when (state) {
            is PlayerAudioPlaybackState.Playing -> AudioPlaybackState.PLAYING
            PlayerAudioPlaybackState.Buffering,
            PlayerAudioPlaybackState.Connecting,
            PlayerAudioPlaybackState.JSONSourceInitializing,
            PlayerAudioPlaybackState.JSONSourceInitialized,
            -> AudioPlaybackState.BUFFERING

            else -> AudioPlaybackState.READY
        }
    }

    private fun NowPlayingAudioItem?.getActiveAudioIds(): Set<String> {
        val nowPlaying = this ?: return emptySet()
        val mediaConfig = nowPlaying.audioMediaConfig
        return listOfNotNull(
            mediaConfig?.id,
            mediaConfig?.mediaId,
            mediaConfig?.rawUrl,
            mediaConfig?.humanRawUrl,
            mediaConfig?.manifestUrl,
            mediaConfig?.url,
            nowPlaying.mediaItemData?.mediaId,
            nowPlaying.mediaItemData?.mediaUrl,
        ).filter { it.isNotBlank() }.toSet()
    }

    private fun getAdRequestTargets(
        item: Ad,
        articleModel: Article2,
        pushTopic: String? = "",
        jTid: Long?,
        currentLayout: BreakPoints.Layout,
    ): AdRequestTargets =
        AdRequestTargets.getDefault().apply {
            addPushTopic(pushTopic)
            addAnalyticsTags(
                jTid = jTid,
                logEventExtras = { it.setContentUrl(articleModel.contenturl) }
            )
            addSlotSizeParameters(getAdSlotType(item.size))
            addAllArticlesAdTargetingValues(
                getArticlesAdTargetingValues(
                    articleModel,
                    item.primarySectionId,
                    null,
                    articleModel.contenturl
                )
            )
            addArticleTags(articleModel.tags?.split(",").orEmpty())
            addPageId(articleModel.arcId)
            addAdPosition(getAdPosition(item, currentLayout))
            setContentUrl(
                getAdContentUrl(
                    articleModel.contenturl.orEmpty(),
                    articleModel.title
                )
            )
        }

    private fun getAdKey(adPath: String?): String? = adPath?.trim('/')

    private fun getAdSlotType(size: String?): AdSlotType {
        // item.size will be "tall" or "medium"
        return when (size) {
            AdSlotType.TALL.value -> AdSlotType.TALL
            else -> AdSlotType.SHORT
        }
    }

    private fun getAdPosition(item: Ad, layout: BreakPoints.Layout): String {
        return when (layout) {
            BreakPoints.Layout.SMALL -> item.position?.small ?: DEFAULT_AD_POSITION
            BreakPoints.Layout.LARGE -> item.position?.large ?: DEFAULT_AD_POSITION
        }
    }

    private fun getAdContentUrl(contentUrl: String, title: String?): String {
        if (contentUrl.isNotEmpty()) {
            return contentUrl
        } else {
            RemoteLog.w(
                getInstance().applicationContext,
                EventLog.Builder()
                    .setMessage("ContentUrl is missing in article ads")
                    .setModule(LogModules.ADS)
                    .set("title", title).build()
            )
        }

        val domain = "https://www.washingtonpost.com"
        return domain
    }

    private fun mapAuthors(authors: List<Author>?): List<AuthorInfoUiModel> {
        return authors?.mapNotNull { author ->
            AuthorInfoUiModel(
                id = author.id ?: return@mapNotNull null,
                name = author.name ?: return@mapNotNull null,
                bio = author.bio,
                expertise = author.expertise,
                imageUrl = author.image,
            )
        } ?: emptyList()
    }

    private fun getForYouCarouselSecondaryLabel(item: RecommendationsItem): String? {
        val labelText = item.labelDisplay?.basic?.text ?: item.label?.basic?.text
        return getRecircCarouselSecondaryLabel(
            labelText = labelText,
            transparencyText = item.labelDisplay?.transparency?.text ?: item.label?.transparency?.text,
            fallbackByline = item.credits?.by
                ?.mapNotNull { byItem ->
                    byItem.additionalProperties?.original?.byline
                        ?.takeIf(String::isNotBlank)
                        ?: byItem.name?.takeIf(String::isNotBlank)
                }
                ?.joinToString(", "),
        )
    }

    private fun getRecircCarouselSecondaryLabel(
        labelText: String?,
        transparencyText: String?,
        fallbackByline: String?,
    ): String? {
        val preferredText = transparencyText
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals(labelText, ignoreCase = true) }

        if (preferredText != null) {
            return preferredText
        }

        return fallbackByline
            ?.removePrefix("By ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals(labelText, ignoreCase = true) }
    }

    private fun isOpinionsCarouselLabel(style: String?, label: String?): Boolean {
        return style.equals("opinions", ignoreCase = true) ||
            label.equals("opinion", ignoreCase = true) ||
            label.equals("opinions", ignoreCase = true)
    }

    private fun mapContentPages(contentPages: List<ContentPagesItem>?): List<ContextBoxUiModel.ContextBoxPageUiModel> {
        return contentPages?.map { contentPage ->
            ContextBoxUiModel.ContextBoxPageUiModel(
                mapContentElements(contentPage.contentElements),
                contentPage.alignment ?: ContextBoxAlignment.UNKNOWN
            )
        } ?: emptyList()
    }

    private fun mapContentElements(elements: List<ElementGroupItem?>?): List<ArticleItemUiModel> {
        return elements
            ?.mapNotNull { item ->
                when (item) {
                    is SanitizedHtml -> {
                        SanitizedHtmlUiModel(
                            content = item.content ?: return@mapNotNull null,
                            oEmbed = item.oembed,
                            questionSets = null,
                            sourceAnnotations = null,
                            subheadLevel = item.subheadLevel,
                            mime = mapMimeType(item.mime),
                            uiStyle = mapSanitizedHtmlUiStyle(item, false, false)
                        )
                    }
                    is ListItem -> {
                        ListUiModel(
                            items = item.content ?: return@mapNotNull null,
                            listType = if (item.subtype.equals("ordered", ignoreCase = true)) {
                                ListType.ORDERED
                            } else {
                                ListType.UNORDERED
                            },
                            uiStyle = mapListUiStyle(item)
                        )
                    }
                    is Title -> {
                        TitleUiModel(
                            text = item.content ?: return@mapNotNull null,
                            uiStyle = mapTitleUiStyle(item)
                        )
                    }
                    is Image -> {
                        ImageUiModel(
                            imageUrl = item.imageURL ?: return@mapNotNull null,
                            darkModeImageUrl = item.darkModeImageUrl ?: item.imageURL,
                            caption = item.fullCaption ?: item.blurb,
                            imageWidth = item.imageWidth,
                            imageHeight = item.imageHeight,
                            isLive = item.isLive,
                            refreshRateMs = if (item.isLive) 60000L else null,
                            widthFactor = parseWidthFactor(item.widthFactor),
                            uiStyle = ImageUiStyle.DEFAULT
                        )
                    }
                    is LiveOutcome -> {
                        val headline = item.headline?.content
                        val subHeadline = item.subHeadline?.content
                        if (headline.isNullOrBlank() && subHeadline.isNullOrBlank()) {
                            null
                        } else {
                            LiveOutcomeUiModel(
                                headline = headline ?: return@mapNotNull null,
                                subHeadline = subHeadline,
                                imageUrl = item.image?.imageURL,
                                uiStyle = if (item.subHeadline?.subtype.equals("live-update", ignoreCase = true)) {
                                    LiveOutcomeUiStyle.LIVE_UPDATE
                                } else {
                                    LiveOutcomeUiStyle.DEFAULT
                                }
                            )
                        }
                    }
                    is SubNav -> {
                        // Only reachable for a sub_nav nested in an element group, where the
                        // strip itself cannot render; fall back to a plain link to its endpoint.
                        InterstitialLinkUiModel(
                            content = "Related",
                            url = item.url ?: return@mapNotNull null,
                            uiStyle = InterstitialLinkUiStyle.DEFAULT
                        )
                    }
                    else -> null
                }
            }
            ?: emptyList()
    }

    private fun mapSanitizedHtmlUiStyle(
        item: SanitizedHtml,
        hasSourceAnnotations: Boolean,
        hasQuestionSets: Boolean
    ): SanitizedHtmlUiStyle {
        val subtype = item.subtype.orEmpty().lowercase()
        val style = item.style.orEmpty().lowercase()
        return when {
            style == "briefs" && item.subheadLevel == 3 -> SanitizedHtmlUiStyle.BRIEFS_EXCLUSIVE_LABEL
            subtype == "subhead" && style == "briefs" -> SanitizedHtmlUiStyle.SUBHEAD_BRIEFS
            subtype == "subhead" -> SanitizedHtmlUiStyle.SUBHEAD
            subtype == "extra" -> SanitizedHtmlUiStyle.EXTRA
            subtype == "trailer" -> SanitizedHtmlUiStyle.TRAILER
            subtype == "letter" -> SanitizedHtmlUiStyle.LETTER
            subtype == "intro" -> SanitizedHtmlUiStyle.INTRO
            subtype == "metatext" -> SanitizedHtmlUiStyle.METATEXT
            subtype == SanitizedHtml.SubType.EXPANDED_BYLINE.value -> SanitizedHtmlUiStyle.EXPANDED_BYLINE
            subtype == "blockquote" -> SanitizedHtmlUiStyle.BLOCKQUOTE
            item.oembed?.isNotEmpty() == true -> SanitizedHtmlUiStyle.SOCIAL_EMBED
            style == "opinions" -> SanitizedHtmlUiStyle.OPINIONS
            style == "briefs" -> SanitizedHtmlUiStyle.PARAGRAPH_BRIEFS
            hasSourceAnnotations -> SanitizedHtmlUiStyle.FROM_THE_SOURCE
            hasQuestionSets -> SanitizedHtmlUiStyle.ASK_THE_POST
            else -> SanitizedHtmlUiStyle.DEFAULT
        }
    }

    private fun mapListUiStyle(item: ListItem): ListUiStyle {
        val subtype = item.subtype.orEmpty().lowercase()
        val style = item.style.orEmpty().lowercase()
        return when {
            subtype == "blockquote" -> ListUiStyle.BLOCKQUOTE
            style == "briefs" -> ListUiStyle.BRIEFS
            style == "opinions" -> ListUiStyle.OPINIONS
            else -> ListUiStyle.DEFAULT
        }
    }

    private fun mapTitleUiStyle(item: Title): TitleUiStyle {
        val subtype = item.subtype.orEmpty().lowercase()
        val style = item.style.orEmpty().lowercase()
        return when {
            subtype == Title.SubType.H1.value -> TitleUiStyle.H1
            subtype == Title.SubType.H2.value -> TitleUiStyle.H2
            subtype == Title.SubType.H3.value -> TitleUiStyle.H3
            subtype == Title.SubType.H4.value -> TitleUiStyle.H4
            subtype == Title.SubType.H5.value -> TitleUiStyle.H5
            subtype == Title.SubType.H6.value -> TitleUiStyle.H6
            subtype == Title.SubType.LIVE_REPORTER_INSIGHTS.value -> TitleUiStyle.LIVE_REPORTER_INSIGHTS
            style == Title.Style.STYLE.value -> TitleUiStyle.STYLE_SECTION
            else -> TitleUiStyle.DEFAULT
        }
    }

    private fun mapDateUiStyle(item: Date): DateUiStyle {
        return when (item.subtype) {
            Date.SubType.LIVE_UPDATE.value,
            Date.SubType.LIVE_REPORTER_INSIGHT.value -> DateUiStyle.LIVE_UPDATE
            else -> DateUiStyle.DEFAULT
        }
    }

    private fun mapKickerUiStyle(item: Kicker): KickerUiStyle {
        return when {
            Kicker.SubType.getValue(item.liveText) == Kicker.SubType.LIVE -> KickerUiStyle.PILL_LIVE
            Kicker.SubType.getValue(item.liveText) == Kicker.SubType.EXCLUSIVE -> KickerUiStyle.PILL_EXCLUSIVE
            item.image != null -> KickerUiStyle.IMAGE
            item.style.equals("briefs", ignoreCase = true) -> KickerUiStyle.BRIEFS
            item.style.equals(Style.SEVEN_LIVE.value, ignoreCase = true) -> KickerUiStyle.THE_SEVEN_LIVE
            item.style.equals("opinions", ignoreCase = true) -> KickerUiStyle.OPINIONS
            else -> KickerUiStyle.DEFAULT
        }
    }

    private fun mapMimeType(mimeType: String?): MimeType? {
        return MimeType.entries.firstOrNull { it.value == mimeType }
    }

    private fun getDefaultExpandCollapseUiModel(group: String): ExpandCollapseUiModel {
        return ExpandCollapseUiModel(
            isExpanded = _expandedElementGroups.contains(group),
            expandedLabel = "Show less",
            truncatedLabel = "Show more",
            minItemsCount = 3,
            group = group,
            uiStyle = ExpandCollapseUiStyle.DEFAULT
        )
    }

    /**
     * Toggles the expand/collapse state of an element group (e.g. a live-update link-box).
     * The new state is persisted in [_expandedElementGroups] so it survives live-poll rebuilds,
     * and the UI items are re-mapped to reflect the change.
     */
    fun toggleElementGroupExpansion(group: String) {
        if (!_expandedElementGroups.remove(group)) {
            _expandedElementGroups.add(group)
        }
        val current = _articleContentState.value as? ArticleContentState.Success ?: return
        val updatedUiItems = mapResponseDataToUiItems(current.article)
        _articleContentState.postValue(
            ArticleContentState.Success(
                article = current.article,
                uiItems = updatedUiItems,
                source = current.source,
                pendingArticle = current.pendingArticle,
                updatesCount = current.updatesCount,
                isUpdate = true,
            )
        )
    }

    private fun parseWidthFactor(widthFactor: String?): WidthFactor? {
        return WidthFactor.entries.firstOrNull { it.value == widthFactor }
    }

        private fun startUiTimeoutTimer() {
            uiTimeOutTimer =
                viewModelScope.launch(dispatcherProvider.io) {
            /*
                Wait for UI timeout before we can show the webview article to the user.
             */
                    delay(ConfigManager.getInstance().config.articleContentUpdateRulesConfig.timeout)
            /*
                We don't need to update the state if Network, Cache request is already successful before the UI is timed out.
             */
                    if (_articleContentState.value is ArticleContentState.Loading) {
                        _articleContentState.postValue(ArticleContentState.UITimedOut)
                    }
                }
        }

        fun shouldProceedToEllipsisMenu(): ArticleContentState? =
            when (val contentState = articleContentState.value) {
                is ArticleContentState.Success -> contentState
                else -> null
            }

        fun isAudioDisabled(url: String): Boolean? {
            return audioManager.isAudioDisabled(url)
        }

        fun updateShowFloatingButton(
            show: Boolean,
            id: String,
        ) {
            savedGalleryId = id
            _showCollapseFloating.postValue(show)
        }

        fun eventTrigger(event: UserEvent) {
            _userEvent.postValue(event)
        }

        fun removeArticleUrls(widgetId: String) {
            viewModelScope.launch {
                articlesSaveRepo.removeArticleUrls(widgetId)
            }
        }

        override fun onCleared() {
            super.onCleared()
            uiTimeOutTimer?.cancel()
        }

        fun prepareContentListForAdapter(article: Article2, originalList: List<Item>?): List<Item>? {
            val mutableList = originalList?.toMutableList() ?: return null

            val audioItemIndex = mutableList.indexOfFirst { it is Audio }
            val correctAudio = article.audio

            if (audioItemIndex != -1 && correctAudio != null) {
                val audioInList = mutableList[audioItemIndex] as Audio
                audioInList.children = correctAudio.children
                audioInList.transitions = correctAudio.transitions
            }

            return mutableList
        }

    suspend fun getDisclaimerInfo(endpoint: String) = disclaimerInfoRepo.getDisclaimerInfo(endpoint)

    companion object {
        const val CONTROL_VARIANT = "control"
        const val VARIANT_A = "a"
        const val ARTICLE_BASE_URL = "https://washingtonpost.com"
    }
}
