package com.wapo.flagship.features.articles2.viewholders

import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.UiUtils
import com.wapo.flagship.features.articles.databinding.ArticleCarouselBinding
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.AutoRecircCarousel
import com.wapo.flagship.features.articles2.viewmodels.recirculation.ArticleRecirculationViewModel
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselArticleItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.utils.ArticleStyleCarouselHelper
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.userhistory.HeadlineViewAction
import com.washingtonpost.userhistory.models.AutoRecircHelperItem
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AutoRecirculationViewHolder(
    val binding: ArticleCarouselBinding,
    private val viewModel: ArticleRecirculationViewModel,
    private val userHistoryViewModel: UserHistoryViewModel,
    private val requestListener: CarouselProvider,
    private val lifecycleOwner: LifecycleOwner,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<AutoRecircCarousel>(binding.root) {
    private val coroutineScope = lifecycleOwner.lifecycleScope
    private var observeUiStateJob: Job? = null
    private var _collectionId: String? = null
    val collectionId: String get() = _collectionId.orEmpty()
    val RTEhelperItems: List<AutoRecircHelperItem>?
        get() {
            val collections = viewModel.uiState.value.collections
            val collection = collections[collectionId]
            val recommendationsItems = collection?.items?.mapIndexed { itemIndex, item ->
                AutoRecircHelperItem(
                    articleId = (item as? CarouselArticleItem)?.arcId.orEmpty(),
                    requestId = collection.requestId.orEmpty(),
                    currentUrl = item.contentUrl,
                    collectionCategory = collection.category.orEmpty(),
                    positionInModule = itemIndex,
                )
            }
            return recommendationsItems
        }

    override fun bind(item: AutoRecircCarousel, position: Int) {
        super.bind(item, position)
        _collectionId = item.carouselId.toString()
        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {
                RTEhelperItems?.getOrNull(positionInCarousel)?.let { recommendationItem ->
                    userHistoryViewModel.captureAutoRecircViewAction(
                        action = HeadlineViewAction.CLICKED,
                        carouselId = collectionId,
                        recommendationsItem = recommendationItem,
                        adapterPosition = positionInCarousel,
                    )
                }
                viewModel.onItemClicked(collectionId, positionInCarousel)
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
                    return true
                }
            },
            shouldDisplaySectionName = true,
            shouldDisplayArrow = isTablet,
            shouldDisplayDateTime = false,
            fixedCardWidth = cardWidth,
            fixedCardPadding = cardPadding,
        )

        observeUiState(collectionId)

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
            horizontalPadding,
            topPadding.toInt(),
            horizontalPadding,
            bottomPadding.toInt(),
        )

        binding.carouselView.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateHeadlineViewRTEEvents()
                }
            }
        )
    }

    private fun observeUiState(collectionId: String) {
        observeUiStateJob?.cancel()
        observeUiStateJob = coroutineScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    val items = it.collections[collectionId]?.items.orEmpty()
                    if (it.error != null || items.isEmpty()) {
                        binding.carouselView.isVisible = false
                        binding.root.requestLayout()
                        return@collect
                    } else if(!binding.carouselView.isVisible) {
                        binding.carouselView.isVisible = true
                        binding.root.requestLayout()
                    }

                    updateContainerHeight(items)
                    binding.carouselView.carouselItemsFetchListener.onCarouselItemsFetched(items)
                }
            }
        }
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

    override fun onVisibilityChanged(visibility: Boolean) {
        if (visibility) {
            updateHeadlineViewRTEEvents()
        } else {
            userHistoryViewModel.stopAllAutoRecircViewedTimers(carouselId = collectionId)
        }
    }

    private fun updateHeadlineViewRTEEvents() {
        val recommendationItems = RTEhelperItems ?: return
        // If carousel itself is at least 50% visible, start recording fy_viewed start times
        if (UiUtils.calculateVerticalVisiblePercentage(binding.carouselView) >= 50) {
            userHistoryViewModel.startOrStopAutoRecircViewedTimers(
                carouselId = collectionId,
                recommendationsItems = recommendationItems,
                recyclerView = binding.carouselView.recyclerView,
            )
        } else {
            userHistoryViewModel.stopAllAutoRecircViewedTimers(carouselId = collectionId)
        }
    }

    override fun unbind() {
        observeUiStateJob?.cancel()
        super.unbind()
    }
}