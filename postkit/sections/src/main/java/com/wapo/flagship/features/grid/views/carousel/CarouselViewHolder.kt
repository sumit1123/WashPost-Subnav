/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views.carousel

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.model.Carousel
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselBrightViewItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.sections.R

class CarouselViewHolder(itemView: View, val requestListener: CarouselProvider, val parent: ViewGroup) : GridViewHolder(itemView) {

    private lateinit var carouselView: CarouselView


    override fun bind(position: Int, gridAdapter: GridAdapter) {
        val carouselModel = gridAdapter.items[position] as Carousel
        carouselView = itemView.findViewById(R.id.carousel_view)
        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {
                gridAdapter.onCarouselCardClicked?.invoke(carouselModel.items.map { it.link }, positionInCarousel)
            }

        }
        val cardWidth = itemView.resources.getDimensionPixelSize(R.dimen.carousel_bright_card_width)
        val cardPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_bright_card_padding)
        val arrowPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_arrow_padding)
        val isFullWidth = carouselModel?.resolvedColumnSpan == (parent as? WPGridView)?.getColumnCount()
        val isSmallerBreakPoint = (parent as? WPGridView)?.getColumnCount() == 1
        val sideMargin = if (isSmallerBreakPoint) 0 else if (isFullWidth) (parent as? WPGridView)?.getSideMargin() ?: 0 else arrowPadding
        val sidePadding = if (isSmallerBreakPoint) (parent as? WPGridView)?.getSideMargin() ?: 0 else 0

        carouselView.initLayout(requestListener, clickListener,
            object : CarouselView.CarouselConsumeTouchEventRule {
                override fun canCarouselConsumeTouchEvent(): Boolean {
                    //TODO refactor
                    // In phones true
                    // In tablets, only in preview mode
                    // But since, tablets only show the carousel in full screen now, I am going to pass true
                    // TODO revisit this logic in tablets
                    return true
                }
            },
            shouldDisplaySectionName = false,
            shouldDisplayArrow = !isSmallerBreakPoint,
            shouldDisplayDateTime = false,
            fixedCardWidth = cardWidth,
            fixedCardPadding = cardPadding,
            marginStart = sideMargin,
            marginEnd = sideMargin
        )
        carouselView.recyclerView?.apply {
            setPadding(sidePadding, paddingTop, sidePadding, paddingBottom)
        }

        val carouselViewItems = carouselModel.items.map { CarouselBrightViewItem(it.media.url!!, it.link.url, it.media.altText, it.excerpt?.text) }

        carouselView.carouselItemsFetchListener.onCarouselItemsFetched(carouselViewItems)

        carouselView.carouselItemsExcerptListener.onCarouselExcerptFetched(carouselViewItems)

        carouselView.recyclerView?.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                gridAdapter.environment.onCarouselScrollStateChanged(carouselModel, newState)
            }
        })
        carouselView.setOnCarouselItemTouchListener(object: OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                itemView.findActivityOfType<GridActivity>()?.let { activity ->
                    val pagerView = activity.getGridEnvironment().getPager()
                    pagerView?.setShouldAllowScroll(!disallowIntercept)
                }
            }
        })
    }
}