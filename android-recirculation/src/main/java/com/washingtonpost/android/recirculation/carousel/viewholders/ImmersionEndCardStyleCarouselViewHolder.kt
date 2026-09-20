/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselEndCardViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem

class ImmersionEndCardStyleCarouselViewHolder(
    itemView: View, val carouselProvider: CarouselProvider?,
    val clickListener: OnCarouselClickedListener?,
) : CarouselViewHolder(itemView) {
    private val res = itemView.context.resources
    private val elevation = res.getDimension(R.dimen.carousel_immersion_card_elevation)
    private val radius = res.getDimension(R.dimen.carousel_immersion_card_radius)

    override fun bind(item: CarouselViewItem) {
        val endCardViewItem = item as CarouselEndCardViewItem
        val cardView = itemView.findViewById<MaterialCardView>(R.id.immersion_item_end_card)
        val textView = itemView.findViewById<TextView>(R.id.text)
        cardify(item, cardView)
        textView.text = endCardViewItem.ctaText
        itemView.setOnClickListener {
            clickListener?.onCardClicked(endCardViewItem.url, position)
        }
    }

    override fun onViewRecycled() {
        // No op
    }

    override fun onBitmapLoaded(bitmap: Bitmap) {
        // No op
    }

    override fun onBitmapError(bitmap: Bitmap?) {
        // No op
    }

    override fun onLowDataModeChange(isEnable: Boolean) {
        // No op
    }

    private fun cardify(item: CarouselViewItem, cardView: MaterialCardView?) {
        cardView?.apply {
            elevation =
                if (item.cardify == false) 0f else this@ImmersionEndCardStyleCarouselViewHolder.elevation
            radius =
                if (item.cardify == false) 0f else this@ImmersionEndCardStyleCarouselViewHolder.radius
            val cardBackgroundColor = if (item.cardify == false)
                ContextCompat.getColor(context, R.color.immersion_carousel_no_cardify_color)
            else
                ContextCompat.getColor(context, R.color.immersion_carousel_card_background)
            setCardBackgroundColor(cardBackgroundColor)
        }
    }
}