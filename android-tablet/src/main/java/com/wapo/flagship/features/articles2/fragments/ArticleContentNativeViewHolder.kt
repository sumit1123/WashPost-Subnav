//package com.wapo.flagship.features.articles2.fragments
//
//import android.content.Context
//import android.os.Bundle
//import android.util.SparseArray
//import android.view.View
//import android.view.animation.Animation
//import android.view.animation.AnimationUtils
//import androidx.core.util.forEach
//import androidx.core.view.size
//import androidx.fragment.app.FragmentActivity
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.Observer
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.wapo.adsinf.BannerAdView
//import com.wapo.android.commons.extensions.toUri
//import com.wapo.android.commons.logs.EventLog
//import com.wapo.android.commons.logs.LogModules
//import com.wapo.android.commons.util.UiUtils
//import com.wapo.android.commons.util.ViewUtil.findActivityOfType
//import com.wapo.android.remotelog.logger.RemoteLog
//import com.wapo.flagship.external.toDp
//import com.wapo.flagship.features.articles2.adinjector.ClassicAdInjector2
//import com.wapo.flagship.features.articles2.gallery.GalleryCollapseButtonTracker
//import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
//import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
//import com.wapo.flagship.features.articles2.interfaces.ExternalEventsCoordinator
//import com.wapo.flagship.features.articles2.interfaces.TextSelection
//import com.wapo.flagship.features.articles2.itemdecorations.CardifyDecorator
//import com.wapo.flagship.features.articles2.itemdecorations.MarginItemDecoration
//import com.wapo.flagship.features.articles2.luf.AnchorTracker
//import com.wapo.flagship.features.articles2.models.AdItem
//import com.wapo.flagship.features.articles2.models.Article2
//import com.wapo.flagship.features.articles2.models.Author
//import com.wapo.flagship.features.articles2.models.FtsCarousel
//import com.wapo.flagship.features.articles2.models.Item
//import com.wapo.flagship.features.articles2.models.Question
//import com.wapo.flagship.features.articles2.models.TableOfContentsEvent
//import com.wapo.flagship.features.articles2.models.constants.AdKind
//import com.wapo.flagship.features.articles2.models.deserialized.Ad
//import com.wapo.flagship.features.articles2.models.deserialized.AuthorInfo
//import com.wapo.flagship.features.articles2.models.deserialized.ByLine
//import com.wapo.flagship.features.articles2.models.deserialized.ExpandCollapseCard
//import com.wapo.flagship.features.articles2.models.deserialized.ForYouRecirculationItem
//import com.wapo.flagship.features.articles2.models.deserialized.GalleryExpandCollapse
//import com.wapo.flagship.features.articles2.models.deserialized.Image
//import com.wapo.flagship.features.articles2.models.deserialized.RecirculationItem
//import com.wapo.flagship.features.articles2.models.deserialized.RecirculationType
//import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
//import com.wapo.flagship.features.articles2.models.deserialized.Tagline
//import com.wapo.flagship.features.articles2.models.deserialized.Toggle
//import com.wapo.flagship.features.articles2.models.deserialized.gallery.Gallery
//import com.wapo.flagship.features.articles2.models.toAd
//import com.wapo.flagship.features.articles2.navigation_models.ActionsOnIndividualArticles
//import com.wapo.flagship.features.articles2.navigation_models.ArticlePage
//import com.wapo.flagship.features.articles2.navigation_models.ShareContent
//import com.wapo.flagship.features.articles2.states.ArticleContentState
//import com.wapo.flagship.features.articles2.tracking.FirebaseAnalyticsTrackingEvent
//import com.wapo.flagship.features.articles2.utils.ArticleItemAdapterHelper
//import com.wapo.flagship.features.articles2.utils.BreakPoints
//import com.wapo.flagship.features.articles2.utils.CustomUrlPrefixes
//import com.wapo.flagship.features.articles2.utils.FeedsVersionHelper
//import com.wapo.flagship.features.articles2.utils.InlineAlertToggleHelper
//import com.wapo.flagship.features.articles2.utils.InlineOfferHelper
//import com.wapo.flagship.features.articles2.utils.ScrollStopper
//import com.wapo.flagship.features.articles2.utils.TextSelectionHelper
//import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
//import com.wapo.flagship.features.articles2.utils.toTrackingInfo
//import com.wapo.flagship.features.articles2.viewholders.AutoRecirculationViewHolder
//import com.wapo.flagship.features.articles2.viewholders.ForYouRecirculationViewHolder
//import com.wapo.flagship.features.articles2.viewholders.ListViewHolder
//import com.wapo.flagship.features.articles2.viewholders.SanitizedHtmlViewHolder
//import com.wapo.flagship.features.articles2.viewmodels.Article2ItemsViewModel
//import com.wapo.flagship.features.articles2.viewmodels.ArticleTableOfContentsViewModel
//import com.wapo.flagship.features.articles2.viewmodels.ArticleWallHelperViewModel
//import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
//import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
//import com.wapo.flagship.features.articles2.viewmodels.InlineTopicFollowViewModel
//import com.wapo.flagship.features.articles2.viewmodels.PageViewTimeTrackerViewModel
//import com.wapo.flagship.features.articles2.viewmodels.recirculation.ArticleRecirculationEvent
//import com.wapo.flagship.features.articles2.viewmodels.recirculation.ArticleRecirculationViewModel
//import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
//import com.wapo.flagship.features.comments.CommentsViewModel
//import com.wapo.flagship.features.conversations.ui.CommentBottomSheetFragment
//import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
//import com.wapo.flagship.features.inlineoffer.model.SectionInlineOfferViewModel
//import com.wapo.flagship.features.lowdatamodelbanner.viewmodel.LowDataBannerViewModel
//import com.wapo.flagship.features.video.viewmodels.VideoActivityViewModel
//import com.wapo.flagship.model.ArticleMeta
//import com.wapo.flagship.util.WPUrlAnalyser
//import com.washingtonpost.android.R
//import com.washingtonpost.android.databinding.FragmentArticleContentBinding
//import com.washingtonpost.android.follow.viewmodel.FollowViewModel
//import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
//import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
//import com.washingtonpost.userhistory.models.RecommendationsHelperItem
//import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
//import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel.Companion.INCLUDED_CONTENT_TYPES
//
//class ArticleContentNativeViewHolder(
//    val context: Context,
//    val binding: FragmentArticleContentBinding,
//    private val articles2ViewModel: Articles2ViewModel,
//    private val articlesPagerCollaborationViewModel: ArticlesPagerCollaborationViewModel,
//    private val pageViewTimeTrackerViewModel: PageViewTimeTrackerViewModel,
//    private val followViewModel: FollowViewModel,
//    private val articleTableOfContentsViewModel: ArticleTableOfContentsViewModel,
//    private val viewLifecycleOwner: LifecycleOwner,
//    private val actionsLiveData: MutableLiveData<ActionsOnIndividualArticles>,
//    private val externalEventsCoordinator: ExternalEventsCoordinator,
//    private val forYouActivityViewModel: ForYouActivityViewModel,
//    private val recirculationViewModel: ArticleRecirculationViewModel,
//    private val videoActivityViewModel: VideoActivityViewModel,
//    private val inlineTopicFollowViewModel: InlineTopicFollowViewModel,
//    private val article2ItemsViewModel: Article2ItemsViewModel,
//    private val userHistoryViewModel: UserHistoryViewModel,
//    private val articleWallHelperViewModel: ArticleWallHelperViewModel,
//    private val lowDataBannerViewModel: LowDataBannerViewModel,
//    private val inlineOfferViewModel: SectionInlineOfferViewModel,
//    private val commentsViewModel: CommentsViewModel,
//    private val audioMediaActivityViewModel: AudioMediaActivityViewModel,
//    val meta: ArticleMeta,
//    val pushTopic: String? = "",
//    val pageIndex: Int,
//    val articlesInteractionHelper: ArticlesInteractionHelper,
//    val shouldSuppressAds: Boolean = false,
//    private val navigatedFromHistory: Boolean = false
//) : ArticleContentViewHolder(context, binding, articlesPagerCollaborationViewModel), TextSelection {
//
//    private lateinit var loadingAnimation: Animation
//
//    private var smoothScroller: RecyclerView.SmoothScroller? = null
//
//    private var scrollStartFired = false
//
//    private var scrollEnded = false
//
//    private var forYouRecircView: View? = null
//    private var forYouRecircViewHolder: ForYouRecirculationViewHolder? = null
//    private val autoRecircViewAndViewHoldersMap: MutableMap<View, AutoRecirculationViewHolder> = mutableMapOf()
//
//    /**
//     * Used to cache ad views so ads are not re-rendered.
//     */
//    val adViews = SparseArray<View>()
//
//    val subNavWebEmbedViews = SparseArray<View>()
//
//    private val currentPageObserver =
//        Observer<ArticlePage> {
//            updateOverlay()
////            collapse(binding.nativeItem.nativeItem)
//        }
//
//    /**
//     * Used to skip slide-in animation when navigating from TableOfContent
//     */
//    private var skipNextLufAnimation = false
//
//    private var marginItemDecoration: MarginItemDecoration? = null
//    private var cardifyDecorator: CardifyDecorator? = null
//    private var layoutSpec: BreakPoints.LayoutSpec? = null
//    private var galleryCollapseButtonTracker: GalleryCollapseButtonTracker? = null
//    private var adapterItems: MutableList<Item>? = null
//
//    private var anchorTracker: AnchorTracker? = null
//
//    override fun bind(savedState: Bundle?) {
////        binding.nativeItem.nativeItem.visibility = View.VISIBLE
//        articlesPagerCollaborationViewModel.currentPage.observe(
//            viewLifecycleOwner,
//            currentPageObserver,
//        )
////        binding.nativeItem.rcvItems.isSelectionEnabled = true
////        binding.nativeItem.rcvItems.setSelectionCallback(
////            TextSelectionHelper.createSelectionCallback(
////                context,
////                this,
////            ),
////        )
//        /*
//            Do not recycle ad views since they should not be redrawn.
//         */
//        binding.nativeItem.rcvItems.recycledViewPool
//            .setMaxRecycledViews(AD_TYPE, 0)
//        binding.nativeItem.rcvItems.recycledViewPool
//            .setMaxRecycledViews(SUB_NAV, 0)
//        binding.nativeItem.rcvItems.setViewCacheExtension(
//            object :
//                RecyclerView.ViewCacheExtension() {
//                override fun getViewForPositionAndType(
//                    recycler: RecyclerView.Recycler,
//                    position: Int,
//                    type: Int,
//                ): View? =
//                    if (type == AD_TYPE) {
//                        adViews.get(position)
//                    } else if (type == SUB_NAV) {
//                        subNavWebEmbedViews.get(
//                            position,
//                        )
//                    } else {
//                        null
//                    }
//            },
//        )
//        loadingAnimation = AnimationUtils.loadAnimation(context, com.washingtonpost.android.articles.R.anim.horizontal_anim)
//        marginItemDecoration =
//            MarginItemDecoration().also {
////                binding.nativeItem.rcvItems.addItemDecoration(it)
//            }
//        cardifyDecorator = CardifyDecorator(context, marginItemDecoration, adViews)
////        smoothScroller = ArticleLinearSmoothScroller(binding.nativeItem.rcvItems.context)
//        setOnScrollScaleWithAnimation()
//        meta.id?.let { id ->
//            articles2ViewModel.startLoadingArticle(meta)
//            articlesPagerCollaborationViewModel.setLiveMapLiveData(id, actionsLiveData)
//            observeArticlesFeedFetch()
//        }
//        observeInlineFollowState()
//        observeArticleRecircEvents()
//        binding.nativeItem.rcvItems.addOnScrollListener(
//            object : RecyclerView.OnScrollListener() {
//                override fun onScrolled(
//                    recyclerView: RecyclerView,
//                    dx: Int,
//                    dy: Int,
//                ) {
//                    super.onScrolled(recyclerView, dx, dy)
//                    val appContext = context.applicationContext
//                    if (appContext is PostTvApplication) {
//                        val videoManager = (appContext as PostTvApplication).videoManager
//                        val itemId = videoManager.playerFrame.tag
//                        if (itemId is Long) {
//                            val holder = recyclerView.findViewHolderForItemId(itemId)
//                            if (holder !is VideoViewHolder) {
//                                videoManager.onScrolled(null)
//                            }
//                        }
//                    }
//                }
//
//                override fun onScrollStateChanged(
//                    recyclerView: RecyclerView,
//                    newState: Int,
//                ) {
//                    super.onScrollStateChanged(recyclerView, newState)
//                    when {
//                        newState == SCROLL_STATE_DRAGGING && !scrollStartFired -> {
//                            scrollStartFired = true
//                            articlesInteractionHelper.onEventFired(
//                                ArticleInteractionEvent.ArticleScrollStartedEvent,
//                            )
//                        }
//
//                        newState == SCROLL_STATE_IDLE && scrollStartFired -> {
//                            if (isLastFeedItemReached() && !scrollEnded) {
//                                scrollEnded = true
//                                articlesInteractionHelper.onEventFired(
//                                    ArticleInteractionEvent.ArticleScrollStoppedEvent,
//                                )
//                            }
//
//                            updateScrollDepth()
//                        }
//                    }
//
//                /* Don't even need to start worrying about tracking the For You recirc module until
//                   the scroll has ended (meaning the user got to the end of the article content for the first time) */
//                    if (scrollEnded) {
//                        startOrStopForYouViewedTimersIfNeeded()
//                        startOrStopAutoRecircViewedTimersIfNeeded()
//                    }
//                }
//            },
//        )
//        binding.nativeItem.rcvItems.addOnItemTouchListener(
//            ScrollStopper(articlesPagerCollaborationViewModel),
//        )
//        listenToRCVItemsLayoutChanges()
//        observeSourceAnnotations()
//    }
//
//    private fun updateScrollDepth() {
////        val rv = binding.nativeItem.rcvItems
//        // Start at end of current rcvItems and find last fully visible content item
////        for (i in rv.size - 1 downTo 0) {
////            val view = rv.getChildAt(i)
////            val viewHolder = rv.getChildViewHolder(view)
////            if (UiUtils.isViewHeightFullyVisibleOnScreen(view) &&
////                isValidContentItemViewHolder(
////                    viewHolder,
////                )
////            ) {
////                val viewHolderId = getViewHolderId(viewHolder) ?: return
////                userHistoryViewModel.updateScrollDepth(viewHolderId)
////                articleWallHelperViewModel.updateScrollDepth(calculateScrollDepthPercentage(view))
////                return
////            }
////        }
//    }
//
//    private fun calculateScrollDepthPercentage(view: View): Int {
//        TODO()
////        val viewHolderPosition = binding.nativeItem.rcvItems.getChildAdapterPosition(view)
////        val rvItemCount =
////            binding.nativeItem.rcvItems.adapter
////                ?.itemCount ?: return -1
////        return ((viewHolderPosition.toDouble() / rvItemCount) * 100).toInt()
//    }
//
//    private fun isValidContentItemViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean =
//        when (viewHolder) {
//            is SanitizedHtmlViewHolder -> {
//                viewHolder.arcId != null
//            }
//            is ListViewHolder -> {
//                viewHolder.arcId != null
//            }
//            else -> {
//                false
//            }
//        }
//
//    private fun getViewHolderId(viewHolder: RecyclerView.ViewHolder): String? =
//        when (viewHolder) {
//            is SanitizedHtmlViewHolder -> {
//                viewHolder.arcId
//            }
//            is ListViewHolder -> {
//                viewHolder.arcId
//            }
//            else -> {
//                null
//            }
//        }
//
//    private fun startOrStopForYouViewedTimersIfNeeded() {
//        if (forYouRecircView == null || forYouRecircViewHolder == null) {
//            // Find the For You recirc view and viewholder
////            for (i in 0 until binding.nativeItem.rcvItems.childCount) {
////                val view = binding.nativeItem.rcvItems.getChildAt(i)
////                val adapterPosition = binding.nativeItem.rcvItems.getChildAdapterPosition(view)
////                val vh =
////                    binding.nativeItem.rcvItems.findViewHolderForAdapterPosition(
////                        adapterPosition,
////                    )
////                if (vh is ForYouRecirculationViewHolder) {
////                    forYouRecircView = view
////                    forYouRecircViewHolder = vh
////                }
////            }
//        }
//
//        val visibleVerticalPercentage = UiUtils.calculateVerticalVisiblePercentage(forYouRecircView)
//        /* If the For You recirc carousel is at least 50% visible, start recording fy_viewed
//           start times for its visible items, otherwise stop all of its items' timers */
//        if (visibleVerticalPercentage >= 50) {
//            forYouRecircViewHolder?.binding?.carouselView?.recyclerView?.let { rv ->
//                forYouActivityViewModel.feedData.value?.let { feedData ->
//                    feedData.recommendations
//                        ?.map {
//                            RecommendationsHelperItem(
//                                it.articleId,
//                                it.recReason,
//                                feedData.requestId,
//                                feedData.recipeId,
//                                feedData.testId,
//                                it.contentType
//                            )
//                        }?.let {
//                            userHistoryViewModel.startOrStopForYouViewedTimers(
//                                recommendationsItems = it,
//                                recyclerView = rv,
//                                surface = ForYouFeedRepositoryImpl.SURFACE_RECIRC
//                            )
//                        }
//                }
//            }
//        } else if (visibleVerticalPercentage < 50) {
//            userHistoryViewModel.stopAllForYouViewedTimers()
//        }
//    }
//
//    private fun startOrStopAutoRecircViewedTimersIfNeeded() {
//        if (autoRecircViewAndViewHoldersMap.isEmpty()) {
//            // Find the AutoRecirc view holders
//            for (i in 0 until binding.nativeItem.rcvItems.childCount) {
//                val view = binding.nativeItem.rcvItems.getChildAt(i)
//                val adapterPosition = binding.nativeItem.rcvItems.getChildAdapterPosition(view)
//                val vh =
//                    binding.nativeItem.rcvItems.findViewHolderForAdapterPosition(
//                        adapterPosition,
//                    )
//                if (vh is AutoRecirculationViewHolder) {
//                    autoRecircViewAndViewHoldersMap[view] = vh
//                }
//            }
//        }
//
//        autoRecircViewAndViewHoldersMap.forEach { (view, holder) ->
//            val visibleVerticalPercentage = UiUtils.calculateVerticalVisiblePercentage(view)
//            if (visibleVerticalPercentage >= 50) {
//                val recommendationsItems = holder.RTEhelperItems
//                if (recommendationsItems != null) {
//                    userHistoryViewModel.startOrStopAutoRecircViewedTimers(
//                        recommendationsItems = recommendationsItems,
//                        recyclerView = holder.binding.carouselView.recyclerView,
//                    )
//                }
//            } else if (visibleVerticalPercentage < 50) {
//                userHistoryViewModel.stopAllAutoRecircViewedTimers()
//            }
//        }
//    }
//
//    override fun unbind() {
//        cleanUp()
////        binding.nativeItem.imageviewArticleCurtainShine.clearAnimation()
//        articlesPagerCollaborationViewModel.currentPage.removeObserver(currentPageObserver)
//        smoothScroller = null
//    }
//
//    private fun updateOverlay() {
////        binding.nativeItem.rcvItems.adapter?.let { adapter ->
////            for (i in 0 until adapter.itemCount) {
////                when (adapter.getItemViewType(i)) {
////                    Articles2ItemsRecyclerViewAdapter.VIDEO_TYPE -> {
////                        binding.nativeItem.rcvItems.findViewHolderForAdapterPosition(i)?.let {
////                            (it as? VideoViewHolder)?.updateOverlay()
////                        }
////                    }
////
////                    else -> {
////                        // no op
////                    }
////                }
////            }
////        }
//    }
//
//    private fun cleanUp() {
////        cleanUpAds()
////        cleanUpSubNavWebEmbed()
////        // Release resources held by View Holders
////        binding.nativeItem.rcvItems.adapter?.let { adapter ->
////            for (i in 0 until adapter.itemCount) {
////                when (adapter.getItemViewType(i)) {
////                    Articles2ItemsRecyclerViewAdapter.VIDEO_TYPE -> {
////                        binding.nativeItem.rcvItems.findViewHolderForAdapterPosition(i)?.let {
////                            (it as? VideoViewHolder)?.releaseResources()
////                        }
////                    }
////
////                    else -> {
////                        // no op
////                    }
////                }
////            }
////        }
//    }
//
//    private fun cleanUpAds() {
//        for (i in 0 until adViews.size()) {
//            val key: Int = adViews.keyAt(i)
//            val view = adViews.get(key)
//            val adView = view.findViewById<BannerAdView>(R.id.ad_view)
//            adView.release()
//        }
//        adViews.clear()
//    }
//
//    private fun cleanUpSubNavWebEmbed() {
//        subNavWebEmbedViews.clear()
//    }
//
//    override fun retry() {
//        meta.id?.let {
//            articles2ViewModel.startLoadingArticle(meta)
//        }
//    }
//
//    private fun observeArticlesFeedFetch() {
////        val url = meta.id ?: ""
////        articles2ViewModel.articleContentState.observe(
////            viewLifecycleOwner,
////            Observer {
////                when (it) {
////                    ArticleContentState.Loading -> {
////                        binding.nativeItem.rcvItems.visibility = View.GONE
////                        binding.nativeItem.loadingCurtain.visibility = View.VISIBLE
////                        binding.nativeItem.imageviewArticleCurtainShine.clearAnimation()
////                        binding.nativeItem.imageviewArticleCurtainShine.startAnimation(loadingAnimation)
////                        binding.errorContainer.visibility = View.GONE
////                        binding.nativeItem.nativeItem.visibility = View.VISIBLE
////                        articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
////                            ArticleMetricsEvent.StartLoading(url),
////                        )
////                    }
////
////                    is ArticleContentState.Success -> {
////                        if (it.pendingArticle != null) return@Observer
////                        collapse(binding.nativeItem.nativeItem, getArticleCollapsedBackground())
////                        binding.nativeItem.loadingCurtain.visibility = View.GONE
////                        binding.nativeItem.imageviewArticleCurtainShine.clearAnimation()
////                        prepareSharingContent(it.article)
////                        populateArticleData(it.article, it.isUpdate)
////                        binding.nativeItem.rcvItems.visibility = View.VISIBLE
////                        binding.errorContainer.visibility = View.GONE
////                        articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
////                            ArticleMetricsEvent.StopLoading(url),
////                        )
////                        // Disabling RV animations when article is cardified.
////                        // Design wants content presentation fast when expand/collapse card items.
////                        if (it.article.isCardified()) {
////                            binding.nativeItem.rcvItems.itemAnimator = null
////                        }
////                    }
////
////                    ArticleContentState.Failure, ArticleContentState.UITimedOut -> {
////                        binding.nativeItem.imageviewArticleCurtainShine.clearAnimation()
////                        binding.nativeItem.rcvItems.visibility = View.GONE
////                        binding.nativeItem.loadingCurtain.visibility = View.GONE
////                        binding.nativeItem.nativeItem.visibility = View.GONE
////                        binding.errorContainer.visibility = View.VISIBLE
////                        articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
////                            ArticleMetricsEvent.StopLoading(url),
////                        )
////                    }
////
////                    else -> {
////                        // no op
////                    }
////                }
////            },
////        )
//    }
//
//    private fun populateArticleData(
//        article: Article2,
//        isUpdate: Boolean,
//    ) {
//        val url = meta.id ?: ""
//        val items = updateItems(article = article)?.toMutableList()
//        val adapter =
//            Articles2ItemsRecyclerViewAdapter(
//                followViewModel,
//                articles2ViewModel,
//                articlesInteractionHelper,
//                article,
//                pushTopic,
//                externalEventsCoordinator,
//                adViews,
//                subNavWebEmbedViews,
//                pageViewTimeTrackerViewModel,
//                forYouActivityViewModel,
//                recirculationViewModel,
//                videoActivityViewModel,
//                userHistoryViewModel,
//                lowDataBannerViewModel,
//                inlineOfferViewModel,
//                audioMediaActivityViewModel,
//                context,
//                shouldSuppressAds
//            )
//        recirculationViewModel.fetchAutoRecirculation(article)
//        articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
//            ArticleMetricsEvent.StartProcessing(
//                url,
//            ),
//        )
//        adapter.onCurrentListChanged = { oldList, newList ->
//            if (oldList.isEmpty() && newList.isNotEmpty()) {
//                articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
//                    ArticleMetricsEvent.StopProcessing(
//                        url,
//                    ),
//                )
//                articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
//                    ArticleMetricsEvent.StartDrawing(
//                        url,
//                    ),
//                )
//                binding.nativeItem.rcvItems.doOnChildLayout {
//                    val state = articles2ViewModel.articleContentState.value
//                    if (state is ArticleContentState.Success) {
//                        articlesPagerCollaborationViewModel.dispatchArticleMetricsEvent(
//                            ArticleMetricsEvent.StopDrawing(url, state.source),
//                        )
//                    }
//                }
//            }
//        }
//        adapter.bindLifecycleOwner(viewLifecycleOwner)
//        binding.nativeItem.rcvItems.adapter = adapter
//        val correctedList = articles2ViewModel.prepareContentListForAdapter(article, items)
//        processAndSubmitList(correctedList)
//
//        if (meta.deepestScrollId != null && !isDeepestScrollIdFirstItem(article.items)) {
//            scrollToAnchorPosition(items, meta.deepestScrollId)
//        } else {
//            if (!articles2ViewModel.handledAnchorIdScrollOnArticleOpen) {
//                // if the user was in a LUF article and an updated posted, they would then receive
//                // a red button that says Update. When they clicked on that button, it would scroll to the latest update
//                scrollToAnchorPosition(items, meta.anchorId)
//                articles2ViewModel.handledAnchorIdScrollOnArticleOpen = true
//            }
//        }
//
//        galleryCollapseButtonTracker?.let {
//            binding.nativeItem.rcvItems.removeOnScrollListener(it)
//        }
//
//        galleryCollapseButtonTracker =
//            GalleryCollapseButtonTracker(items) { show ->
//                val items = (binding.nativeItem.rcvItems.adapter as? Articles2ItemsRecyclerViewAdapter)?.currentList
//                val expandItem =
//                    items?.firstOrNull {
//                        it is GalleryExpandCollapse && it.galleryId == galleryCollapseButtonTracker?.buttonId
//                    }
//                val truncated = expandItem?.state?.isTruncated() ?: false
//                articles2ViewModel.updateShowFloatingButton(
//                    show = show && !truncated,
//                    galleryCollapseButtonTracker?.buttonId ?: "",
//                )
//            }
//        galleryCollapseButtonTracker?.let {
//            binding.nativeItem.rcvItems.addOnScrollListener(it)
//        }
//
//        anchorTracker?.let {
//            binding.nativeItem.rcvItems.removeOnScrollListener(it)
//        }
//        anchorTracker =
//            AnchorTracker(items) { anchor ->
//                if (anchor == null) {
//                    binding.stickyNav.visibility = View.GONE
//                } else {
//                    val entry = article.tableOfContents?.children?.find { it.anchor == anchor.id }
//                    if (entry != null) {
//                        val isVisible = binding.stickyNav.visibility == View.VISIBLE
//                        val durationOut = if (isVisible && !skipNextLufAnimation) 100L else 0L
//                        val durationIn = if (isVisible && !skipNextLufAnimation) 300L else 0L
//                        skipNextLufAnimation = false
//                        binding.stickyNav.visibility = View.VISIBLE
//                        binding.stickyNavText.tag = anchor.id
//                        binding.lufTextContainer.fadeOut(duration = durationOut) {
//                            binding.lufTextContainer.alpha = 1f
//                            binding.stickyNavText.text = entry.title
//                            if (entry.displayDate != null && entry.displayDate > 0) {
//                                binding.stickyNavTime.visibility = View.VISIBLE
//                                binding.stickyNavTime.text = entry.displayDate.toLUFDateFormat()
//                            } else {
//                                binding.stickyNavTime.visibility = View.GONE
//                            }
//                            binding.lufTextContainer.slideFromLeft(duration = durationIn)
//                        }
//                    } else {
//                        binding.stickyNav.visibility = View.GONE
//                    }
//                }
//            }
//        anchorTracker?.let {
//            binding.nativeItem.rcvItems.addOnScrollListener(it)
//        }
//
//        if (!isUpdate) {
//            observeTableOfContentsScrollToAnchorEvent()
//            observeRestoreShowTableOfContentsEvent()
//            observeGalleryButton()
//            observeItemsChangedEvent()
//        } else {
//            scrollToAnchorPosition(
//                items,
//                article.tableOfContents
//                    ?.children
//                    ?.maxByOrNull { it.displayDate ?: 0L }
//                    ?.anchor,
//            )
//            val firstAnchor = items?.firstOrNull { it is Anchor }
//            if (firstAnchor is Anchor) {
//                anchorTracker?.onAnchorChanged?.invoke(firstAnchor)
//            }
//        }
//
//        binding.stickyNav.setOnClickListener {
//            dispatchShowTableOfContentsEvent(article)
//        }
//    }
//
//    private fun isDeepestScrollIdFirstItem(items: List<Item>?) =
//        items
//            ?.firstOrNull { it.arcId != null && INCLUDED_CONTENT_TYPES.contains(it.type) }
//            ?.arcId == meta.deepestScrollId
//
//    private fun observeGalleryButton() {
//        articles2ViewModel.showCollapseFloating.observe(viewLifecycleOwner) {
//            if (it) {
//                binding.galleryCollapseButton.visibility = View.VISIBLE
//            } else {
//                binding.galleryCollapseButton.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun observeInlineFollowState() {
//        inlineTopicFollowViewModel.updateInlineTopicFollowItem.observe(viewLifecycleOwner) { key ->
////            val adapter =
////                binding.nativeItem.rcvItems.adapter as? Articles2ItemsRecyclerViewAdapter
////            val list = adapter?.currentList?.toMutableList()
////            list?.forEachIndexed { index, item ->
////                if (item is InlineTopicFollowItem) {
////                    adapter.notifyItemChanged(index)
////                }
////            }
//        }
//    }
//
//    private fun observeArticleRecircEvents() = with(viewLifecycleOwner) {
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                recirculationViewModel.event.collect {
//                    when (it) {
//                        is ArticleRecirculationEvent.OpenArticle -> {
//                            context.startActivity(it.articlesParcel.buildIntent(context))
//                        }
//
//                        null -> { /* no-op */
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun observeItemsChangedEvent() {
////        article2ItemsViewModel.itemsChangedEvent.observe(viewLifecycleOwner) { state ->
////            val items = (binding.nativeItem.rcvItems.adapter as? Articles2ItemsRecyclerViewAdapter)?.currentList ?: emptyList()
////            if (state.fromIndex in items.indices && items[state.fromIndex].state?.truncateState == ItemTruncateState.COLLAPSED) {
////                (binding.nativeItem.rcvItems.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
////                    state.fromIndex,
////                    (binding.nativeItem.rcvItems.height / 2f).toInt(),
////                )
////            }
////            article2ItemsViewModel.updateItemsVisibility(state.items)
////            galleryCollapseButtonTracker?.updateList(items)
////            cardifyDecorator?.processItemsCards(items)
////            val updatedItemsCount = state.items.size - state.fromIndex
////            binding.nativeItem.rcvItems.adapter?.notifyItemRangeChanged(
////                state.fromIndex,
////                updatedItemsCount,
////            )
////        }
//    }
//
//    private fun updateItems(article: Article2): List<Item>? {
//        val items: MutableList<Item>? = article.items?.toMutableList()
//
//        removeOutdatedItems(items, article)
//
//        if (!shouldSuppressAds) {
//            loadAds(items, article)
//        }
//
//        loadInlineAlertToggle(items, article)
//
//        loadInlineTopicFollow(items)
//
//        loadInlineOffer(items)
//
//        loadCards(items)
//
//        loadRecirc(items, article)
//
//        loadGallery(items)
//
//        items?.add(Tagline())
//
//        removeUnknownItems(items)
//
//        updateAuthorInfo(items, article)
//        updateItemsLayoutSpec(items)
//        galleryCollapseButtonTracker?.updateList(items)
//        article2ItemsViewModel.processItemsState(items)
//        cardifyDecorator?.processItemsCards(items)
//
//        return items
//    }
//
//    private fun loadCards(items: MutableList<Item>?) {
//        items ?: return
//        val itemsWithCollapseCards = mutableListOf<Item>()
//        var index = 0
//        var lastExpandCard: ExpandCollapseCard? = null
//        while (index < items.size) {
//            if (lastExpandCard != null && items[index].group != lastExpandCard.group) {
//                itemsWithCollapseCards.add(
//                    ExpandCollapseCard(
//                        lastExpandCard.type,
//                        lastExpandCard.truncatedLabel,
//                        lastExpandCard.expandedLabel,
//                    ).apply {
//                        group = lastExpandCard?.group
//                    },
//                )
//                lastExpandCard = null
//            } else if (items[index] is ExpandCollapseCard) {
//                lastExpandCard = items[index] as ExpandCollapseCard
//            }
//            itemsWithCollapseCards.add(items[index])
//            index++
//        }
//        // Handle case where expandCard on the last group and list is ended with that group.
//        if (lastExpandCard != null) {
//            itemsWithCollapseCards.add(
//                ExpandCollapseCard(
//                    lastExpandCard.type,
//                    lastExpandCard.truncatedLabel,
//                    lastExpandCard.expandedLabel,
//                ).apply {
//                    group = lastExpandCard?.group
//                },
//            )
//            lastExpandCard = null
//        }
//        items.apply {
//            clear()
//            addAll(itemsWithCollapseCards)
//        }
//    }
//
//    private fun loadGallery(items: MutableList<Item>?) {
//        items ?: return
//        val itemsWithExpandCollapseCards = mutableListOf<Item>()
//        var index = 0
//        while (index < items.size) {
//            if (items[index] is Gallery) {
//                val gallery = items[index] as Gallery
//                val imageItems =
//                    gallery.images?.map {
//                        it.apply {
//                            it.galleryId = gallery.id
//                            it.group = gallery.group
//                        }
//                    }
//                if (!imageItems.isNullOrEmpty()) {
//                    itemsWithExpandCollapseCards.add(imageItems.first())
//                    itemsWithExpandCollapseCards.add(
//                        GalleryExpandCollapse(
//                            type = "gallery_expand_card",
//                            galleryId = gallery.id,
//                        ).apply {
//                            group = items[index].group
//                        },
//                    )
//                    itemsWithExpandCollapseCards.addAll(imageItems.subList(1, imageItems.size))
//                }
//            } else {
//                itemsWithExpandCollapseCards.add(items[index])
//            }
//            index++
//        }
//        items.apply {
//            clear()
//            addAll(itemsWithExpandCollapseCards)
//        }
//    }
//
//    private fun loadRecirc(
//        items: MutableList<Item>?,
//        article: Article2,
//    ) {
//        forYouActivityViewModel.feedData.value?.recommendations?.let {
//            items?.add(ForYouRecirculationItem(article, it))
//        }
//
//        items?.add(RecirculationItem(article, RecirculationType.MOST_READ))
//    }
//
//    private fun loadInlineAlertToggle(
//        items: MutableList<Item>?,
//        article: Article2,
//    ) {
//        val inlineAlertToggleHelper = InlineAlertToggleHelper()
//        if (!FeedsVersionHelper(article).hasInlineAlertToggle) {
//            items?.let {
//                val inlineAlertToggleItem =
//                    inlineAlertToggleHelper.getInlineAlertToggleItem(article, items)
//                inlineAlertToggleItem?.let {
//                    items.add(inlineAlertToggleHelper.getToggleIndex(items, it, article), it)
//                }
//            }
//        } else {
//            // load Alerts toggle
//            val index = items?.indexOfFirst { it is Toggle && it.subtype == "alerts" }
//            items?.apply {
//                if (index != null && index != -1) {
//                    val toggle = items[index] as Toggle
//                    removeAt(index)
//                    if (inlineAlertToggleHelper.alertsDisabledForSegment(toggle.key)) {
//                        inlineAlertToggleHelper.convertToggleToInlineAlertToggleItem(toggle)?.let { inlineAlertToggleItem ->
//                            add(index, inlineAlertToggleItem)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun loadInlineOffer(items: MutableList<Item>?) {
//        val offerItems = items?.filter { it is Toggle && it.subtype == "offer" }
//        val inlineOfferHelper = InlineOfferHelper()
//        offerItems?.forEach {
//            val index = items.indexOf(it)
//            items.apply {
//                if (index != -1) {
//                    removeAt(index)
//                    inlineOfferHelper.getInlineOfferItem()?.let { inlineOfferItem ->
//                        add(index, inlineOfferItem)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun loadInlineTopicFollow(items: MutableList<Item>?) {
//        val topicFollowItems = items?.filter { it is Toggle && it.subtype == "topic-follow" }
//        topicFollowItems?.forEach {
//            val index = items.indexOf(it)
//            items.apply {
//                if (index != -1) {
//                    val toggle = items[index] as Toggle
//                    inlineTopicFollowViewModel
//                        .getInlineTopicFollowItem(
//                            toggle.key,
//                            toggle.group,
//                            context,
//                            true,
//                        )?.let { inlineItem ->
//                            removeAt(index)
//                            add(index, inlineItem)
//                        }
//                }
//            }
//        }
//    }
//
//    private fun removeOutdatedItems(
//        items: MutableList<Item>?,
//        article: Article2,
//    ) {
//        val omit: ((Item) -> Boolean) = { item ->
//            (item.hideVersion != null && item.hideVersion!! >= FeedsVersionHelper(article).supportsElevatedByline) ||
//                (item.showVersion != null && item.showVersion!! > (article.version ?: 0))
//        }
//        items?.removeAll { omit(it) }
//    }
//
//    private fun loadAds(
//        items: MutableList<Item>?,
//        article: Article2,
//    ) {
//        // App Side Ads Injection Logic when there are no ads in cached articles
//        // (temporary changes and will be cleaned after adoption).
//        if (!FeedsVersionHelper(article).hasInlineAds) {
//            val adInjector = ClassicAdInjector2()
//            val adPositions = adInjector.getAdPositions(article, article.items)
//            adPositions.forEach { key, value ->
//                items?.add(key, AdItem(adKey = value).toAd())
//            }
//        } else {
//            // Remove ads that are not allowed to be displayed on Android instead of updating ad views in ViewHolders.
//            // Remove when kind is "float"
//            var floatAdsExist = false
//            val shouldRemove: ((Item) -> Boolean) = {
//                (it is Ad && it.kind == AdKind.FLOAT.value).let { isFloatAd ->
//                    if (!floatAdsExist) floatAdsExist = isFloatAd
//                    isFloatAd
//                }
//            }
//            val itemsWithFinalAds =
//                items?.filterNot { shouldRemove(it) }?.toMutableList()
//            itemsWithFinalAds?.apply {
//                forEach {
//                    // Display block ads on both phones and tablets on Android irrespective of breakpoints when there are float ads.
//                    if (it is Ad && floatAdsExist) {
//                        it.breakpoints = null
//                    }
//                    if (it is Ad &&
//                        (
//                            it.kind.isNullOrEmpty() ||
//                                it.size.isNullOrEmpty() ||
//                                it.adPath.isNullOrEmpty() ||
//                                it.breakpoints.isNullOrEmpty()
//                        )
//                    ) {
//                        EventLog
//                            .Builder()
//                            .apply {
//                                setMessage("Feed Ad item values are missing")
//                                setModule(LogModules.ARTICLES)
//                                set("version", article.version)
//                                set("breakpoints", it.breakpoints)
//                                set("kind", it.kind)
//                                set("size", it.size)
//                                set("ad_path", it.adPath)
//                                set("article_url", article.contenturl)
//                            }.run {
//                                RemoteLog.d(context, build())
//                            }
//                    }
//                }
//                if (isNotEmpty()) {
//                    items.clear()
//                    items.addAll(itemsWithFinalAds)
//                }
//            }
//        }
//    }
//
//    private fun updateItemsLayoutSpec(items: List<Item>?) {
//        items?.forEach {
//            it.layoutSpec = this.layoutSpec
//        }
//    }
//
//    private fun removeUnknownItems(items: MutableList<Item>?) {
//        val isKnown: ((Item) -> Boolean) = { item ->
//            ArticleItemAdapterHelper.getItemViewType(item) > -1
//        }
//        items?.removeAll { !isKnown(it) }
//    }
//
//    private fun updateAuthorInfo(
//        items: MutableList<Item>?,
//        article: Article2,
//    ) {
//        if (article.isCardified() && layoutSpec?.layout == BreakPoints.Layout.SMALL) {
//            items?.firstOrNull { it is AuthorInfo }?.let {
//                (it as? AuthorInfo)?.showDivider = false
//            }
//        }
//    }
//
//    private fun dispatchShowTableOfContentsEvent(article: Article2) {
//        article.tableOfContents?.let {
//            articleTableOfContentsViewModel.setTableOfContentsEvent(
//                TableOfContentsEvent(
//                    meta.id,
//                    it,
//                    binding.stickyNavText.tag as? String,
//                ),
//            )
//            articleTableOfContentsViewModel.dispatchShowTableOfContentsEvent(true)
//        }
//    }
//
//    private fun observeTableOfContentsScrollToAnchorEvent() {
//        articleTableOfContentsViewModel.scrollToAnchorEvent.observe(viewLifecycleOwner) {
//            if (it != null && articleTableOfContentsViewModel.tableOfContentsEvent.value?.metaId == meta.id) {
//                skipNextLufAnimation = true
//                scrollToAnchorPosition(adapterItems, it)
//            }
//        }
//    }
//
//    private fun observeRestoreShowTableOfContentsEvent() {
//        articleTableOfContentsViewModel.restoreShowTableOfContentsEvent.observe(viewLifecycleOwner) {
//            if (it == meta.id) {
//                (articles2ViewModel.articleContentState.value as? ArticleContentState.Success)?.let {
//                    dispatchShowTableOfContentsEvent(it.article)
//                }
//            }
//        }
//    }
//
//    override fun onAudioClicked() {
//        binding.nativeItem.rcvItems.adapter?.let { adapter ->
//            for (i in 0 until adapter.itemCount) {
//                when (adapter.getItemViewType(i)) {
//                    Articles2ItemsRecyclerViewAdapter.VIDEO_TYPE -> {
//                        binding.nativeItem.rcvItems.findViewHolderForAdapterPosition(i)?.let {
//                            context.findActivityOfType<BaseActivity>()?.onAudioStarted()
//                            (it as? VideoViewHolder)?.resetVideo()
//                        }
//                    }
//
//                    else -> {
//                        // no op
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onViewCommentsClicked() {
//        val currentPage = articlesPagerCollaborationViewModel.currentPage.value
//        val fbTrackingInfo = articlesPagerCollaborationViewModel.getCurrentTrackingInfo()
//        val arcId = fbTrackingInfo?.omnitureX?.arcId ?: currentPage?.articleMeta?.id ?: meta.id
//        val storyUrl = (currentPage?.articleMeta?.id ?: meta.id)?.let { getUrlWithoutParameters(it) }
//        val storyTitle = (articles2ViewModel.articleContentState.value as? ArticleContentState.Success)?.article?.title ?: ""
//        val trackingInfo = fbTrackingInfo?.let {
//            it.omnitureX?.toTrackingInfo()?.apply {
//                contentURL = it.contentUrl
//            }
//        }
//        (context as? FragmentActivity)?.supportFragmentManager?.let { fm ->
//            if (!CommentBottomSheetFragment.showComments(
//                    fm,
//                    arcId,
//                    storyUrl,
//                    storyTitle,
//                    "bottom-article",
//                    trackingInfo
//                )) {
//                val commentsUrl = articlesPagerCollaborationViewModel.currentPage.value?.articleMeta?.id
//                    .toUri()
//                    .buildUpon()
//                    .appendQueryParameter(DeepLinksProcessor.OUTPUT_TYPE, DeepLinksProcessor.COMMENT)
//                    .appendQueryParameter(DeepLinksProcessor.NO_NAV, true.toString())
//                    .build()
//                    .toString()
//
//                WPUrlAnalyser.getWPUrlAnalyser().analyseAndStartIntent(context, commentsUrl, "")
//            }
//        }
//        fbTrackingInfo?.let {
//            articlesPagerCollaborationViewModel.dispatchFirebaseAnalyticsTrackingEvent(
//                FirebaseAnalyticsTrackingEvent.ViewCommentsTracking(it)
//            )
//        }
//    }
//
//    override fun onViewSpecificCommentClicked(commentIdUrl: String) {
//        val commentUrl = commentIdUrl
//            .toUri()
//            .buildUpon()
//            .appendQueryParameter(DeepLinksProcessor.NO_NAV, true.toString())
//            .build()
//            .toString()
//
//        WPUrlAnalyser.getWPUrlAnalyser().analyseAndStartIntent(context, commentUrl, "")
//    }
//
//    override fun onAuthorClicked(author: Author) {
//        articlesPagerCollaborationViewModel.authorClicked(author)
//    }
//
//    override fun onAskThePostClicked(questions: List<Question>) {
//        articlesPagerCollaborationViewModel.askThePostClicked(questions)
//    }
//
//    /**
//     * This fucntion just sets the sharing content in the view model so that it can be used when share icon is clicked by user from the activity toolbar
//     */
//    private fun prepareSharingContent(article: Article2) {
//        val byLineItem = article.items?.firstOrNull { item -> item is ByLine }
//        val byLineText = (byLineItem as? ByLine)?.content
//        setShareContent(article.contenturl, article.title, byLineText)
//    }
//
//    /**
//     * Sets the content that is used by [performShare] method when share from the toolbar is clicked on.
//     */
//    private fun setShareContent(
//        url: String,
//        content: String?,
//        byLine: String?,
//    ) {
//        articles2ViewModel.setShareContent(url, content, byLine)
//    }
//
//    private fun setOnScrollScaleWithAnimation() {
////        binding.nativeItem.rcvItems.addOnScrollListener(
////            object : RecyclerView.OnScrollListener() {
////                override fun onScrolled(
////                    recyclerView: RecyclerView,
////                    dx: Int,
////                    dy: Int,
////                ) {
////                    super.onScrolled(recyclerView, dx, dy)
////                    val llm = (binding.nativeItem.rcvItems.layoutManager as LinearLayoutManager)
////                    val isTopChildVisible = llm.findFirstVisibleItemPosition() <= 1
////                    val topChild = binding.nativeItem.rcvItems.getChildAt(0)
////                    val childY = topChild?.top ?: 0
////                    if (dy < 0 && isTopChildVisible && childY >= 0 && expanded) {
////                        collapse(
////                            binding.nativeItem.nativeItem,
////                            backgroundResId = getArticleCollapsedBackground(),
////                        )
////                    } else if (dy > 0 && !isTopChildVisible && !expanded) {
////                        expand(
////                            binding.nativeItem.nativeItem,
////                            backgroundResId = getArticleExpandedBackground(),
////                        )
////                        // SanitizedHtml view is not realigning its text content based on view width once container expands.
////                        // Text is readjusting once user taps on it.
////                        // So making a requestLayout call on all visible child views to get them updated as soon as they can.
////                        val firstVisibleChildPosition = llm.findFirstVisibleItemPosition()
////                        val lastVisibleChildPosition = llm.findLastVisibleItemPosition()
////                        for (i in firstVisibleChildPosition..lastVisibleChildPosition) {
////                            llm.getChildAt(i)?.requestLayout()
////                        }
////                    }
////                }
////            },
////        )
//    }
//
//    private fun getArticleCollapsedBackground(): Int {
//        if (layoutSpec?.layout == BreakPoints.Layout.LARGE) return com.washingtonpost.android.articles.R.drawable.card_article
//        val currentState = articles2ViewModel.articleContentState.value
//        if (currentState is ArticleContentState.Success) {
//            if (currentState.article.isCardified()) {
//                return com.washingtonpost.android.articles.R.drawable.card_article_cardified
//            }
//        }
//        return com.washingtonpost.android.articles.R.drawable.card_article
//    }
//
//    private fun getArticleExpandedBackground(): Int {
//        if (layoutSpec?.layout == BreakPoints.Layout.LARGE) return com.washingtonpost.android.articles.R.drawable.card_article_expanded
//        val currentState = articles2ViewModel.articleContentState.value
//        if (currentState is ArticleContentState.Success) {
//            if (currentState.article.isCardified()) {
//                return com.washingtonpost.android.articles.R.drawable.card_article_expanded_cardified
//            }
//        }
//        return com.washingtonpost.android.articles.R.drawable.card_article_expanded
//    }
//
//    override fun onImageClicked(image: Image) {
//        articlesPagerCollaborationViewModel.imageTapped(image)
//    }
//
//    override fun onLinkClicked(url: String) {
//        articlesPagerCollaborationViewModel.linkClicked(url)
//    }
//
//    override fun onLufOutcomePostClicked(url: String) {
//        articlesPagerCollaborationViewModel.lufOutcomePostClicked(url)
//    }
//
//    /**
//     * Triggered when share is tapped on in text selection mode.
//     */
//    override fun onSharePreformed(selectedText: String?) {
//        val currentPage = articlesPagerCollaborationViewModel.currentPage.value
//        val url = currentPage?.articleMeta?.id
//        url?.let {
//            articlesInteractionHelper.onEventFired(
//                ArticleInteractionEvent.TextSelectionShareEvent(
//                    ShareContent(
//                        url,
//                        selectedText,
//                        "",
//                    ),
//                ),
//            )
//        }
//    }
//
////    private fun isTextSelectionActive(): Boolean = binding.nativeItem.rcvItems.isSelectionActive
//
//    private fun scrollToAnchorPosition(
//        items: List<Item>?,
//        anchorId: String?,
//    ) {
////        /*calling this on main thread since it seems like it was using a background thread. It was
////        causing the article to load without the ads populated.*/
////        CoroutineScope(Dispatchers.Main).launch {
////            if (anchorId.isNullOrEmpty()) {
////                return@launch
////            }
////            items?.forEachIndexed { index, it ->
////                if (it is Anchor) {
////                    if (it.id.equals(anchorId, ignoreCase = true)) {
////                        (binding.nativeItem.rcvItems.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
////                            index,
////                            binding.stickyNav.measuredHeight,
////                        )
////                        return@forEachIndexed
////                    }
////                } else if (it.arcId == anchorId) {
////                    (binding.nativeItem.rcvItems.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
////                        index + 1,
////                        binding.nativeItem.rcvItems.height - 50,
////                    )
////                }
////                return@forEachIndexed
////            }
////        }
//    }
//
//    /**
//     * To know the last feed item in the recycler view is reached or not.
//     * @param excludedItemsCount default value is 3(for For You, Most Read and Tagline) to not to be considered.
//     * Caller can send 0 for excludedItemsCount to consider all items.
//     * @return Boolean
//     */
//    private fun isLastFeedItemReached(excludedItemsCount: Int = 3): Boolean {
//        TODO()
////        val lastVisibleItemPos =
////            (binding.nativeItem.rcvItems.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
////        return lastVisibleItemPos >= (
////            binding.nativeItem.rcvItems.adapter
////                ?.itemCount
////                ?: 0
////        ) - 1 - excludedItemsCount
//    }
//
//    private fun listenToRCVItemsLayoutChanges() {
////        binding.nativeItem.rcvItems.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
////            if (detectLayoutSizeChange(v)) {
////                cardifyDecorator?.let {
////                    if (layoutSpec?.layout == BreakPoints.Layout.SMALL) {
////                        binding.nativeItem.rcvItems.addItemDecoration(it)
////                    } else {
////                        binding.nativeItem.rcvItems.removeItemDecoration(it)
////                    }
////                }
////                cleanUpAds()
////                articles2ViewModel.articleContentState.let {
////                    (it.value as? ArticleContentState.Success)?.let { article ->
////                        (binding.nativeItem.rcvItems.adapter as? Articles2ItemsRecyclerViewAdapter)?.currentList?.let { items ->
////                            updateAuthorInfo(items, article.article)
////                            updateItemsLayoutSpec(items)
////                            article2ItemsViewModel.updateItemsVisibility(items)
////                            (binding.nativeItem.rcvItems.adapter as? ListAdapter<*, *>)?.notifyDataSetChanged()
////                        }
////                    }
////                }
////            }
////        }
//    }
//
//    private fun detectLayoutSizeChange(view: View): Boolean {
//        val nextItemVisiblePx =
//            view.resources.getDimensionPixelSize(
//                com.washingtonpost.android.articles.R.dimen.viewpager_next_article_visible,
//            )
//        val currentItemHorizontalMarginPx =
//            view.resources.getDimensionPixelSize(
//                com.washingtonpost.android.articles.R.dimen.viewpager_current_item_horizontal_margin,
//            )
//        val viewWidth = view.width + if (!expanded) nextItemVisiblePx + currentItemHorizontalMarginPx else 0
//        val layoutSpec =
//            BreakPoints.getLayoutSpec(
//                viewWidth.toDp(view.resources.displayMetrics.density),
//            )
//        if (layoutSpec != this.layoutSpec) {
//            this.layoutSpec = layoutSpec
//            return true
//        }
//        return false
//    }
//
//    private fun processAndSubmitList(items: List<Item>?) {
//        adapterItems = items?.toMutableList()
////        (binding.nativeItem.rcvItems.adapter as? ListAdapter<*, *>)?.submitList(
////            adapterItems as List<Nothing>?,
////        )
//    }
//
//    fun handleGalleryCollapseButton(galleryId: String?) {
//        galleryId ?: return
//        val items = adapterItems
//        val collapseCard = items?.firstOrNull { it is GalleryExpandCollapse && it.galleryId == galleryCollapseButtonTracker?.buttonId }
//        if (collapseCard != null) {
//            onTruncateItemClick(collapseCard)
//        }
//    }
//
//    fun onTruncateItemClick(item: Item) {
//        article2ItemsViewModel.onItemClick(item, adapterItems)
//    }
//
//    private fun observeSourceAnnotations() {
//        commentsViewModel.sourceAnnotations.observe(this.viewLifecycleOwner) { annotations ->
//            val contentState = articles2ViewModel.articleContentState.value
//            if (contentState is ArticleContentState.Success) {
//                contentState.article.items?.let { items ->
////                    val adapter = binding.nativeItem.rcvItems.adapter as? Articles2ItemsRecyclerViewAdapter
//                    items.forEachIndexed { index, item ->
//                        when (item) {
//                            is SanitizedHtml -> {
//                                if (item.content?.contains(CustomUrlPrefixes.FROM_THE_SOURCE.value,true) == true) {
//                                    item.sourceAnnotations = annotations.filterNotNull()
////                                    adapter?.notifyItemChanged(index)
//                                }
//                            }
//
//                            is FtsCarousel -> {
//                                item.sourceComments = annotations.mapNotNull { it?.sourceComments?.firstOrNull() }
////                                adapter?.notifyItemChanged(index)
//                            }
//
//                            else -> {}
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onVisibilityChanged(visibilty: Boolean) {
//        // Update visible items
////        val rv = binding.nativeItem.rcvItems
////        val llm = (rv.layoutManager as LinearLayoutManager)
////        val firstVisibleChildPosition = llm.findFirstVisibleItemPosition()
////        val lastVisibleChildPosition = llm.findLastVisibleItemPosition()
////        for (i in firstVisibleChildPosition..lastVisibleChildPosition) {
////            rv.findViewHolderForLayoutPosition(i)?.let {
////                if (it is ArticleItemViewHolder<*>) it.onVisibilityChanged(visibilty)
////            }
////        }
//    }
//}
