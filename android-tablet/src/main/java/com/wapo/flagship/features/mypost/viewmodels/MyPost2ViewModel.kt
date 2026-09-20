package com.wapo.flagship.features.mypost.viewmodels

import androidx.annotation.MainThread
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.ContentType
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.Utils
import com.wapo.flagship.di.app.modules.features.readinghistory.ReadingHistoryRepo
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.preferencesapi.ContentPacksListApiStatus
import com.wapo.flagship.features.preferencesapi.GetUserContentPacksApiStatus
import com.wapo.flagship.features.preferencesapi.models.ContentPackUiItem
import com.wapo.flagship.features.preferencesapi.models.Followable
import com.wapo.flagship.features.preferencesapi.repo.ContentPacksRepo
import com.wapo.flagship.features.purchasedarticles.model.MetadataPurchasedArticleModel
import com.wapo.flagship.features.purchasedarticles.repo.PurchasedArticlesRepository
import com.wapo.flagship.features.topicfollow.fragments.TopicFollowBottomSheetFragment
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.MyPostConfig
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.model.ArticleItem
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.MyPostCarouselViewItem
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.ReadingHistoryAndMetadata
import com.washingtonpost.android.save.database.model.SavedArticleModel
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.EmptyState
import com.washingtonpost.android.save.models.FollowSnapshot
import com.washingtonpost.android.save.models.MyPostArticleItem
import com.washingtonpost.android.save.models.MyPostTopicItem
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.models.PreviewItem.EmptyPreviewItem
import com.washingtonpost.android.save.models.PreviewItem.FooterPreviewItem
import com.washingtonpost.android.save.models.PreviewItem.SectionPreviewItem
import com.washingtonpost.android.save.models.mapFromArticleAndMetadata
import com.washingtonpost.android.save.models.mapFromAuthorItem
import com.washingtonpost.android.save.models.toMyPostCarouselViewItem
import com.washingtonpost.android.save.repo.MyPost2Repository
import com.washingtonpost.android.save.types.MyPostSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map

private const val FOLLOW_ARTICLES_LIMIT = 6

@HiltViewModel
class MyPost2ViewModel
@Inject
constructor(
    private val myPost2Repository: MyPost2Repository,
    private val contentPacksRepo: ContentPacksRepo,
    private val purchasedArticlesRepository: PurchasedArticlesRepository,
    private val audioManager: ClassicAudioManager2,
    private val readingHistoryRepo: ReadingHistoryRepo
) : ViewModel() {
    val config: MyPostConfig
        get() = ConfigManager.getInstance().config.myPostConfig

    /**
     * Constant to configure range of items (1..carouselItemsSize) to be shown in a carousel view
     * in preview cards.
     * Note: 0th position item will be the lead item. So carousel items starts from items positions 1.
     */
    val carouselItemsSize = 10

    /**
     * LiveData for currently selected MyPostSection
     */
    private val _section = MutableLiveData<MyPostSection>()
    var section: LiveData<MyPostSection> = _section

    /**
     * LiveEvent for handling article item click event in preview and detail views.
     */
    private val _articleItemClickEvent = LiveEvent<ArticleActionItem>()
    val articleItemClickEvent: LiveData<ArticleActionItem> = _articleItemClickEvent

    /**
     * LiveEvent for handling bookmark item click event in preview and detail views.
     * Also will be updated when "Remove from ..." is selected in the Utility menu.
     */
    private val _saveClickEvent = LiveEvent<ArticleActionItem>()
    val saveClickEvent: LiveData<ArticleActionItem> = _saveClickEvent

    /**
     * LiveEvent for handling Utility menu in preview, detail and author page views.
     */
    private val _optionsClickEvent = LiveEvent<ArticleActionItem>()
    val optionsClickEvent: LiveData<ArticleActionItem> = _optionsClickEvent

    /**
     * LiveEvent for handling ViewMore item click event in preview view.
     */
    private val _viewMoreClickEvent = LiveEvent<MyPostSection>()
    val viewMoreClickEvent: LiveData<MyPostSection> = _viewMoreClickEvent

    /**
     * LiveEvent for handling "More from ..." item click event in preview following view.
     */
    private val _moreFromAuthorClickEvent = LiveEvent<AuthorItem>()
    val moreFromAuthorClickEvent: LiveData<AuthorItem> = _moreFromAuthorClickEvent

    /**
     * LiveEvent for handling view archive click event in Saved Stories detail view.
     */
    private val _viewArchiveClickEvent = LiveEvent<Any>()
    val viewArchiveClickEvent: LiveData<Any> = _viewArchiveClickEvent

    /**
     * LiveEvent for handling Go to TopStories click event
     */
    private val _viewTopStoriesClickEvent = LiveEvent<Any>()
    val viewTopStoriesClickEvent: LiveData<Any> = _viewTopStoriesClickEvent

    /**
     * LiveEvent for handling Sign In click event on an empty item views.
     */
    private val _signInClickEvent = LiveEvent<Any>()
    val signInClickEvent: LiveData<Any> = _signInClickEvent

    /**
     * LiveEvent for handling settings click event in toolbar.
     */
    private val _settingsClickEvent = LiveEvent<Any>()
    val settingsClickEvent: LiveData<Any> = _settingsClickEvent

    /**
     * LiveEvent for handling update consent settings for reading history consent.
     */
    private val _updateConsentSettingsClickEvent = LiveEvent<Any>()
    val updateConsentSettingsClickEvent: LiveData<Any> = _updateConsentSettingsClickEvent

    /**
     * LiveEvent for handling birthday front page click event in toolbar.
     */
    private val _birthdayFrontPageClickEvent = LiveEvent<Any>()
    val birthdayFrontPageClickEvent: LiveData<Any> = _birthdayFrontPageClickEvent

    /**
     * LiveEvent for handling Crosswords section click event.
     * This can be used to open any site service section using its label url.
     */
    private val _openSectionClickEvent = LiveEvent<String>()
    val openSectionClickEvent: LiveData<String> = _openSectionClickEvent

    /**
     * LiveEvent to maintain state to refresh UI when users comes from sign in page.
     */
    private val _signInAttemptEvent = LiveEvent<Any>()
    val signInAttemptEvent: LiveData<Any> = _signInAttemptEvent

    private val _rhHasCache = MutableLiveData<Boolean>()
    val rhHasCache: LiveData<Boolean> = _rhHasCache

    private val _contentPacksData = MutableLiveData<List<MyPostTopicItem>>()
    val contentPacksData: LiveData<List<MyPostTopicItem>> = _contentPacksData

    private var afterSignInAttempt = false

    /**
     * Members to load LiveData<ArticleAndMetaData> from db.
     */
    private val currentPageUrl = MutableLiveData<String>()

    private val refreshReadingHistoryTrigger = MutableLiveData(Unit)

    private val defaultAllSectionsOrdering =
        mutableListOf(
            MyPostSection.READING_HISTORY,
            MyPostSection.SAVED_STORIES,
            MyPostSection.FOLLOWING,
            MyPostSection.TOPICS,
        )

    /**
     * Live data to store My Post Section Ordering
     */
    private val _allSections = MutableLiveData<MutableList<MyPostSection>>()
    val allSections: LiveData<MutableList<MyPostSection>> get() = _allSections

    private var myPostBannerMessage: BannerPaywallMessage? = null

    private val _purchasedArticlesFlow =
        MutableStateFlow<List<MetadataPurchasedArticleModel>>(emptyList())
    val purchasedArticles: LiveData<List<MyPostArticleItem>> =
        _purchasedArticlesFlow.flatMapLatest { list ->
            purchasedArticlesRepository.getPurchasedArticles()
        }.asLiveData()

    init {
        _allSections.value = defaultAllSectionsOrdering
    }

    private val contentPacksListObserver = Observer<ContentPacksListApiStatus> { status ->
        if (status is ContentPacksListApiStatus.Success) {
            previousTopics = null
            _contentPacksData.postValue(buildTopicsList())
        }
    }

    private val getroContentPackObserver = Observer<GetUserContentPacksApiStatus> {
        previousTopics = null
        _contentPacksData.postValue(buildTopicsList())
    }

    fun downloadPurchasedArticles() {
        viewModelScope.launch {
            purchasedArticlesRepository.downloadPurchasedArticles()
        }
    }

    fun updateSections() {
        val nonEmptyList = mutableListOf<MyPostSection>()
        val emptyList = mutableListOf<MyPostSection>()
        setPurchasedArticleCarouselItems()
        viewModelScope
            .launch {
                filterEmptySection(emptyList, nonEmptyList)
            }.invokeOnCompletion {
                _allSections.postValue((nonEmptyList + emptyList) as MutableList<MyPostSection>?)
            }
    }

    private fun setPurchasedArticleCarouselItems() {
        _purchasedCarouselItemsList.apply {
            val myPostArticleItems = purchasedArticles.value?.map { it.toMyPostCarouselViewItem() }
            clear()
            if (!myPostArticleItems.isNullOrEmpty()) {
                //Dont show first item in purchased article carousel list since its the preview hero
                addAll(myPostArticleItems.subList(1, myPostArticleItems.size))
            }
        }
    }

    private val _previewSections = MutableLiveData<List<PreviewItem>>()
    val previewSections: LiveData<List<PreviewItem>> = _previewSections


    private fun filterEmptySection(
        emptyList: MutableList<MyPostSection>,
        nonEmptyList: MutableList<MyPostSection>,
    ) {
        defaultAllSectionsOrdering.forEach {
            if (hasAnyEmptyState(it)) {
                emptyList.add(it)
            } else {
                nonEmptyList.add(it)
            }
        }
        if (shouldDisplayPurchasedArticles()) {
            nonEmptyList.add(MyPostSection.PURCHASE)
        }
    }

    val liveArticleByUrl =
        currentPageUrl.switchMap { url ->
            myPost2Repository.getLiveArticleByUrl(url)
        }

    private fun setCurrentPageUrl(
        @NonNull url: String,
    ) {
        currentPageUrl.value = url
    }

    /**
     * Members to maintain and remove an article item from the db.
     */
    private val _unsaveArticle: LiveEvent<Pair<String, MyPostSection>?> = LiveEvent()
    val liveUnsavedArticle =
        _unsaveArticle.switchMap { article ->
            article?.run {
                myPost2Repository.getLiveArticleByUrl(first)
            }
        }
    val unsaveArticle: LiveData<Pair<String, MyPostSection>?> = _unsaveArticle

    /**
     * Member to maintain targetingEnabled state for For You empty card [EmptyState.FOR_YOU_ALLOW_COOKIES]
     * This empty card should have lower priority than [EmptyState.FOR_YOU_SIGN_IN]
     * False by default.
     */
    private val _targetingEnabled = LiveEvent<Boolean>()
    val targetingEnabled: LiveData<Boolean> = _targetingEnabled

    /**
     * To maintain Saved stories carousel items.
     */
    private val _readingCarouselItemsList = mutableListOf<CarouselViewItem>()

    /**
     * To maintain Reading history carousel items.
     */
    private val _readingHistoryCarouselItemsList = mutableListOf<MyPostCarouselViewItem>()

    /**
     * To maintain Purchased articles carousel items.
     */
    private val _purchasedCarouselItemsList = mutableListOf<MyPostCarouselViewItem>()

    /**
     * LiveData to maintain Saved stories items for the preview and detail views.
     * will also update [_readingCarouselItemsList]
     */
    val readingArticleItemsList =
        myPost2Repository.getPagedArticles().switchMap {
            MutableLiveData(
                mapFromArticleAndMetadata(it).also { itemList ->
                    _readingCarouselItemsList.apply {
                        clear()
                        addAll(mapToCarouselItems(itemList))
                    }
                },
            )
        }

    /**
     * LiveData to maintain Following items for the preview view.
     */
    private val followingArticleItemsList: MutableLiveData<List<MyPostArticleItem>> =
        MutableLiveData()
    val followingAuthors = myPost2Repository.getFollowedAuthorsLiveData()

    private lateinit var followSnapShot: LiveData<FollowSnapshot>

    val topics
        get() = buildTopicsList()

    var previousTopics: List<MyPostTopicItem>? = null

    /**
     * Creates a list of [MyPostTopicItem]s to display in the Topics section preview
     * Displays followed topics first and then fills the list up to [maxTopicsToDisplay]
     * (or all remaining items if [maxTopicsToDisplay] is null) Items are ordered by [ContentPackUiItem.priority]
     */
    private fun buildTopicsList(): List<MyPostTopicItem> {
        val context = FlagshipApplication.getInstance().applicationContext
        val topicList = mutableListOf<MyPostTopicItem>()

        previousTopics?.let {
            it.forEach { myPostTopicItem ->
                val isFollowing = contentPacksRepo.isTopicFollowed(context, myPostTopicItem.topicId)
                if (isFollowing != myPostTopicItem.isFollowing) {
                    val topicWithUpdatedFollowState =
                        MyPostTopicItem(
                            myPostTopicItem.topicId,
                            myPostTopicItem.iconUrl,
                            myPostTopicItem.text,
                            myPostTopicItem.destinationUrl,
                            isFollowing,
                        )
                    topicList.add(topicWithUpdatedFollowState)
                } else {
                    topicList.add(myPostTopicItem)
                }
            }

            previousTopics = topicList
            return topicList
        }

        val uiItems = contentPacksRepo.getContentPacksListStatus.value
        val uiItemsSortedByPriority =
            if (uiItems is ContentPacksListApiStatus.Success) {
                uiItems.contentPacks.filterNotNull().sortedWith(
                    compareBy(nullsLast()) { it.myPostPriority },
                )
            } else {
                previousTopics = topicList
                return topicList
            }

        // Add followed topics
        uiItemsSortedByPriority.forEach {
            it.id?.let { id ->
                if (contentPacksRepo.isTopicFollowed(context, id)) {
                    getMyPostTopicItem(it)?.let { myPostTopicItem ->
                        topicList.add(myPostTopicItem)
                    }
                }
            }
        }

        // Add remaining topics
        uiItemsSortedByPriority.forEach {
            it.id?.let { id ->
                if (!contentPacksRepo.isTopicFollowed(context, id)) {
                    getMyPostTopicItem(it)?.let { myPostTopicItem ->
                        topicList.add(myPostTopicItem)
                    }
                }
            }
        }

        previousTopics = topicList
        return topicList
    }

    private fun getMyPostTopicItem(uiItem: ContentPackUiItem): MyPostTopicItem? =
        if (uiItem.id != null &&
            uiItem.transparentSmall != null &&
            uiItem.heading != null
        ) {
            val isFollowing =
                contentPacksRepo.isTopicFollowed(
                    FlagshipApplication.getInstance().applicationContext,
                    uiItem.id,
                )
            MyPostTopicItem(
                uiItem.id,
                uiItem.transparentSmall,
                uiItem.heading,
                uiItem.destinationUrl,
                isFollowing,
            )
        } else {
            null
        }

    fun getFollowSnapshot(): LiveData<FollowSnapshot> {
        if (!this::followSnapShot.isInitialized) {
            followSnapShot =
                MediatorLiveData<FollowSnapshot>().also { mediator ->
                    mediator.addSource(followingAuthors) { authorList ->

                        val authorsWithIcons =
                            authorList
                                ?.map {
                                    // replace author full image with thumbnail
                                    it.copy(image = myPost2Repository.getAuthorImageUrl(it.image))
                                }

                        mediator.value =
                            FollowSnapshot(authorsWithIcons, followingArticleItemsList.value)
                    }

                    mediator.addSource(followingArticleItemsList) {
                        mediator.value =
                            FollowSnapshot(followingAuthors.value, followingArticleItemsList.value)
                    }
                }
        }

        return followSnapShot
    }

    fun onAuthorDataRequested(authorId: String) {
        viewModelScope.launch {
            val authorItem = myPost2Repository.getArticlesByFollow(authorId)
            if (authorItem != null) {
                val items = mapFromAuthorItem(listOf(authorItem), FOLLOW_ARTICLES_LIMIT)
                val currentList = followingArticleItemsList.value.orEmpty().toMutableList()
                currentList.addAll(items)
                val distincted = currentList.distinct()
                followingArticleItemsList.postValue(distincted)
            }
        }
    }

    // TODO change from ReadingHistoryAndMetadata to HistoryData
    val readingHistoryLiveData: LiveData<List<ReadingHistoryAndMetadata>> =
        refreshReadingHistoryTrigger.switchMap {
            liveData(context = viewModelScope.coroutineContext) {
                emit(readingHistoryRepo.mergeReadingHistory())
            }
        }

    /**
     * LiveData to maintain Reading history items for the preview and detail views.
     * will also update [_readingHistoryCarouselItemsList]
     */
    val readingHistoryArticleItemsList: LiveData<List<MyPostArticleItem>> =
        readingHistoryLiveData.map { rh ->
            val items = mapFromReadingHistoryAndMetadata(rh)
            _readingHistoryCarouselItemsList.apply {
                clear()
                addAll(mapToCarouselItems(items))
            }
            items
        }


    fun refreshReadingHistory() {
        refreshReadingHistoryTrigger.value = Unit
    }

    fun refreshContentPacks() {
        viewModelScope.launch {
            contentPacksRepo.getContentPackItems(isOnboarding = false)
            contentPacksRepo.getUserContentPacks()
        }
    }

    private fun mapFromReadingHistoryAndMetadata(articleAndMetadataList: List<ReadingHistoryAndMetadata>?): List<MyPostArticleItem> {
        if (articleAndMetadataList.isNullOrEmpty()) return emptyList()
        return articleAndMetadataList.mapNotNull {
            it.let {
                if (it.contentType == ContentType.ARTICLE && it.contentUrl != null) {
                    MyPostArticleItem(
                        contentUrl = it.contentUrl.toString(),
                        headline = it.headline,
                        headlinePrefix = it.headlinePrefix,
                        blurb = it.blurb,
                        kicker = it.displayLabel,
                        transparency = it.displayTransparency,
                        imageUrl = it.imageURL,
                        byline = it.byline,
                        dateTime = it.lastUpdated ?: it.publishedTime,
                        displayDate = it.displayDate,
                        authorId = null,
                        trackingString = it.trackingString,
                        deepestScrollId = it.deepestScrollId,
                        listenDepthSec = it.listenDepthSec,
                        contentType = it.contentType,
                        percentConsumed = it.percentConsumed
                    )
                } else {
                    MyPostArticleItem(
                        mediaId = it.mediaId,
                        streamUrl = it.streamUrl,
                        headline = it.headline,
                        imageUrl = it.imageURL,
                        displayDate = it.displayDate,
                        listenDepthSec = it.listenDepthSec,
                        contentType = it.contentType,
                        percentConsumed = it.percentConsumed,
                        label = it.label
                    )
                }
            }
        }.toList()
    }

    @MainThread
    fun setSection(section: MyPostSection) {
        _section.value = section
    }

    fun setTargetingEnabled(targetingEnabled: Boolean) {
        _targetingEnabled.value = targetingEnabled
    }

    fun handleArticleItemClickEvent(actionItem: ArticleActionItem) {
        val list = getPreviewListBySection(actionItem.section)
        val articleListOnly = list?.filter { it.contentType == ContentType.ARTICLE }
        val historyData = ArticleActionItem(
            section = actionItem.section,
            url = actionItem.url,
            isPreviewHeroArticle = actionItem.isPreviewHeroArticle,
            articleList = articleListOnly,
            recipePageName = actionItem.recipePageName,
            shouldPlayAudioArticle = actionItem.shouldPlayAudioArticle
        )
        if (actionItem.itemClicked?.contentType == ContentType.PODCAST) {
            if (!actionItem.itemClicked?.mediaId.isNullOrEmpty()) {
                val positionToStart = convertToMilliseconds(actionItem.itemClicked?.listenDepthSec)
                val mediaConfig = AudioMediaConfig(
                    mediaId = actionItem.itemClicked?.mediaId,
                    title = actionItem.itemClicked?.headline,
                    streamUrl = actionItem.itemClicked?.streamUrl,
                    positionToStartInMs = positionToStart
                )
                audioManager.playMedia(mediaConfig)
            }
        } else {
            _articleItemClickEvent.value = historyData
        }
    }

    private fun convertToMilliseconds(listenDepthSec: Long?): Long? {
        listenDepthSec?.let {
            return it * 1000
        }
        return null
    }

    fun handleSaveClickEvent(actionItem: ArticleActionItem) {
        _saveClickEvent.value = actionItem
    }

    fun handleSaveConfirmClickEvent() {
        _saveClickEvent.value?.let {
            _unsaveArticle.value = Pair(it.url, it.section)
        }
    }

    fun clearSaveConfirmClickEvent() {
        _unsaveArticle.value = null
    }

    fun handleOptionsClickEvent(actionItem: ArticleActionItem) {
        setCurrentPageUrl(actionItem.url)
        _optionsClickEvent.value = actionItem
    }

    fun handleViewMoreClickEvent(section: MyPostSection) {
        _viewMoreClickEvent.value = section
    }

    fun handleMoreFromAuthorClickEvent(authorItem: AuthorItem) {
        _moreFromAuthorClickEvent.value = authorItem
    }

    fun handleViewArchiveClickEvent() {
        _viewArchiveClickEvent.value = Any()
    }

    fun handleSignInClickEvent() {
        afterSignInAttempt = true
        _signInClickEvent.value = Any()
    }

    fun handleSettingsClickEvent() {
        afterSignInAttempt = true
        _settingsClickEvent.value = Any()
    }

    fun handleUpdateConsentSettingsClickEvent() {
        afterSignInAttempt = true
        _updateConsentSettingsClickEvent.value = Any()
    }

    fun handleBirthdayFrontPageClickEvent() {
        afterSignInAttempt = true
        _birthdayFrontPageClickEvent.value = Any()
    }

    fun handleOpenSectionClickEvent(link: String) {
        _openSectionClickEvent.value = link
    }

    fun handleAfterSignInAttempt() {
        if (afterSignInAttempt) {
            Measurement.trackSignInComplete(Measurement.APP_SECTION_MY_POST)
            afterSignInAttempt = false
            _signInAttemptEvent.value = Any()
        }
    }

    fun handleFollowOptionsClickEvent(articleItem: ArticleItem?) {
        articleItem?.let {
            handleOptionsClickEvent(
                ArticleActionItem(MyPostSection.FOLLOWING, it.url, false, null),
            )
        }
    }

    fun handleFollowButtonClickEvent(topicId: String) {
        (FlagshipApplication.getInstance().currentActivity as FragmentActivity).supportFragmentManager?.let { fm ->
            val followable =
                contentPacksRepo.getFollowableForId(topicId) ?: run {
                    // Build a bare-bones followable to display if followable is null
                    val contentPackUiItem = contentPacksRepo.getContentPackForId(topicId)
                    contentPackUiItem?.heading?.let { heading ->
                        Followable(heading = heading, image = contentPackUiItem.transparentImage)
                    }
                }

            followable?.let {
                TopicFollowBottomSheetFragment(
                    topicId,
                    it,
                    Measurement.getMyPostPageName(section.value),
                ).apply { show(fm, "topic-follow") }
            }
        }
    }

    fun removeArticleFromList(
        savedArticleMeta: ArticleAndMetadata,
    ) {
        myPost2Repository.removeArticleFromList(savedArticleMeta)
    }

    fun saveArticle(
        articleModel: SavedArticleModel,
        metadataModel: MetadataModel,
    ) {
        myPost2Repository.saveArticle(articleModel, metadataModel)
    }

    fun isLoggedInUserAndSubscriber(): Boolean =
        PaywallService.initialized() &&
                PaywallService.getInstance().isPremiumUser &&
                PaywallService.getInstance().isWpUserLoggedIn

    fun isLoggedInUserOrSubscriber(): Boolean =
        PaywallService.initialized() &&
                (PaywallService.getInstance().isWpUserLoggedIn || PaywallService.getInstance().isPremiumUser)

    fun isLoggedInUser(): Boolean =
        PaywallService.initialized() && PaywallService.getInstance().isWpUserLoggedIn

    fun canSwipeToRefresh(): Boolean =
        when (section.value) {
            MyPostSection.ALL, MyPostSection.SAVED_STORIES, MyPostSection.READING_HISTORY ->
                isLoggedInUserAndSubscriber()

            else -> false
        }

    fun refresh() {
        myPost2Repository.synchronize()
        downloadPurchasedArticles()
        refreshReadingHistory()
        refreshContentPacks()
    }

    fun hasAnyEmptyState(section: MyPostSection): Boolean = getEmptyState(section) != null

    /**
     * Method to know if given section has any empty state. UI will be updated based on it.
     */
    fun getEmptyState(section: MyPostSection): EmptyState? {
        val paywallService = PaywallService.getInstance()
        return when (section) {
            MyPostSection.SAVED_STORIES -> {
                when {
                    !paywallService.isWpUserLoggedIn -> EmptyState.SAVED_STORIES_SIGN_IN
                    readingArticleItemsList.value.isNullOrEmpty() -> EmptyState.SAVED_STORIES_SIGNED_IN
                    else -> null
                }
            }

            MyPostSection.FOLLOWING -> {
                when {
                    getAuthorListBySection(section).isNullOrEmpty() ->
                        when {
                            !paywallService.isWpUserLoggedIn -> EmptyState.FOLLOWING_SIGN_IN
                            else -> EmptyState.FOLLOWING_SIGNED_IN
                        }

                    else -> null
                }
            }

            MyPostSection.READING_HISTORY -> {
                when {
                    !paywallService.isWpUserLoggedIn -> EmptyState.READING_HISTORY_SIGN_IN
                    !isConsentGiven() -> EmptyState.READING_HISTORY_CONSENT_MISSING
                    rhHasCache.value == true -> null
                    readingHistoryArticleItemsList.value.orEmpty()
                        .isEmpty() -> EmptyState.READING_HISTORY_SIGNED_IN

                    else -> null
                }
            }

            else -> null
        }
    }

    fun shouldDisplayPurchasedArticles(): Boolean {
        val paywallService = PaywallService.getInstance()
        return paywallService.isWpUserLoggedIn && purchasedArticles.value?.isNotEmpty() == true
    }

    fun isConsentGiven(): Boolean {
        return OneTrustHelper.isFunctionalityEnabled() &&
                OneTrustHelper.isPerformanceEnabled() &&
                OneTrustHelper.isTargetingEnabled()
    }

    fun getPreviewListBySection(section: MyPostSection): List<MyPostArticleItem>? =
        when (section) {
            MyPostSection.SAVED_STORIES -> readingArticleItemsList.value
            MyPostSection.FOLLOWING -> followingArticleItemsList.value
            MyPostSection.READING_HISTORY -> readingHistoryArticleItemsList.value
            MyPostSection.PURCHASE -> purchasedArticles.value
            else -> null
        }

    fun getCarouselListBySection(section: MyPostSection): List<CarouselViewItem>? =
        when (section) {
            MyPostSection.SAVED_STORIES -> _readingCarouselItemsList
            MyPostSection.READING_HISTORY -> _readingHistoryCarouselItemsList
            MyPostSection.PURCHASE -> _purchasedCarouselItemsList
            else -> null
        }

    fun getAuthorListBySection(section: MyPostSection): List<AuthorEntity>? =
        when (section) {
            MyPostSection.FOLLOWING -> followingAuthors.value
            else -> null
        }

    fun getTopicListBySection(section: MyPostSection): List<MyPostTopicItem>? =
        when (section) {
            MyPostSection.TOPICS -> topics
            else -> null
        }

    /**
     * Show if user has withdrawn Targeting consent or consent not given.
     */
    private fun shouldShowAllowCookies(): Boolean = _targetingEnabled.value == false

    private fun mapToCarouselItems(list: List<MyPostArticleItem>?): List<MyPostCarouselViewItem> {
        list?.let {
            if (it.size > 1) {
                return it
                    .subList(
                        1,
                        if (it.size >= carouselItemsSize) carouselItemsSize else it.size,
                    ).map { item ->
                        item.toMyPostCarouselViewItem()
                    }
            }
        }
        return emptyList()
    }

    fun shouldDisplayBanner(sectionName: String): Boolean {
        val message = myPostBannerMessage
        val isMessageValid = message?.let {
            !it.title.isNullOrBlank() && !it.body.isNullOrBlank() && !it.imageUrl.isNullOrBlank()
        } ?: false
        return if (isMessageValid && sectionName == MyPostSection.ALL.name) {
            true
        } else {
            config.banner?.let {
                it.enabled && sectionName == MyPostSection.ALL.name && userMeetsBannerCriteria(it.criteria)
            } ?: false
        }
    }

    /**
     * Only show banner if user meets ALL criteria.
     */
    private fun userMeetsBannerCriteria(criteria: List<String>?): Boolean {
        if (criteria.isNullOrEmpty()) {
            return true
        }
        return when {
            criteria.contains("logged-in") && !PaywallService.getInstance().isWpUserLoggedIn -> false
            criteria.contains("premium-access") && !PaywallService.getInstance().isPremiumUser -> false
            criteria.contains("anonymous") && !PaywallService.getInstance().isAnonymousUser -> false
            criteria.contains("grace-period") && !PaywallService.getInstance().isSubInGracePeriod -> false
            criteria.contains("active-sub") && PaywallService.getInstance().subStatus != PaywallConstants.ACTIVE -> false
            criteria.contains("terminated") && PaywallService.getInstance().subStatus != PaywallConstants.TERMINATED -> false
            criteria.contains("no-sub") && PaywallService.getInstance().subStatus != PaywallConstants.EMPTY -> false
            criteria.contains("suspended") && PaywallService.getInstance().subStatus != PaywallConstants.SUSPENDED -> false
            criteria.contains("free-trial") && PaywallService.getInstance().subStatus != PaywallConstants.FREE_TRIAL -> false
            criteria.contains("free-days") && !PaywallService.getInstance().isFreeDaysUser -> false
            criteria.contains("free-articles") && !PaywallService.getInstance().isFreeArticlesUser -> false
            criteria.contains("playstore") && Utils.isAmazonBuild() -> false
            criteria.contains("amazon") && !Utils.isAmazonBuild() -> false
            else -> true
        }
    }

    fun onPreviewSectionUpdate() {
        val items = mutableListOf<PreviewItem>()
        var hasEmptySections = false
        if (shouldDisplayBanner(MyPostSection.ALL.name)) {
            val bannerPreviewItem = getBannerItem()
            bannerPreviewItem?.let {
                items.add(
                    PreviewItem.BannerPreviewItem(
                        it.attributionInfo,
                        it.title,
                        it.url,
                        it.subtitle,
                        it.imageUrl,
                        it.darkImageUrl,
                        it.asset,
                        it.assetHasPriority,
                        it.minimizeOnNarrowScreen,
                        it.buttonText
                    )
                )
            }
        }
        _allSections.value?.forEach { myPostSection ->
            if (!hasAnyEmptyState(myPostSection)) {
                items.add(
                    SectionPreviewItem(
                        myPostSection,
                        getPreviewListBySection(myPostSection),
                        getCarouselListBySection(myPostSection),
                        getAuthorListBySection(myPostSection),
                        getTopicListBySection(myPostSection),
                    ),
                )
            } else {
                hasEmptySections = true
            }
        }
        // add birthday front page if enabled and user is signed in
        if (config?.birthdayFrontPageConfig?.enabled == true && isLoggedInUser()) {
            items.add(PreviewItem.BirthdayFrontPagePreviewItem())
        }
        // empty sections appear after birthday section
        if (hasEmptySections) {
            _allSections.value?.forEach { myPostSection ->
                if (hasAnyEmptyState(myPostSection)) {
                    getEmptyState(myPostSection)?.let {
                        items.add(EmptyPreviewItem(myPostSection, it))
                    }
                }
            }
        }
        items.add(FooterPreviewItem())
        _previewSections.value = items
    }

    private fun getBannerItem(): PreviewItem.BannerPreviewItem? {
        val message = myPostBannerMessage
        val primaryImageUrl = message?.iterableImage?.light?.takeUnless { it.isBlank() }
        val fallbackImageUrl = message?.imageUrl!!.takeUnless { it.isBlank() }
        val imageUrl = primaryImageUrl ?: fallbackImageUrl
        var banner = if (!imageUrl.isNullOrBlank() && !message.title.isNullOrBlank() && !message.body.isNullOrBlank()) {
            val darkImageUrl = message.iterableImage?.dark?.takeUnless { it.isBlank() }
            PreviewItem.BannerPreviewItem(
                attributionInfo = message.attributionInfo,
                title = message.title.orEmpty(),
                url = message.url,
                subtitle = message.body.orEmpty(),
                imageUrl = imageUrl,
                darkImageUrl = darkImageUrl
            )
        } else {
            null
        }
        if (config.banner?.enabled == true && banner == null && section.value == MyPostSection.ALL) {
            banner = config.banner?.let {
                PreviewItem.BannerPreviewItem(
                    title = it.title,
                    url = it.link,
                    subtitle = it.subtitle,
                    imageUrl = it.image.url,
                    darkImageUrl = it.image.urlDark,
                    asset = it.image.asset,
                    assetHasPriority = it.image.assetHasPriority,
                    minimizeOnNarrowScreen = it.cta.minimizeOnNarrowScreen,
                    buttonText = it.cta.buttonText
                )
            }
        }

        return banner
    }


    override fun onCleared() {
        super.onCleared()

        contentPacksRepo.getContentPacksListStatus.removeObserver(contentPacksListObserver)
        contentPacksRepo.getroContentPackStatus.removeObserver(getroContentPackObserver)
    }

    fun updateBannerMessage(banner: BannerPaywallMessage?) {
        myPostBannerMessage = banner
    }


    /**
     * Default to select "All" section in the Chip group.
     */
    init {
        setSection(MyPostSection.ALL)
        viewModelScope.launch(Dispatchers.IO) {
            _rhHasCache.postValue(readingHistoryRepo.getCount() > 0)
        }
        contentPacksRepo.getContentPacksListStatus.observeForever(contentPacksListObserver)
        contentPacksRepo.getroContentPackStatus.observeForever(getroContentPackObserver)
    }
}
