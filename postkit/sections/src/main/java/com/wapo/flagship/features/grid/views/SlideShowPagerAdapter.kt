/*
 * Copyright (C) 2021 Washington Post Android Application
 */

package com.wapo.flagship.features.grid.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.wapo.flagship.features.grid.ScalingStrategyType
import com.wapo.flagship.features.grid.model.ArtPosition
import com.wapo.flagship.features.grid.model.ArtWidth
import com.wapo.flagship.features.grid.model.SlideShow
import com.washingtonpost.android.sections.databinding.SlideShowImageBinding
import kotlin.math.roundToInt

class SlideShowPagerAdapter(private val slideImage: SlideShow?, private val availableWidth: Int, private val artPosition: ArtPosition, private val artWidth: ArtWidth) :
    PagerAdapter() {
    private val glideOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
    var binding: SlideShowImageBinding? = null
    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val context = container.context
        binding = SlideShowImageBinding.inflate(LayoutInflater.from(context), container, false)

        val images = slideImage?.images
        val slideShowImageInfo = images?.get(position)
        val imageURL: String? = slideShowImageInfo?.url
        var imageSlide = binding?.slideShowImage as ImageView
        var rootLayout = binding?.slideImageLayout as LinearLayout
        rootLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        //Scaling Strategy to set the slideshow image to FIT or Fill the View
        if (slideImage?.scalingStrategy == ScalingStrategyType.FILL) {
            imageSlide.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageSlide.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val height = if (artWidth == ArtWidth.FULL_WIDTH) {
            (availableWidth / slideImage?.aspectRatio as Float).roundToInt()
        } else {
            /** If [artWidth] is any other value, it only has half of the [availableWidth] to use */
            ((availableWidth / 2) / slideImage?.aspectRatio as Float).roundToInt()
        }
        imageSlide.layoutParams.height = height
        rootLayout.layoutParams.height = height

        Glide.with(context).applyDefaultRequestOptions(glideOptions)
            .load(imageURL)
            .into(imageSlide)
        container.addView(rootLayout, position)
        return rootLayout
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View?)
    }

   override fun getPageTitle(position: Int): CharSequence? {
        return null
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return slideImage?.images?.count() ?: 0
    }

}