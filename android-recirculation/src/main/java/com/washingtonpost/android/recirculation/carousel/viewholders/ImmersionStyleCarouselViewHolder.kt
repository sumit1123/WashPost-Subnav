package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.WpTextFormatter
import com.wapo.text.applyUnderline
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselImmersionViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.StyleEntity
import java.lang.ref.WeakReference

class ImmersionStyleCarouselViewHolder(
    itemView: View,
    private val carouselProvider: CarouselProvider?,
    val clickListener: OnCarouselClickedListener?,
    private val cardWidth: Int,
    private val isNewsprint: Boolean = false,
    private val setResizeCarouselView: () -> Unit,
    private val resizeCarouselView: () -> Unit
) : CarouselViewHolder(itemView) {
    private var imageViewRef: WeakReference<ImageView?>? = null
    private var cardView: MaterialCardView? = null
    private var cardViewContent: ViewGroup? = null
    private lateinit var artView: ImageView
    private lateinit var headlineView: TextView
    private lateinit var kickerView: TextView
    private lateinit var bylineView: TextView
    private lateinit var labelContainer: LinearLayout
    private lateinit var secondaryLabelView: TextView
    private val res = itemView.context.resources
    private val artHeight = res.getDimensionPixelSize(R.dimen.carousel_immersion_card_image_height)

    override fun bind(item: CarouselViewItem) {
        val viewItem: CarouselImmersionViewItem = item as CarouselImmersionViewItem
        cardView = itemView.findViewById(R.id.immersion_item_card)
        cardViewContent = itemView.findViewById(R.id.immersion_item_content)
        artView = itemView.findViewById(R.id.item_immersion_art)
        imageViewRef = WeakReference(artView)
        headlineView = itemView.findViewById(R.id.immersion_headline_title)
        kickerView = itemView.findViewById(R.id.immersion_kicker)
        bylineView = itemView.findViewById(R.id.byline)
        secondaryLabelView = itemView.findViewById(R.id.secondary_label)
        labelContainer = itemView.findViewById(R.id.label_container)
        cardView?.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        styleArtView(item)
        updateArtView(item)

        //Headline prefix styling
        headlineView.apply {
            if (!TextUtils.isEmpty(item.title)) {
                if (isNewsprint) {
                    headlineView.setTextColor(Color.WHITE)
                }
                WpTextFormatter.applyLineSpacing(this, R.style.carousel_item_headline_style)
                text = if (!TextUtils.isEmpty(item.headlinePrefix)) {
                    val spannableString =
                        SpannableString("${item.headlinePrefix} ${item.headline}")
                    val spanStyle = if (isNewsprint) {
                        R.style.newsprint_carousel_item_headline_prefix_style
                    } else {
                        R.style.carousel_item_headline_prefix_style
                    }
                    spannableString.setSpan(
                        WpTextAppearanceSpan(
                            context,
                            spanStyle
                        ),
                        0, item.headlinePrefix?.length ?: 0,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableString.setSpan(
                        WpTextAppearanceSpan(
                            itemView.context,
                            R.style.for_you_headline_prefix_separator_style
                        ),
                        (item.headlinePrefix?.length)?.minus(
                            2
                        ) ?: 0, (item.headlinePrefix?.length) ?: 0,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableString
                } else {
                    item.headline
                }
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        bylineView.apply {
            if (item.byline.isNotEmpty()) {
                text = item.byline
                if (isNewsprint) {
                    bylineView.setTextColor(Color.WHITE)
                }
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        secondaryLabelView.apply {
            if (item.secondaryText?.isNotEmpty() == true) {
                text = item.secondaryText
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        if (item.kicker.isNullOrEmpty() && item.secondaryText.isNullOrEmpty()) {
            labelContainer.visibility = View.GONE
        } else {
            labelContainer.visibility = View.VISIBLE
        }
        kickerView.apply {
            if (item.kicker.isNullOrEmpty()) {
                visibility = View.GONE
            } else {
                if (item.kickerStyle == StyleEntity.OPINIONS) {
                    val labelSpan = SpannableStringBuilder(item.kicker)
                    labelSpan.applyUnderline(context, 0, 1, com.wpds.wpds.R.color.opinion_spark, resources.getInteger(
                        com.wapo.view.R.integer.first_part_opinion_left_padding_underline).toFloat(),  resources.getInteger(
                        com.wapo.view.R.integer.first_part_opinion_right_padding_underline).toFloat(), -4f)
                    labelSpan.applyUnderline(context,2, labelSpan.length, com.wpds.wpds.R.color.opinion_spark,   resources.getInteger(
                        com.wapo.view.R.integer.second_part_opinion_left_padding_underline).toFloat(),  resources.getInteger(
                        com.wapo.view.R.integer.second_part_opinion_right_padding_underline).toFloat(), -4f)
                    text = labelSpan
                } else {
                    text = item.kicker
                }
                if (isNewsprint) {
                    kickerView.setTextColor(Color.WHITE)
                }
                visibility = View.VISIBLE
            }
        }
        // Set bottom padding only when any text view is visible. No margin is required when there is only art.
        cardViewContent?.apply {
            val cardBottomPadding = if (headlineView.visibility == View.VISIBLE || bylineView.visibility == View.VISIBLE || kickerView.visibility == View.VISIBLE)
                res.getDimensionPixelSize(R.dimen.carousel_card_text_margin_medium)
            else 0
            setPadding(0, 0, 0, cardBottomPadding)
        }
        itemView.findViewById<MaterialCardView>(R.id.immersion_item_card)
            .setOnClickListener { clickListener?.onCardClicked(viewItem.contentUrl, position) }
    }

    override fun onBitmapLoaded(bitmap: Bitmap) {
        onBitmapLoaded(bitmap, true)
    }

    override fun onBitmapLoaded(bitmap: Bitmap, animate: Boolean) {
        if (animate) {
            if (imageViewRef?.get() != null) {
                val animation = AlphaAnimation(0f, 1f)
                animation.duration = 1500
                imageViewRef?.get()?.setImageBitmap(bitmap)
                imageViewRef?.get()?.startAnimation(animation)
                imageViewRef?.get()?.background = null
            }
        } else {
            if (imageViewRef?.get() != null) {
                imageViewRef?.get()?.setImageBitmap(bitmap)
                imageViewRef?.get()?.background = null
            }
        }
    }

    override fun onBitmapError(bitmap: Bitmap?) {
        bitmap?.let { onBitmapLoaded(it) }
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

    override fun onViewRecycled() {
        imageViewRef?.get()?.apply {
            setImageDrawable(null)
            setImageBitmap(null)
        }
    }

    private fun styleArtView(item: CarouselImmersionViewItem) {
        artView.apply {
            if (item.liveImage == false) {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams.height = artHeight
            } else {
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun updateArtView(item: CarouselImmersionViewItem) {
        artView.setImageBitmap(null)
        if (!item.imageUrl.isNullOrEmpty()) {
            artView.visibility = View.VISIBLE
            if (item.liveImage == true)
                carouselProvider?.makeLiveImageRequest(item.imageUrl, cardWidth, 0, this)
            else
                carouselProvider?.makeImageRequest(item.imageUrl, cardWidth, 0, this)
        } else {
            artView.visibility = View.GONE
        }
    }
}