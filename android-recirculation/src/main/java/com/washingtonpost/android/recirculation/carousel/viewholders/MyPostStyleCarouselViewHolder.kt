/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.wapo.android.commons.util.ContentType
import com.wapo.android.commons.util.isRecentMinutes
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.MyPostCarouselViewItem
import kotlin.math.ceil

class MyPostStyleCarouselViewHolder(
    itemView: View,
    val carouselItemWidth: Int,
    val shouldDisplayDateTime: Boolean,
    val clickListener: OnCarouselClickedListener?,
    val onAuthorClick: ((String) -> Unit)?,
    val onSaveClick: ((String) -> Unit)?,
    val onOptionsClick: ((String) -> Unit)?,
    private val onAudioIconClicked: ((MyPostCarouselViewItem) -> Unit)?,
) : CarouselViewHolder(itemView) {
    override fun bind(item: CarouselViewItem) {
        val myPostCarouselViewItem = item as MyPostCarouselViewItem
        val kickerView = itemView.findViewById<TextView>(R.id.section)
        val transparencyView = itemView.findViewById<TextView>(R.id.transparency)
        val headlineView = itemView.findViewById<TextView>(R.id.headline)
        val signatureView = itemView.findViewById<TextView>(R.id.signature)
        val saveView = itemView.findViewById<ImageButton>(R.id.ib_save)
        val dateTime = itemView.findViewById<TextView>(R.id.tv_date_time)
        val optionsView = itemView.findViewById<ImageButton>(R.id.ib_utility_menu)
        val audioImageButton = itemView.findViewById<ImageButton>(R.id.audioImageButton)

        if (carouselItemWidth > - 1) {
            itemView.layoutParams.width = carouselItemWidth
        }

        kickerView?.apply {
            if(!TextUtils.isEmpty(myPostCarouselViewItem.kicker)) {
                text = myPostCarouselViewItem.kicker
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        transparencyView?.apply {
            if(!TextUtils.isEmpty(myPostCarouselViewItem.transparency)) {
                text = myPostCarouselViewItem.transparency
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        headlineView?.apply {
            text = headlinePrefixStyling(myPostCarouselViewItem)
            visibility = if (!TextUtils.isEmpty(myPostCarouselViewItem.headline)){
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        signatureView?.apply {
            if (onAuthorClick != null && !TextUtils.isEmpty(myPostCarouselViewItem.byline)) {
                text = myPostCarouselViewItem.byline
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        dateTime?.apply {
            if (shouldDisplayDateTime && myPostCarouselViewItem.displayDateMillis != null
                && isRecentMinutes(
                    myPostCarouselViewItem.displayDateMillis,
                    myPostCarouselViewItem.recencyThresholdMinutes
                )
            ) {
                text = DateUtils.getRelativeTimeSpanString(
                    myPostCarouselViewItem.displayDateMillis,
                    System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
                )
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        if (onSaveClick != null) {
            saveView.setOnClickListener { onSaveClick.invoke(myPostCarouselViewItem.contentUrl) }
            saveView.visibility = View.VISIBLE
            audioImageButton.visibility = View.GONE
        } else {
            saveView.visibility = View.GONE
        }

        optionsView?.setOnClickListener { onOptionsClick?.invoke(myPostCarouselViewItem.contentUrl) }

        itemView.setOnClickListener {
            clickListener?.onCardClicked(myPostCarouselViewItem.contentUrl, adapterPosition)
        }

        audioImageButton.setOnClickListener {
            onAudioIconClicked?.invoke(myPostCarouselViewItem)
        }

        if (onAudioIconClicked != null) {
            myPostCarouselViewItem.percentConsumed?.let {
                if (it > 0.0f) {
                    signatureView.apply {
                        visibility = View.VISIBLE
                        text = itemView.resources.getString(R.string.pick_up_where_you_left_off_percentage_completed, (ceil(it * 100.0)).toInt())
                    }
                }
            } ?: {
                signatureView.apply {
                    val textToDisplay = if (myPostCarouselViewItem.contentType == ContentType.PODCAST) {
                        myPostCarouselViewItem.byline
                    } else {
                        if (onAuthorClick != null && myPostCarouselViewItem.byline.isNotEmpty()) {
                            myPostCarouselViewItem.byline
                        } else {
                            ""
                        }
                    }

                    signatureView.visibility = if (textToDisplay.isNotEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }

            if (myPostCarouselViewItem.contentType == ContentType.PODCAST || myPostCarouselViewItem.listenDepthSec != null) {
                audioImageButton.visibility = View.VISIBLE
            } else {
                audioImageButton.visibility = View.GONE
            }

            if (myPostCarouselViewItem.contentType == ContentType.PODCAST) {
                optionsView.visibility = View.GONE
            } else {
                optionsView.visibility = View.VISIBLE
            }
        }
    }

    override fun onViewRecycled() {
        //do nothing, no image to recycle
    }

    override fun onBitmapLoaded(bitmap: Bitmap) {
        //no image supported
    }

    override fun onBitmapError(bitmap: Bitmap?) {
    }

    override fun onLowDataModeChange(isEnable: Boolean) {
        //no image supported
    }

    private fun headlinePrefixStyling(myPostCarouselViewItem: MyPostCarouselViewItem): CharSequence? {
        if (!TextUtils.isEmpty(myPostCarouselViewItem.headline)) {
            if (!TextUtils.isEmpty(myPostCarouselViewItem.headlinePrefix)) {
                val spannableString =
                    SpannableString("${myPostCarouselViewItem.headlinePrefix} | ${myPostCarouselViewItem.headline}")
                spannableString.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        R.style.carousel_item_headline_prefix_style
                    ),
                    0, (myPostCarouselViewItem.headlinePrefix?.length) ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableString.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        R.style.for_you_headline_prefix_separator_style
                    ),
                    (myPostCarouselViewItem.headlinePrefix?.length)?.plus(
                        1
                    ) ?: 0, (myPostCarouselViewItem.headlinePrefix?.length)?.plus(2) ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spannableString
            } else {
                return myPostCarouselViewItem.headline
            }
        }
        return null
    }
}