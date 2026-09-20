/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.WpTextFormatter
import com.wapo.text.applyUnderline
import com.wapo.view.CustomTypefaceSpan
import com.wapo.view.RippleHelper
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.StyleEntity
import java.lang.ref.WeakReference

class ArticleStyleCarouselViewHolder(
    itemView: View,
    val requestListener: CarouselProvider?,
    val clickListener: OnCarouselClickedListener?,
    private val setResizeCarouselView: () -> Unit,
    private val resizeCarouselView: () -> Unit,
    val cardWidth: Int
) : CarouselViewHolder(itemView) {

    private var imageViewRef: WeakReference<ImageView?>? = null


    override fun bind(item: CarouselViewItem) {
        val context = itemView.context
        val cardView = itemView.findViewById<CardView>(R.id.carousel_card_view)
        val imageView = itemView.findViewById<ImageView>(R.id.carousel_image)
        val sectionTopic = itemView.findViewById<TextView>(R.id.section_topic)
        val title = itemView.findViewById<TextView>(R.id.article_title)
        val byline = itemView.findViewById<TextView>(R.id.byline)

        imageViewRef = WeakReference(imageView)

        item.apply {
            cardView?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.story_carousel_background_color))

            val franklinItcBold = ResourcesCompat.getFont(context, R.font.franklinitcstd_bold)
            val franklinItcLight = ResourcesCompat.getFont(context, R.font.franklinitcstd_light)

            imageView?.apply {
                scaleType = if (shouldShowTitle) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_XY
                if(!TextUtils.isEmpty(imageUrl)) {
                    requestListener?.makeImageRequest(imageUrl, cardWidth, 0, this@ArticleStyleCarouselViewHolder)
                }
            }

            kicker?.let {
                sectionTopic?.apply {
                    var spannableKicker = SpannableStringBuilder(kicker)
                    if (style == StyleEntity.OPINIONS) {
                        spannableKicker.applyUnderline(
                            context,
                            0,
                            1,
                            com.wpds.wpds.R.color.opinion_spark,
                            resources.getInteger(com.wapo.view.R.integer.first_part_opinion_left_padding_underline)
                                .toFloat(),
                            resources.getInteger(com.wapo.view.R.integer.first_part_opinion_right_padding_underline)
                                .toFloat(),
                            -4f
                        )
                        spannableKicker.applyUnderline(
                            context,
                            2,
                            spannableKicker.length,
                            com.wpds.wpds.R.color.opinion_spark,
                            resources.getInteger(com.wapo.view.R.integer.second_part_opinion_left_padding_underline)
                                .toFloat(),
                            resources.getInteger(com.wapo.view.R.integer.second_part_opinion_right_padding_underline)
                                .toFloat(),
                            -4f
                        )
                    }
                    val resId = if (isLive == true) com.wpds.wpds.R.drawable.live_solid_red_circle else 0
                    sectionTopic.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
                    when {
                        !TextUtils.isEmpty(kicker) && isLive == true -> {
                            text = " $spannableKicker"
                            typeface = franklinItcBold
                        }

                        !TextUtils.isEmpty(kicker) -> {
                            if (style == StyleEntity.OPINIONS) {
                                val authorName = item.secondaryLabel ?: item.byline.removePrefix("By ")

                                spannableKicker = if (authorName.isNullOrBlank()) {
                                    SpannableStringBuilder("$spannableKicker")
                                } else {
                                    SpannableStringBuilder("$spannableKicker $authorName")
                                }
                                spannableKicker.setSpan(
                                    CustomTypefaceSpan("", franklinItcBold),
                                    0, kicker?.length ?: 0,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                spannableKicker.setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(context, com.wpds.wpds.R.color.gray80)),
                                    (kicker?.length ?: 0) + 1, spannableKicker.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                spannableKicker.applyUnderline(
                                    context, 0, 1, com.wpds.wpds.R.color.opinion_spark,
                                    resources.getInteger(com.wapo.view.R.integer.first_part_opinion_left_padding_underline)
                                        .toFloat(),
                                    resources.getInteger(com.wapo.view.R.integer.first_part_opinion_right_padding_underline)
                                        .toFloat(),
                                    -4f
                                )
                                kicker?.let {
                                    spannableKicker.applyUnderline(
                                        context, 2, it.length, com.wpds.wpds.R.color.opinion_spark,
                                        resources.getInteger(com.wapo.view.R.integer.second_part_opinion_left_padding_underline)
                                            .toFloat(),
                                        resources.getInteger(com.wapo.view.R.integer.second_part_opinion_right_padding_underline)
                                            .toFloat(),
                                        -4f
                                    )
                                }
                            } else {
                                spannableKicker = SpannableStringBuilder("$spannableKicker")
                                spannableKicker.setSpan(
                                    CustomTypefaceSpan("", franklinItcBold),
                                    0, kicker?.length ?: 0,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                            typeface = franklinItcLight
                            text = spannableKicker
                            visibility = View.VISIBLE
                        }

                        !TextUtils.isEmpty(kicker) && TextUtils.isEmpty(storyType) -> {
                            text = spannableKicker
                            typeface = franklinItcBold
                            visibility = View.VISIBLE
                        }

                        TextUtils.isEmpty(kicker) && !TextUtils.isEmpty(storyType) -> {
                            text = storyType
                            typeface = franklinItcLight
                            visibility = View.VISIBLE
                        }

                        else -> {
                            visibility = View.GONE
                        }
                    }
                }
            }

            title?.apply {
                if (!TextUtils.isEmpty(item.title)) {
                    WpTextFormatter.applyLineSpacing(this, R.style.carousel_item_headline_style)
                    text = headlinePrefixStyling(item)
                    typeface = franklinItcLight
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            byline?.apply {
                if (!TextUtils.isEmpty(item.byline)) {
                    text = item.byline
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            itemView.setOnClickListener { clickListener?.onCardClicked(contentUrl, adapterPosition) }

            RippleHelper.addRippleEffectToView(itemView)
        }
    }

    private fun headlinePrefixStyling(carouselViewItem: CarouselViewItem): CharSequence? {
        if (!TextUtils.isEmpty(carouselViewItem.title)) {
            if (!TextUtils.isEmpty(carouselViewItem.headlinePrefix)) {
                val spannableString =
                    SpannableString("${carouselViewItem.headlinePrefix} | ${carouselViewItem.title}")
                spannableString.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        R.style.carousel_item_headline_prefix_style
                    ),
                    0, (carouselViewItem.headlinePrefix?.length) ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableString.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        R.style.for_you_headline_prefix_separator_style
                    ),
                    (carouselViewItem.headlinePrefix?.length)?.plus(
                        1
                    ) ?: 0, (carouselViewItem.headlinePrefix?.length)?.plus(2) ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spannableString
            } else {
                return carouselViewItem.title
            }
        }
        return null
    }

    override fun onViewRecycled() {
        imageViewRef?.get()?.isVisible = true
        imageViewRef?.get()?.setImageDrawable(null)
    }

    override fun onBitmapLoaded(bitmap: Bitmap) {
        imageViewRef?.get()?.isVisible = true
        if (imageViewRef?.get() != null) {
            val animation = AlphaAnimation(0f, 1f)
            animation.duration = 1500
            imageViewRef?.get()?.setImageBitmap(bitmap)
            imageViewRef?.get()?.startAnimation(animation)
        }
    }

    override fun onLowDataModeChange(isEnable: Boolean) {
        if (isEnable) {
            imageViewRef?.get()?.isVisible = false
            setResizeCarouselView()
        } else {
            imageViewRef?.get()?.isVisible = true
            resizeCarouselView()
        }
    }

    override fun onBitmapError(bitmap: Bitmap?) {
        imageViewRef?.get()?.isVisible = true
        bitmap?.let { onBitmapLoaded(it) }
    }
}