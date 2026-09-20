// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles2.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.UiUtils
import com.wapo.flagship.features.articles.databinding.ArticleCarouselBinding
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.ForYouRecirculationItem
import com.wapo.flagship.features.articles2.utils.KickerStyleHelper.getStyle
import com.washingtonpost.android.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.utils.ArticleStyleCarouselHelper
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.foryou.data.byline
import com.washingtonpost.foryou.data.getURL
import com.washingtonpost.foryou.data.toNamesString
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import com.washingtonpost.userhistory.ForYouViewedAction
import com.washingtonpost.userhistory.models.RecommendationsHelperItem
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel

class ForYouRecirculationViewHolder(
    val binding: ArticleCarouselBinding,
    private val requestListener: CarouselProvider,
    private val forYouActivityViewModel: ForYouActivityViewModel,
    private val userHistoryViewModel: UserHistoryViewModel,
    private val onCardClicked: (Int, List<CarouselViewItem>?) -> Unit,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<ForYouRecirculationItem>(binding.root) {

    val recommendationsHelperItems = mutableListOf<RecommendationsHelperItem>()

    override fun bind(
        item: ForYouRecirculationItem,
        position: Int,
    ) {
        super.bind(item, position)

        val carouselItems = getCarouselItems(item)

        val feed = forYouActivityViewModel.feedData.value
        recommendationsHelperItems.apply {
            clear()
            addAll(
                item.list.map {
                    RecommendationsHelperItem(
                        it.articleId,
                        it.recReason,
                        feed?.requestId,
                        feed?.recipeId,
                        feed?.testId,
                        it.contentType
                    )
                })
        }
        val clickListener: OnCarouselClickedListener =
            object : OnCarouselClickedListener {
                override fun onCardClicked(
                    url: String?,
                    positionInCarousel: Int,
                ) {
                    userHistoryViewModel.captureForYouViewAction(
                        action = ForYouViewedAction.CLICKED,
                        recommendationsItem = recommendationsHelperItems[positionInCarousel],
                        adapterPosition = positionInCarousel,
                        surface = ForYouFeedRepositoryImpl.SURFACE_RECIRC
                    )
                    this@ForYouRecirculationViewHolder.onCardClicked(
                        positionInCarousel,
                        carouselItems,
                    )
                }
            }
        val cardWidth =
            itemView.resources.getDimensionPixelSize(
                com.washingtonpost.android.recirculation.R.dimen.carousel_article_style_card_width,
            )
        val cardPadding =
            itemView.resources.getDimensionPixelSize(com.washingtonpost.android.sections.R.dimen.carousel_story_card_padding)
        val isTablet = AppContextUtils.isTablet()
        binding.carouselView.initLayout(
            requestListener,
            clickListener,
            object : CarouselView.CarouselConsumeTouchEventRule {
                override fun canCarouselConsumeTouchEvent(): Boolean {
                    // TODO refactor
                    // In phones true
                    // In tablets, only in preview mode
                    // But since, tablets only show the carousel in full screen now, I am going to pass true
                    // TODO revisit this logic in tablets
                    return true
                }
            },
            true,
            shouldDisplayArrow = isTablet,
            shouldDisplayDateTime = false,
            fixedCardWidth = cardWidth,
            fixedCardPadding = cardPadding,
        )

        updateContainerHeight(carouselItems)
        binding.carouselView.carouselItemsFetchListener.onCarouselItemsFetched(carouselItems)

        binding.carouselView.setInStoryCarouselDesign()
        binding.carouselView.setInStoryDividers()

        val horizontalPadding = 0
        val topPadding =
            binding.carouselView.context.resources.getDimension(
                com.washingtonpost.android.recirculation.R.dimen.carousel_recycler_top_padding,
            )
        val bottomPadding =
            binding.carouselView.context.resources.getDimension(
                com.washingtonpost.android.recirculation.R.dimen.carousel_recycler_bottom_padding,
            )
        binding.carouselView.recyclerView.setPadding(
            horizontalPadding.toInt(),
            topPadding.toInt(),
            horizontalPadding.toInt(),
            bottomPadding.toInt(),
        )

        binding.carouselView.recyclerView.apply {
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) {
                        super.onScrolled(recyclerView, dx, dy)
                        updateForYouViewedEvents()
                    }
                },
            )
        }

        binding.carouselView.setOnCarouselItemTouchListener(object: OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.CarouselCardSwipeEvent(!disallowIntercept))
            }
        })

    }

    private fun getCarouselItems(recircItem: ForYouRecirculationItem): List<CarouselViewItem> =
        recircItem.list.filter { it.headline != null && recircItem.article.contenturl != it.url }.map {
            CarouselViewItem(
                hashCode(),
                it.getURL(),
                it.label?.transparency?.text,
                it.label?.basic?.text,
                it.label?.basic?.text ?: "",
                it.headline ?: "",
                it.byline(),
                it.imageUrl,
                SECTION_TITLE,
                true,
                it.recReason,
                it.lastUpdatedDate,
                "", // TODO Update
                it.labelDisplay?.basic?.headlinePrefix,
                style =
                    it.labelDisplay
                        ?.basic
                        ?.style
                        ?.let { it1 -> getStyle(it1) },
                secondaryLabel = it.authors.toNamesString()
            )
        }

    private fun updateContainerHeight(carouselViewItems: List<CarouselViewItem>?) {
        carouselViewItems ?: return
        ArticleStyleCarouselHelper
            .calculateTallestCardHeight(
                itemView.context,
                carouselViewItems,
            ).let { tallestCardHeight ->
                binding.carouselView.recyclerView.apply {
                    minimumHeight = tallestCardHeight
                }
            }
    }

    override fun unbind() {
        recommendationsHelperItems.clear()
        requestListener.tearDown()
    }

    override fun onVisibilityChanged(visibility: Boolean) {
        if (visibility) {
            updateForYouViewedEvents()
        } else {
            userHistoryViewModel.stopAllForYouViewedTimers()
        }
    }

    private fun updateForYouViewedEvents() {
        // If carousel itself is at least 50% visible, start recording fy_viewed start times
        if (UiUtils.calculateVerticalVisiblePercentage(binding.carouselView) >= 50) {
            userHistoryViewModel.startOrStopForYouViewedTimers(
                recommendationsItems = recommendationsHelperItems,
                recyclerView = binding.carouselView.recyclerView,
                surface = ForYouFeedRepositoryImpl.SURFACE_RECIRC
            )
        } else {
            userHistoryViewModel.stopAllForYouViewedTimers()
        }
    }

    companion object {
        const val SECTION_TITLE = "More for you"
    }
}
