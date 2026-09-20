/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselBrightViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import java.lang.ref.WeakReference

class BrightStyleCarouselViewHolder(
    itemView: View,
    val requestListener: CarouselProvider?,
    val clickListener: OnCarouselClickedListener?,
    val cardWidth: Int,
    var isExcerptBlank : Boolean,
    private val setResizeCarouselView: () -> Unit,
    private val resizeCarouselView: () -> Unit
) : CarouselViewHolder(itemView) {

    private var imageViewRef: WeakReference<ImageView?>? = null

    override fun bind(item: CarouselViewItem) {
        val imageView = itemView.findViewById<ImageView>(R.id.bright_carousel_image)
        val textView = itemView.findViewById<TextView>(R.id.excerpt_text)
        imageViewRef = WeakReference(imageView)
        val brightViewItem: CarouselBrightViewItem = item as CarouselBrightViewItem
        if (isExcerptBlank) {
            textView.visibility = View.GONE
        }
        else
        {
            textView.text = brightViewItem.excerpt
            textView.visibility = View.VISIBLE
        }
        requestListener?.makeImageRequest(brightViewItem.brightUrl, cardWidth, 0, this)
        itemView.setOnClickListener { clickListener?.onCardClicked(brightViewItem.storyUrl, position) }
        imageView?.contentDescription = brightViewItem.brightDescription
    }
    override fun onViewRecycled() {
        imageViewRef?.get()?.setImageDrawable(null)
    }

    override fun onBitmapLoaded(bitmap: Bitmap) {
        if (imageViewRef?.get() != null) {
            val animation = AlphaAnimation(0f, 1f)
            animation.duration = 1500
            imageViewRef?.get()?.setImageBitmap(bitmap)
            imageViewRef?.get()?.startAnimation(animation)
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
}