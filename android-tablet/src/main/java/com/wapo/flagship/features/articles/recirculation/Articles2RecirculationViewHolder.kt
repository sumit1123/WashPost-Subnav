// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles.recirculation

import android.content.Context
import android.view.View
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.RecirculationItem
import com.wapo.flagship.features.articles2.models.deserialized.RecirculationType
import com.washingtonpost.android.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.utils.ArticleStyleCarouselHelper
import com.washingtonpost.android.recirculation.carousel.views.CarouselView

class Articles2RecirculationViewHolder(
    itemView: View,
    private val itemsFetcher: CarouselItemsFetcher,
    private val requestListener: CarouselProvider,
    private val clickListener: ClickListener,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<RecirculationItem>(itemView) {
    private lateinit var carouselView: CarouselView
    private var items: List<CarouselViewItem>? = null

    override fun bind(
        item: RecirculationItem,
        position: Int,
    ) {
        super.bind(item, position)
        carouselView = itemView.findViewById(com.washingtonpost.android.save.R.id.carousel_view)

        val clickListener: OnCarouselClickedListener =
            object : OnCarouselClickedListener {
                override fun onCardClicked(
                    url: String?,
                    positionInCarousel: Int,
                ) {
                    this@Articles2RecirculationViewHolder.clickListener.onClicked(
                        positionInCarousel,
                        items,
                        item.recirculationType,
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
        carouselView.initLayout(
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
        itemsFetcher.getCarouselItems(
            itemView.context,
            item,
            object : CarouselItemsFetcher.ResultListener {
                override fun onSuccess(items: List<CarouselViewItem>) {
                    updateContainerHeight(items)
                    this@Articles2RecirculationViewHolder.items = items
                    carouselView.carouselItemsFetchListener.onCarouselItemsFetched(items)
                }

                override fun onError(e: Throwable) {
                    carouselView.setVisibility(View.GONE)
                    itemView.requestLayout()
                }
            },
        )

        carouselView.setInStoryCarouselDesign()
        carouselView.setInStoryDividers()

        val horizontalPadding = 0
        val topPadding =
            carouselView.context.resources.getDimension(
                com.washingtonpost.android.recirculation.R.dimen.carousel_recycler_top_padding,
            )
        val bottomPadding =
            carouselView.context.resources.getDimension(
                com.washingtonpost.android.recirculation.R.dimen.carousel_recycler_bottom_padding,
            )
        carouselView.recyclerView?.setPadding(
            horizontalPadding.toInt(),
            topPadding.toInt(),
            horizontalPadding.toInt(),
            bottomPadding.toInt(),
        )
    }

    private fun updateContainerHeight(carouselViewItems: List<CarouselViewItem>?) {
        carouselViewItems ?: return
        ArticleStyleCarouselHelper
            .calculateTallestCardHeight(
                itemView.context,
                carouselViewItems,
            ).let { tallestCardHeight ->
                carouselView.recyclerView.apply {
                    minimumHeight = tallestCardHeight
                }
            }
    }

    interface CarouselItemsFetcher {
        fun getCarouselItems(
            context: Context,
            item: RecirculationItem,
            resultListener: ResultListener,
        )

        interface ResultListener {
            fun onSuccess(items: List<CarouselViewItem>)

            fun onError(e: Throwable)
        }
    }

    interface ClickListener {
        fun onClicked(
            positionInCarousel: Int,
            items: List<CarouselViewItem>?,
            recirculationType: RecirculationType,
        )
    }
}
