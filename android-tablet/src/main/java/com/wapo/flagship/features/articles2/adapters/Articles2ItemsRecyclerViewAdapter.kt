package com.wapo.flagship.features.articles2.adapters

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.wapo.adsinf.databinding.AdLayoutBinding
import com.wapo.adsinf.models.AdsModel
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles.ArticleLinkType
import com.wapo.flagship.features.articles.databinding.ArticleCarouselBinding
import com.wapo.flagship.features.articles.recirculation.Articles2RecirculationViewHolder
import com.wapo.flagship.features.articles.recirculation.Carousel2ItemFetcher
import com.wapo.flagship.features.articles.recirculation.CarouselEnvironment
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.diffutils.Articles2ItemDiffUtils
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.interfaces.ExternalEventsCoordinator
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.RecirculationType
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderView
import com.wapo.flagship.features.articles2.utils.ArticleItemAdapterHelper
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.articles2.viewholders.*
import com.wapo.flagship.features.articles2.viewholders.InlineMessageArticleHolder
import com.wapo.flagship.features.articles2.viewmodels.Articles2ViewModel
import com.wapo.flagship.features.articles2.viewmodels.PageViewTimeTrackerViewModel
import com.wapo.flagship.features.articles2.viewmodels.ArticleInlineMessageViewModel
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import com.wapo.flagship.features.lowdatamodelbanner.viewmodel.LowDataBannerViewModel
import com.wapo.flagship.features.video.viewmodels.VideoActivityViewModel
import com.wapo.flagship.model.ArticleMeta
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles2.viewmodels.recirculation.ArticleRecirculationViewModel
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.views.ExpandableListAdapter
import com.washingtonpost.android.databinding.*
import com.washingtonpost.android.follow.viewmodel.FollowViewModel
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.wapocontent.ILoader
import com.washingtonpost.android.wapocontent.LoaderProvider
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel

class Articles2ItemsRecyclerViewAdapter(
    private val followViewModel: FollowViewModel,
    private val articles2ViewModel: Articles2ViewModel,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
    private val article2: Article2,
    private val pushTopic: String?,
    private val externalEventsCoordinator: ExternalEventsCoordinator,
    private val adViews: SparseArray<View>,
    private val subNavViews: SparseArray<View>,
    private val pageViewTimeTrackerViewModel: PageViewTimeTrackerViewModel,
    private val forYouActivityViewModel: ForYouActivityViewModel,
    private val recirculationViewModel: ArticleRecirculationViewModel,
    private val videoActivityViewModel: VideoActivityViewModel,
    private val userHistoryViewModel: UserHistoryViewModel,
    private val lowDataBannerViewModel: LowDataBannerViewModel,
    private val articleInlineMessageViewModel: ArticleInlineMessageViewModel,
    private val audioMediaActivityViewModel: AudioMediaActivityViewModel,
    private val context: Context,
    private var adsModel: AdsModel
) : ExpandableListAdapter<Item, RecyclerView.ViewHolder>(Articles2ItemDiffUtils()) {
    val elementGroupStyleHelper = ElementGroupStyleHelper(context)
    val contextBoxStyleHelper = ContextBoxStyleHelper(context)
    var onCurrentListChanged: ((MutableList<Item>, MutableList<Item>) -> Unit)? = null
    var onNavigationBehaviorChanged: ((String?) -> Unit)? = null
    var imageLoader: ILoader? = null

    private var lifecycleOwner: LifecycleOwner? = null

    /**
     * Updates the ads model used by newly created [AdViewHolder] and [InlineMessageArticleHolder].
     * Call this when reusing an existing adapter after an ads mode change.
     */
    fun updateAdsModel(newAdsModel: AdsModel) {
        adsModel = newAdsModel
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val resolvedLifecycleOwner = lifecycleOwner
            ?: parent.findViewTreeLifecycleOwner()
            ?: parent.context as? LifecycleOwner
        return when (viewType) {
            KICKER_TYPE -> {
                val binding = ItemKickerBinding.inflate(inflater, parent, false)
                KickerViewHolder(binding)
            }
            SUB_NAV -> {
                val binding = ItemSubNavBinding.inflate(inflater, parent, false)
                SubNavViewHolder(binding, onNavigationBehaviorChanged)
            }
            TITLE_TYPE -> {
                val binding = ItemTitleBinding.inflate(inflater, parent, false)
                TitleViewHolder(binding)
            }
            BYLINE_TYPE -> {
                val binding = ItemBylineBinding.inflate(inflater, parent, false)
                ByLineViewHolder(binding, articlesInteractionHelper)
            }
            ELEVATED_BYLINE_TYPE -> {
                val binding = ItemElevatedBylineBinding.inflate(inflater, parent, false)
                ElevatedBylineViewHolder(binding, articlesInteractionHelper)
            }
            DATE_TYPE -> {
                val binding = ItemDateBinding.inflate(inflater, parent, false)
                DateViewHolder(binding)
            }
            DECK_TYPE -> {
                val binding = ItemDeckBinding.inflate(inflater, parent, false)
                DeckViewHolder(binding)
            }
            SANITIZED_HTML -> {
                val binding = ItemSanitizedHtmlBinding.inflate(inflater, parent, false)
                val questionSet = article2.atpQuestions?.questionSet ?: emptyList()
                SanitizedHtmlViewHolder(binding, questionSet, articlesInteractionHelper)
            }
            IMAGE_TYPE -> {
                val binding = ItemImageBinding.inflate(inflater, parent, false)
                ImageViewHolder(binding, articlesInteractionHelper)
            }
            LUF_LIVE_IMAGE_TYPE -> {
                if (context is LoaderProvider) {
                    imageLoader = (context as LoaderProvider).loader
                }
                val binding = LufLiveImageBinding.inflate(inflater, parent, false)
                ArticleLiveImageViewHolder(binding, articlesInteractionHelper, imageLoader)
            }
            CORRECTION_TYPE -> {
                val binding = ItemCorrectionBinding.inflate(inflater, parent, false)
                CorrectionViewHolder(binding, articlesInteractionHelper)
            }
            LIST_TYPE -> {
                val binding = ItemListBinding.inflate(inflater, parent, false)
                ListViewHolder(binding, articlesInteractionHelper)
            }
            DIVIDER_TYPE -> {
                val binding = ItemDividerBinding.inflate(inflater, parent, false)
                DividerViewHolder(binding)
            }
            VIDEO_TYPE -> {
                val binding = ItemVideoBinding.inflate(inflater, parent, false)
                requireNotNull(resolvedLifecycleOwner) {
                    "VideoViewHolder requires a LifecycleOwner, but none was found in lifecycleOwner, view tree, or parent.context"
                }
                VideoViewHolder(
                    binding,
                    article2,
                    videoActivityViewModel,
                    userHistoryViewModel,
                    resolvedLifecycleOwner,
                )
            }
            AUTHOR_INFO_TYPE -> {
                val binding = ItemAuthorInfoBinding.inflate(inflater, parent, false)
                AuthorInfoViewHolder(binding, followViewModel)
            }
            TWEET_TYPE -> {
                val binding = ItemTweetBinding.inflate(inflater, parent, false)
                TweetViewHolder(binding)
            }
            COMMENT_SUB_TYPE -> {
                val binding = ItemCommentsBinding.inflate(inflater, parent, false)
                CommentsViewHolder(binding, articlesInteractionHelper)
            }
            PODCAST_TYPE -> {
                val binding = ItemPodcastBinding.inflate(inflater, parent, false)
                PodcastViewHolder(binding)
            }
            INLINE_PODCAST_TYPE -> {
                val binding = ItemInlinePodcastBinding.inflate(inflater, parent, false)
                InlinePodcastViewHolder(binding, audioMediaActivityViewModel)
            }
            ELEMENT_GROUP_TYPE -> {
                val binding = ItemElementGroupBinding.inflate(inflater, parent, false)
                ElementGroupViewHolder(binding, this, articlesInteractionHelper)
            }
            TAGLINE_TYPE -> {
                val binding = ItemTaglineBinding.inflate(inflater, parent, false)
                TaglineViewHolder(binding)
            }
            PULL_QUOTE_TYPE -> {
                val binding = ItemPullQuoteBinding.inflate(inflater, parent, false)
                PullQuoteViewHolder(binding)
            }
            INTERSTITIAL_LINK_TYPE -> {
                val binding = ItemInterstatialLinkBinding.inflate(inflater, parent, false)
                InterstatialLinkViewHolder(binding, articlesInteractionHelper)
            }
            AD_TYPE -> {
                val binding = AdLayoutBinding.inflate(inflater, parent, false)
                // This should not be null unless the ad views are loaded ahead in time for articles that are not on screen. We do have remotelog for this.
                val jTid =
                    if (pageViewTimeTrackerViewModel.articleTimeStamp.articleUrl ==
                        getUrlWithoutParameters(
                            article2.contenturl,
                        )
                    ) {
                        pageViewTimeTrackerViewModel.articleTimeStamp.timeStamp
                    } else {
                        null
                    }
                AdViewHolder(binding, article2, pushTopic, jTid, adsModel)
            }
            HUMAN_AUDIO_TYPE -> {
                val binding = ItemHumanAudioBinding.inflate(inflater, parent, false)
                HumanAudioViewHolder(
                    binding,
                    articlesInteractionHelper,
                    externalEventsCoordinator,
                    followViewModel,
                )
            }
            AUDIO_TYPE -> {
                val binding = ItemAudioBinding.inflate(inflater, parent, false)
                requireNotNull(resolvedLifecycleOwner) {
                    "AudioViewHolder requires a LifecycleOwner, but none was found in lifecycleOwner, view tree, or parent.context"
                }
                AudioViewHolder(
                    binding,
                    externalEventsCoordinator,
                    resolvedLifecycleOwner,
                ) { item ->
                    articlesInteractionHelper.onEventFired(ArticleInteractionEvent.AudioItemClicked(item.mediaId!!))
                }
            }
            STANDALONE_AUDIO_TYPE -> {
                val binding = ItemStandaloneAudioBinding.inflate(inflater, parent, false)
                StandaloneAudioViewHolder(
                    binding,
                    articlesInteractionHelper,
                    externalEventsCoordinator,
                )
            }
            OLYMPICS_MEDALS -> {
                val binding = ItemOlympicsMedalsBinding.inflate(inflater, parent, false)
                OlympicsMedalsViewHolder(binding, articlesInteractionHelper)
            }
            ANCHOR -> {
                return ItemAnchorBinding
                    .inflate(inflater, parent, false)
                    .run { AnchorViewHolder(this) }
            }
            INSTAGRAM_TYPE -> {
                val binding = ItemInstagramBinding.inflate(inflater, parent, false)
                InstagramViewHolder(binding)
            }
            TABLE_TYPE -> {
                val binding = FragmentArticleTableBinding.inflate(inflater, parent, false)
                TableViewHolder(binding)
            }
            INLINE_ALERT_TOGGLE_ITEM -> {
                val binding = InlineAlertToggleBinding.inflate(inflater, parent, false)
                InlineAlertToggleViewHolder(binding)
            }
            INLINE_TOPIC_FOLLOW -> {
                val binding = InlineTopicFollowBinding.inflate(inflater, parent, false)
                InlineTopicFollowViewHolder(binding, articlesInteractionHelper)
            }
            INLINE_EXTRA_ACCOUNT_TOGGLE_ITEM -> {
                val binding = InlineOfferToggleBinding.inflate(inflater, parent, false)
                val inlineMessage = articleInlineMessageViewModel.articleInlineMessage.value
                InlineMessageArticleHolder(
                    binding,
                    inlineMessage,
                    articlesInteractionHelper,
                    adsModel
                )
            }
            CONTEXT_BOX_TYPE -> {
                val binding = ContextBoxBinding.inflate(inflater, parent, false)
                ContextBoxViewHolder(binding, articlesInteractionHelper, contextBoxStyleHelper)
            }
            FOR_YOU_RECIRC_TYPE -> {
                val binding = ArticleCarouselBinding.inflate(inflater, parent, false)
                val provider =
                    CarouselEnvironment(
                        parent.context,
                        lowDataBannerViewModel.lowDataBannerState,
                    )
                ForYouRecirculationViewHolder(
                    binding,
                    provider,
                    forYouActivityViewModel,
                    userHistoryViewModel,
                    { pos, list ->
                        val urlsArray = list?.map { it.contentUrl }
                        val intent =
                            ArticlesParcel
                                .builder()
                                .setArticleUrls(urlsArray, pos)
                                .setSectionDisplayName(ForYouRecirculationViewHolder.SECTION_TITLE)
                                .setCarouselOriginated(true)
                                .setRecircModuleOriginated(true)
                        val reason =
                            list?.getOrNull(pos)?.trackingString
                                ?: "recommendation_reason_missing__position${pos + 1}"
                        intent.setNavigationBehavior("recircmodule_for_you__$reason")
                        parent.context.startActivity(intent.buildIntent(parent.context))
                    },
                    articlesInteractionHelper,
                )
            }
            EXPAND_COLLAPSE_TYPE -> {
                val binding = ItemExpandCollapseBinding.inflate(inflater, parent, false)
                ExpandCollapseViewHolder(binding, articlesInteractionHelper)
            }
            OEMBED_TYPE -> {
                val binding = ItemArticleEmbedBinding.inflate(inflater, parent, false)
                EmbedViewHolder(binding)
            }
            GALLERY_EXPAND_COLLAPSE_TYPE -> {
                val binding = ItemExpandCollapseBinding.inflate(inflater, parent, false)
                GalleryExpandCollapseViewHolder(binding, articlesInteractionHelper)
            }
            LINK_BUTTON_SUB_TYPE -> {
                val binding = ItemLinkButtonBinding.inflate(inflater, parent, false)
                LinkButtonViewHolder(binding, articlesInteractionHelper)
            }
            PIN_TYPE -> {
                val binding = ItemPinBinding.inflate(inflater, parent, false)
                PinViewHolder(binding)
            }
            LIVE_OUTCOME -> {
                val binding = ItemLiveOutcomeBinding.inflate(inflater, parent, false)
                LiveOutcomeViewHolder(binding, articlesInteractionHelper)
            }
            MOST_READ -> {
                val carouselCache =
                    FlagshipApplication.getInstance().articleRecircCarouselCache
                val listFetcher = Carousel2ItemFetcher(carouselCache)
                val imageFetcher =
                    CarouselEnvironment(
                        parent.context,
                        lowDataBannerViewModel.lowDataBannerState,
                    )
                val clickListener =
                    object : Articles2RecirculationViewHolder.ClickListener {
                        override fun onClicked(
                            positionInCarousel: Int,
                            items: List<CarouselViewItem>?,
                            recirculationType: RecirculationType,
                        ) {
                            val urlsArray = items?.map { it.contentUrl }
                            val intent =
                                ArticlesParcel
                                    .builder()
                                    .setArticleUrls(urlsArray, positionInCarousel)
                                    .setSectionDisplayName(recirculationType.sectionName)
                                    .setCarouselOriginated(true)
                                    .setRecircModuleOriginated(true)
                            parent.context.startActivity(intent.buildIntent(parent.context))
                        }
                    }

                return Articles2RecirculationViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        com.wapo.flagship.features.articles.R.layout.article_carousel,
                        parent,
                        false,
                    ),
                    listFetcher,
                    imageFetcher,
                    clickListener,
                )
            }
            INLINE_CAROUSEL_TYPE -> {
                val binding = ArticleCarouselBinding.inflate(inflater, parent, false)
                val provider =
                    CarouselEnvironment(
                        parent.context,
                        lowDataBannerViewModel.lowDataBannerState,
                    )
                InStoryRecirculationViewHolder(
                    binding,
                    provider,
                    articlesInteractionHelper,
                    article2.contenturl,
                ) { pos, list ->
                    val articleMetas =
                        list?.map {
                            ArticleMeta(
                                it.contentUrl,
                                false,
                                ArticleLinkType.ARTICLE,
                                it.lmt ?: 0,
                            )
                        }
                    val intent =
                        ArticlesParcel
                            .builder()
                            .setArticleMetas(articleMetas, pos)
                            .setSectionDisplayName(InStoryRecirculationViewHolder.SECTION_TITLE)
                            .setCarouselTitle(list?.getOrNull(pos)?.trackingString)
                            .setCarouselOriginated(true)
                    parent.context.startActivity(intent.buildIntent(parent.context))
                }
            }
            FTS_CAROUSEL_TYPE -> {
                val composeView = ComposeView(parent.context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                FtsCarouselRecirculationViewHolder(composeView, articlesInteractionHelper)
            }
            AUTO_RECIRC_TYPE -> {
                requireNotNull(resolvedLifecycleOwner) {
                    "ArticleRecirculationViewHolder requires a LifecycleOwner, but none was found in lifecycleOwner, view tree, or parent.context"
                }
                val binding = ArticleCarouselBinding.inflate(inflater, parent, false)
                val provider = CarouselEnvironment(parent.context, lowDataBannerViewModel.lowDataBannerState)
                AutoRecirculationViewHolder(
                    binding,
                    recirculationViewModel,
                    userHistoryViewModel,
                    provider,
                    resolvedLifecycleOwner,
                )
            }
            else -> {
                val binding = ItemDefaultBinding.inflate(inflater, parent, false)
                DefaultViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val item: Item? = getItem(position)
        if (holder is AdViewHolder) {
            adViews.put(
                position,
                holder.getItemView(),
            )
        } else if (holder is SubNavViewHolder) {
            subNavViews.put(position, holder.getItemView())
        }

        item?.let {
            it.isLowDataModeEnable = lowDataBannerViewModel.lowDataBannerState.value?.isLowDataBannerEnable == true
            it.lowDataModeLive = lowDataBannerViewModel.lowDataBannerState
            it.pageName = article2.omniture?.pageName
            (holder as ArticleItemViewHolder<Item>).bind(it, position)
        } ?: Logger.e(
            TAG,
            "getItem(position) returned null for ContentUrl: ${article2.contenturl} and position: $position",
        )
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is PodcastViewHolder -> holder.unbindMedia()
            is HumanAudioViewHolder -> holder.unbind()
            is InlinePodcastViewHolder -> holder.unbindMedia()
            is ContextBoxViewHolder -> holder.unbind()
            is VideoViewHolder -> holder.unbindMedia()
            is EmbedViewHolder -> holder.unbind()
            is ArticleItemLowDataModeViewHolder<*> -> holder.unbind()
            is AudioViewHolder -> holder.unbind()
            is AutoRecirculationViewHolder -> holder.unbind()
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is VideoViewHolder -> holder.bindMedia()
        }
        super.onViewAttachedToWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is VideoViewHolder -> holder.onViewRecycled()
            is AudioViewHolder -> holder.unbind()
            is InlineMessageArticleHolder -> holder.unbind()
            is ForYouRecirculationViewHolder -> holder.unbind()
            is AutoRecirculationViewHolder -> holder.unbind()
        }
        super.onViewRecycled(holder)
    }

    override fun getItemViewType(position: Int): Int {
        val item: Item? = getItem(position)
        if (item == null) {
            Logger.e(
                TAG,
                "getItem(position) returned null for ContentUrl: ${article2.contenturl} and position: $position",
            )
        }

        if (item?.invisible == true) {
            // Return default view type to draw an empty item.
            return 0
        }

        val viewType = ArticleItemAdapterHelper.getItemViewType(getItem(position))
        return if (viewType == -1) super.getItemViewType(position) else viewType
    }

    fun bindLifecycleOwner(owner: LifecycleOwner) {
        this.lifecycleOwner = owner
    }

    companion object {
        const val KICKER_TYPE = 1
        const val TITLE_TYPE = 2
        const val BYLINE_TYPE = 3
        const val DATE_TYPE = 4
        const val DECK_TYPE = 5
        const val IMAGE_TYPE = 6
        const val CORRECTION_TYPE = 7
        const val SANITIZED_HTML = 8
        const val LIST_TYPE = 9
        const val VIDEO_TYPE = 10
        const val PULL_QUOTE_TYPE = 11
        const val TWEET_TYPE = 12
        const val GALLERY_EXPAND_COLLAPSE_TYPE = 13
        const val INTERSTITIAL_LINK_TYPE = 14
        const val LINK_TYPE = 15
        const val AUTHOR_INFO_TYPE = 16
        const val ELEMENT_GROUP_TYPE = 17
        const val DIVIDER_TYPE = 18
        const val COMMENT_SUB_TYPE = 19
        const val PODCAST_TYPE = 20
        const val TAGLINE_TYPE = 21
        const val AD_TYPE = 22
        const val AUDIO_TYPE = 23
        const val OLYMPICS_MEDALS = 24
        const val ANCHOR = 25
        const val INSTAGRAM_TYPE = 26
        const val TABLE_TYPE = 27
        const val INLINE_ALERT_TOGGLE_ITEM = 28
        const val INLINE_PODCAST_TYPE = 29
        const val HUMAN_AUDIO_TYPE = 30
        const val FOR_YOU_RECIRC_TYPE = 31
        const val CONTEXT_BOX_TYPE = 32
        const val EXPAND_COLLAPSE_TYPE = 33
        const val OEMBED_TYPE = 34
        const val LINK_BUTTON_SUB_TYPE = 35
        const val LUF_LIVE_IMAGE_TYPE = 36
        const val PIN_TYPE = 37
        const val INLINE_TOPIC_FOLLOW = 38
        const val MOST_READ = 39
        const val INLINE_CAROUSEL_TYPE = 40
        const val STANDALONE_AUDIO_TYPE = 41
        const val LIVE_OUTCOME = 42
        const val SUB_NAV = 43
        const val ELEVATED_BYLINE_TYPE = 44
        const val INLINE_EXTRA_ACCOUNT_TOGGLE_ITEM = 45
        const val FTS_CAROUSEL_TYPE = 46
        const val AUTO_RECIRC_TYPE = 47
        const val PDF_TYPE = 48
        const val BLOCK_QUOTE_TYPE = 49
        const val QUOTE_TYPE = 50
        const val TAG = "Articles2ItemsRecyclerView"

        const val INLINE_PODCAST_SUBTYPE = "inline"
        const val HUMAN_AUDIO_SUBTYPE = "human"
        const val STANDALONE_AUDIO_SUBTYPE = "standalone"
    }

    open class ArticleItemViewHolder<T : Item>(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        open fun bind(
            item: T,
            position: Int,
        ) {
        }

        open fun unbind() {}

        /**
         * Method to delegate recycler view's visibility to its children/view holders.
         */
        open fun onVisibilityChanged(visibility: Boolean) {}
    }

    abstract class ArticleItemLowDataModeViewHolder<T : Item>(
        itemView: View,
        private val composeContainer: ComposeView?,
    ) : ArticleItemViewHolder<T>(
            itemView,
        ) {
        var observer: Observer<LowDataBanner>? = null

        override fun bind(
            item: T,
            position: Int,
        ) {
            observer =
                Observer { value ->
                    if (value.isLowDataBannerEnable && !item.isItemAlreadyShowed) {
                        composeContainer?.isVisible = true
                        onLowDataModeEnable(item)
                    } else {
                        onLowDataModeDisable(item)
                    }
                }

            itemView.findComponentActivity()?.let {
                observer?.let { notNullObserver ->
                    item.lowDataModeLive?.observe(it, notNullObserver)
                }
            }

            onBindItem(item, position)

            checkLowDataModeState(item)

            composeContainer?.setContent {
                val itemState =
                    item.placeHolderState ?: MutableLiveData<PlaceHolderState>(
                        PlaceHolderState.PlaceHolderEnable,
                    )
                item.placeHolderState = itemState
                setPlaceHolderData(item)?.let { data ->
                    PlaceHolderView(
                        isVisible = true,
                        data = data,
                        state = itemState,
                    ) {
                        item.placeHolderState?.postValue(PlaceHolderState.PlaceHolderLoading)
                        data.onLoadResource()
                        Measurement.trackLowDataModeUnhideMedia(item.pageName, getTrackMediaType())
                    }
                }
            }
        }

        fun onItemReady(item: T) {
            item.placeHolderState?.postValue(PlaceHolderState.PlaceHolderDisable)
            composeContainer?.isVisible = false
        }

        // Method in charge of bind the view into the child class
        open fun onBindItem(
            item: T,
            position: Int,
        ) {}

        // Method in chat of return the configuration of the placeholder
        abstract fun setPlaceHolderData(item: T): PlaceHolderData?

        // Implement this method to hide the view
        abstract fun onLowDataModeEnable(item: T)

        abstract fun onLowDataModeDisable(item: T)

        override fun unbind() {
            observer = null
        }

        open fun aspectRatio(
            imageWidth: Int? = null,
            imageHeight: Int? = null,
        ): Float? = null

        private fun checkLowDataModeState(item: T) {
            if (item.isLowDataModeEnable && !item.isItemAlreadyShowed) {
                composeContainer?.isVisible = true
                onLowDataModeEnable(item)
            } else {
                composeContainer?.isVisible = false
                onLowDataModeDisable(item)
            }
        }

        private fun getTrackMediaType(): String =
            when (this) {
                is ArticleLiveImageViewHolder -> "liveimage"
                is EmbedViewHolder -> "embed"
                is ImageViewHolder -> "image"
                is InlineTopicFollowViewHolder -> "inlinetopic"
                is PodcastViewHolder -> "podcast"
                is VideoViewHolder -> "video"
                else -> "media"
            }

        sealed class PlaceHolderState {
            object PlaceHolderEnable : PlaceHolderState()

            object PlaceHolderLoading : PlaceHolderState()

            object PlaceHolderDisable : PlaceHolderState()
        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Item>,
        currentList: MutableList<Item>,
    ) {
        super.onCurrentListChanged(previousList, currentList)
        onCurrentListChanged?.invoke(previousList, currentList)
    }
}
