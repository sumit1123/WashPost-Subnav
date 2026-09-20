/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.animateSize
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.adapter.CarouselRecyclerViewAdapter
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselVideoViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import java.lang.ref.WeakReference

class VideoStyleCarouselViewHolder(
    itemView: View, val requestListener: CarouselProvider?,
    val clickListener: OnCarouselClickedListener?,
    private val cardWidth: Int,
    private val cardHeight: Int,
    private val setResizeCarouselView: () -> Unit,
    private val resizeCarouselView: () -> Unit
) : CarouselViewHolder(itemView), CarouselRecyclerViewAdapter.MediaViewHolder {

    private var imageViewRef: WeakReference<ImageView?>? = null
    private var container: FrameLayout? = null
    private var cardView: ViewGroup? = null
    private var mediaContainer: ViewGroup? = null
    private var imageContainer: ViewGroup? = null
    private var videoContainer: ViewGroup? = null
    private val extraHeight =
        itemView.resources.getDimension(R.dimen.carousel_video_focused_card_extra_height).toInt()
    private var aspectRatio: Float = cardHeight.toFloat() / cardWidth.toFloat()
    private var newHeight: Int = cardHeight
    private var newWidth: Int = cardWidth

    override fun bind(item: CarouselViewItem) {
        val viewItem: CarouselVideoViewItem = item as CarouselVideoViewItem
        cardView = itemView.findViewById(R.id.carousel_video_card_view)
        container = itemView.findViewById(R.id.video_card_main_container)
        mediaContainer = itemView.findViewById(R.id.carousel_media_container)
        imageContainer = itemView.findViewById(R.id.carousel_image_container)
        videoContainer = itemView.findViewById(R.id.carousel_video_container)
        val imageView = itemView.findViewById<ImageView>(R.id.carousel_video_image)
        // Set cardHeight to both mediaContainer and the image views.

        // Make Image request
        imageViewRef = WeakReference(imageView)
        requestListener?.makeImageRequest(viewItem.imageUrl, cardWidth, 0, this)
        // Set headline and duration
        val headline = itemView.findViewById<TextView>(R.id.player_headline)
        headline.text = viewItem.headline
        val duration = itemView.findViewById<TextView>(R.id.player_duration)
        duration.text = viewItem.duration
        imageView?.contentDescription = viewItem.altText

        // Handle Click listener
        val clickContainer = itemView.findViewById<View>(R.id.click_container)
        clickContainer.setOnClickListener {
            clickListener?.onCardClicked(
                viewItem.contentUrl,
                position
            )
        }

        val medContainerParam = mediaContainer?.layoutParams
        medContainerParam?.height = cardHeight
        mediaContainer?.layoutParams = medContainerParam

        newHeight = cardHeight + extraHeight
        newWidth = (newHeight / aspectRatio).toInt()

        val params = container?.layoutParams
        params?.height = newHeight
        container?.layoutParams = params
    }

    override fun onViewRecycled() {
        imageViewRef?.get()?.setImageDrawable(null)
        imageViewRef = null
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
        // No need to display bitmap in an error case.
        // ImageView has its own background for video items.
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

    override fun getVideoContainer(): ViewGroup? {
        return videoContainer
    }

    override fun prepare() {
        videoContainer?.visibility = View.VISIBLE
        imageContainer?.visibility = View.INVISIBLE
    }

    override fun focus(onComplete: () -> Unit) {
        if (!AppContextUtils.isTablet()) {
            mediaContainer?.apply {
                animateSize(
                    cardWidth.toFloat(),
                    newWidth.toFloat(),
                    cardHeight.toFloat(),
                    newHeight.toFloat()
                ) {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    override fun release() {
        // Release videoContainer's child views to release any views that are added by the
        // application view classes between prepare and release calls.
        videoContainer?.apply {
            removeAllViews()
            visibility = View.GONE
        }
        imageContainer?.visibility = View.VISIBLE
        if (!AppContextUtils.isTablet()) {
            mediaContainer?.apply {
                animateSize(
                    newWidth.toFloat(),
                    cardWidth.toFloat(),
                    newHeight.toFloat(),
                    cardHeight.toFloat()
                )
            }
        }
    }
}