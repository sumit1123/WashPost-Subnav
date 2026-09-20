package com.washingtonpost.android.recirculation.carousel.utils

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import com.wapo.text.TextMeasure
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.StyleEntity
import com.washingtonpost.android.recirculation.databinding.CarouselItemBinding

object ArticleStyleCarouselHelper {

    fun calculateTallestCardHeight(
        context: Context,
        carouselViewItems: List<CarouselViewItem>?
    ): Int {
        var tallestItemHeight = 0
        var tallestItem: CarouselViewItem? = null
        val res = context.resources
        val cardWidth =
            res.getDimensionPixelSize(R.dimen.carousel_article_style_card_width)
        val imageMargin = res.getDimensionPixelSize(R.dimen.carousel_image_top_margin)

        val marginStart = res.getDimensionPixelSize(R.dimen.carousel_title_left_margin)
        val imageWidth = res.getDimensionPixelSize(R.dimen.carousel_image_height)
        val titleTextWidth = cardWidth - imageWidth - marginStart

        carouselViewItems?.forEach { item ->
            val kicker = getKicker(item)
            val kickerTextHeight = if (kicker != null)
                TextMeasure().measureTextHeight(
                    kicker,
                    cardWidth,
                    context,
                    R.style.article_style_carousel_item_kicker
                ) else 0
            val headlineTextHeight = if (item.title.isNotEmpty())
                TextMeasure().measureTextHeight(
                    if (!item.headlinePrefix.isNullOrEmpty()) "${item.headlinePrefix} | ${item.title}" else item.title,
                    titleTextWidth,
                    context,
                    R.style.article_style_carousel_item_title
                ) else 0
            val itemHeight = kickerTextHeight + maxOf(headlineTextHeight, imageWidth) + imageMargin
            if (itemHeight > tallestItemHeight) {
                tallestItemHeight = itemHeight
                tallestItem = item
            }
        }

        // Update code here for any change in carousel_item.xml and in ArticleStyleCarouselViewHolder
        // to calculate item's height here correctly.
        tallestItem?.let {
            val dummyView: CarouselItemBinding =
                CarouselItemBinding.inflate(LayoutInflater.from(context))
            val kicker = getKicker(it)
            if (kicker?.isNotEmpty() == true) {
                dummyView.sectionTopic.visibility = View.VISIBLE
                dummyView.sectionTopic.text = kicker
            }
            if (it.title.isNotEmpty()) {
                dummyView.articleTitle.visibility = View.VISIBLE
                dummyView.articleTitle.text =
                    if (!it.headlinePrefix.isNullOrEmpty()) "${it.headlinePrefix} | ${it.title}" else it.title
            } else dummyView.articleTitle.visibility = View.GONE

            dummyView.root.measure(
                View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )

            // Moved adjustment to carouse_item.xml as layout_marginBottom to fix card shadow clipping.
            // Add any additional adjustment if needed here.
            val cardHeightAdjustment =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 0f, res.displayMetrics).toInt()

            return dummyView.root.measuredHeight - cardHeightAdjustment
        }

        return 0
    }

    private fun getKicker(it: CarouselViewItem): String? {
        val authorName = it.secondaryLabel ?: it.byline.removePrefix("By ")
        return when {
            !TextUtils.isEmpty(it.kicker) && it.isLive == true -> it.kicker
            !TextUtils.isEmpty(it.kicker) && it.style == StyleEntity.OPINIONS && !authorName.isNullOrEmpty() ->
                "${it.kicker} $authorName"
            !TextUtils.isEmpty(it.kicker) -> it.kicker
            !TextUtils.isEmpty(it.storyType) -> it.storyType
            else -> null
        }
    }
}