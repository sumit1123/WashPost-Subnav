package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselRecipeViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.EllipsisActionItem
import java.lang.ref.WeakReference

class RecipeStyleCarouselViewHolder(
    itemView: View,
    val carouselProvider: CarouselProvider?,
    val clickListener: OnCarouselClickedListener?,
    private val cardWidth: Int,
    private val onBookmarkClick: ((EllipsisActionItem, ImageView, isStatusChecked: Boolean) -> Unit)?,
    private val setResizeCarouselView: () -> Unit,
    private val resizeCarouselView: () -> Unit
) : CarouselViewHolder(itemView) {
    private var imageViewRef: WeakReference<ImageView?>? = null
    private var cardView: MaterialCardView? = null
    private lateinit var artView: ImageView
    private lateinit var headlineView: TextView
    private lateinit var bookMark: ImageView
    private lateinit var ratingImage: ImageView
    private lateinit var ratingNumber: TextView
    private lateinit var subTitle: TextView

    override fun bind(item: CarouselViewItem) {
        val viewItem: CarouselRecipeViewItem = item as CarouselRecipeViewItem
        cardView = itemView.findViewById(R.id.recipe_item_card)
        bookMark = itemView.findViewById(R.id.recipe_bookmark_icon)
        artView = itemView.findViewById(R.id.recipe_item_art)
        ratingImage = itemView.findViewById(R.id.rating)
        ratingNumber = itemView.findViewById(R.id.rating_num)
        subTitle = itemView.findViewById(R.id.subtitle)
        imageViewRef = WeakReference(artView)
        cardView?.apply {
           setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        setupRating(viewItem)
        setupSubtitle(viewItem)
        bookMark.setOnClickListener {
            updateBookMarkStatus(viewItem, true)
        }
        updateBookMarkStatus(viewItem, false)

        if (viewItem.imageUrl != null) {
            carouselProvider?.makeImageRequest(viewItem.imageUrl, cardWidth, 0, this)
        } else {
            val placeHolderImage = BitmapFactory.decodeResource(
                itemView.context.resources,
                com.wapo.view.R.drawable.recipe_placeholder_item
            )
            onBitmapLoaded(placeHolderImage)
        }

        headlineView = itemView.findViewById(R.id.recipe_headline_title)
        headlineView.text = item.headline
        itemView.findViewById<MaterialCardView>(R.id.recipe_item_card)
            .setOnClickListener { clickListener?.onCardClicked(viewItem.contentUrl, position) }
    }

    private fun setupSubtitle(item: CarouselRecipeViewItem) {
        val duration = item.duration.takeIf { it != null && it > 0 }
            .let { AudioViewUtils.getDurationText(it?.toLong(), itemView.context) }
        val elements = listOfNotNull(duration, item.course).filter { it.isNotEmpty() }
        subTitle.text = elements.joinToString(separator = " | ")
    }

    private fun updateBookMarkStatus(viewItem: CarouselRecipeViewItem, isStatusChecked: Boolean) {
        onBookmarkClick.let { item ->
            if (item != null) {
                item(
                    EllipsisActionItem(
                        url = viewItem.articleUrl,
                        imageUrl = viewItem.imageUrl ?: "",
                        headline = viewItem.headline,
                    ), bookMark, isStatusChecked
                )
            }
        }
    }

    private fun setupRating(item: CarouselRecipeViewItem) {
        val rating = item.rating
        if (rating != null && rating > 0.0) {
            ratingImage.visibility = View.VISIBLE
            ratingNumber.visibility = View.VISIBLE
            ratingNumber.text = rating.toString()
            setupReviews(item)

        } else {
            ratingImage.visibility = View.INVISIBLE
            ratingNumber.visibility = View.INVISIBLE
        }
    }

    private fun setupReviews(item: CarouselRecipeViewItem) {
        val reviews = item.reviews
        if (reviews != null && reviews > 0) {
            val text = ratingNumber.text
            ratingNumber.text = "${text} (${item.reviews})"
        }
    }


    override fun onBitmapLoaded(bitmap: Bitmap) {
        if (imageViewRef?.get() != null) {
            val animation = AlphaAnimation(0f, 1f)
            animation.duration = 1500
            imageViewRef?.get()?.setImageBitmap(bitmap)
            imageViewRef?.get()?.startAnimation(animation)
            imageViewRef?.get()?.background = null
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
        imageViewRef?.get()?.setImageDrawable(null)
    }
}