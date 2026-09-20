package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselSevenLiveViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.wapo.text.WpTextAppearanceSpan
import android.text.SpannableString
import android.text.TextUtils
import android.text.Spanned
import androidx.core.content.ContextCompat
import com.wapo.text.WpTextFormatter
import java.lang.ref.WeakReference

class SevenLiveItemCarouselViewHolder(
    itemView: View,
    private val carouselProvider: CarouselProvider?,
    val clickListener: OnCarouselClickedListener?,
    private val cardWidth: Int,
    private val isNewsprint: Boolean = false,
    private val setResizeCarouselView: () -> Unit,
    private val resizeCarouselView: () -> Unit,
) : CarouselViewHolder(itemView) {

    private var imageViewRef: WeakReference<ImageView?>? = null
    private var cardView: MaterialCardView? = null
    private var cardViewContent: ViewGroup? = null
    private lateinit var timestampView: TextView
    private lateinit var headlineView: TextView
    private lateinit var secondaryLabelView: TextView
    private lateinit var artView: ImageView
    private val res = itemView.context.resources
    private val artHeight = res.getDimensionPixelSize(R.dimen.carousel_external_card_image_size)

    override fun bind(item: CarouselViewItem) {
        val viewItem: CarouselSevenLiveViewItem = item as CarouselSevenLiveViewItem
        cardView = itemView.findViewById(R.id.seven_live_item_card)
        cardViewContent = itemView.findViewById(R.id.seven_live_item_content)
        artView = itemView.findViewById(R.id.item_seven_live_art)
        imageViewRef = WeakReference(artView)

        timestampView = itemView.findViewById(R.id.seven_live_timestamp)
        headlineView = itemView.findViewById(R.id.seven_live_headline_title)
        secondaryLabelView = itemView.findViewById(R.id.secondary_label)
        cardView?.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        styleArtView(viewItem)
        updateArtView(viewItem)

        val timestamp = viewItem.relativeTime
        if (!timestamp.isNullOrEmpty()) {
            timestampView.text = timestamp
            timestampView.visibility = View.VISIBLE

            val colorRes = if (viewItem.isRecent) {
                com.wpds.wpds.R.color.live_update_text_color
            } else {
                com.wpds.wpds.R.color.live_update_non_recent_text_color
            }
            timestampView.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
        } else {
            timestampView.visibility = View.GONE
        }

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

        secondaryLabelView.apply {
            if (item.secondaryText?.isNotEmpty() == true) {
                text = item.secondaryText
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        cardViewContent?.apply {
            val cardBottomPadding =
                if (headlineView.isVisible || secondaryLabelView.isVisible) {
                    res.getDimensionPixelSize(R.dimen.carousel_card_text_margin_medium)
                } else {
                    0
                }
            setPadding(0, 0, 0, cardBottomPadding)
        }

        itemView.findViewById<MaterialCardView>(R.id.seven_live_item_card)
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

    override fun onBitmapError(bitmap: Bitmap?) { bitmap?.let { onBitmapLoaded(it) } }

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

    private fun styleArtView(item: CarouselSevenLiveViewItem) {
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

    private fun updateArtView(item: CarouselSevenLiveViewItem) {
        artView.setImageBitmap(null)
        if (!item.imageUrl.isNullOrEmpty()) {
            artView.visibility = View.VISIBLE
            if (item.liveImage == true) {
                carouselProvider?.makeLiveImageRequest(item.imageUrl, cardWidth, 0, this)
            } else {
                carouselProvider?.makeImageRequest(item.imageUrl, cardWidth, 0, this)
            }
        } else {
            artView.visibility = View.GONE
        }
    }
}