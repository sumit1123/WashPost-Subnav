package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.articles.databinding.ArticleCarouselBinding
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.CarouselItem
import com.wapo.flagship.features.articles2.models.deserialized.InlineCarousel
import com.wapo.flagship.features.articles2.utils.KickerStyleHelper.getStyle
import com.washingtonpost.android.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.utils.ArticleStyleCarouselHelper
import com.washingtonpost.android.recirculation.carousel.views.CarouselView

class InStoryRecirculationViewHolder(
    val binding: ArticleCarouselBinding,
    private val requestListener: CarouselProvider,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
    private val currentArticleUrl: String,
    private val onCardClicked: (Int, List<CarouselViewItem>?) -> Unit,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<InlineCarousel>(binding.root) {
    override fun bind(
        item: InlineCarousel,
        position: Int,
    ) {
        super.bind(item, position)

        val carouselItems =
            item.carouselItems?.let {
                getCarouselItems(
                    item.headline ?: "",
                    it,
                    currentArticleUrl,
                )
            }

        val clickListener: OnCarouselClickedListener =
            object : OnCarouselClickedListener {
                override fun onCardClicked(
                    url: String?,
                    positionInCarousel: Int,
                ) {
                    this@InStoryRecirculationViewHolder.onCardClicked(
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

        binding.carouselView.setOnCarouselItemTouchListener(object: OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                articlesInteractionHelper.onEventFired(ArticleInteractionEvent.CarouselCardSwipeEvent(!disallowIntercept))
            }
        })

    }

    private fun getCarouselItems(
        title: String,
        items: List<CarouselItem>,
        currentArticleUrl: String,
    ): List<CarouselViewItem> =
        items.filter { it.title?.content?.isNotEmpty() == true && it.contentUrl != currentArticleUrl }.map {
            CarouselViewItem(
                hashCode(),
                it.contentUrl ?: "",
                it.label?.displayLabel ?: "",
                it.label?.displayTransparency,
                it.label?.displayTransparency ?: "",
                it.title?.content ?: "",
                it.byLine?.content ?: "",
                it.image?.imageURL ?: "",
                title,
                true,
                null,
                null,
                title,
                it.title?.prefix,
                lmt = it.lmt,
                isLive = it.label?.coverageActive,
                style = it.label?.style?.let { it1 -> getStyle(it1) },
                secondaryLabel = it.analytics?.contentAuthor
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

    companion object {
        const val SECTION_TITLE = "Inline Carousel"
    }
}
