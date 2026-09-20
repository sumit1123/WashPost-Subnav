/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views

import android.content.res.Resources
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import com.wapo.flagship.features.grid.model.ArtPosition
import com.wapo.flagship.features.grid.model.LiveImage
import com.wapo.flagship.features.grid.model.LiveImageType
import com.wapo.flagship.features.sections.utils.UIUtils
import com.washingtonpost.android.wapocontent.ILoader
import java.lang.IllegalStateException
import kotlin.math.roundToInt

class LiveImagePagerAdapter(private val liveImage: LiveImage, private val imageService: ILoader, private val refreshInterval: Long, private val availableWidth: Int, private val artPosition: ArtPosition, private val pageClickListener: View.OnClickListener, private val isNightMode: Boolean) : PagerAdapter() {


    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val context = container.context
        val layout = LinearLayout(context)
        layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val res: Resources = context.resources
        val paddingInPx = UIUtils.dpToPx(8f, res)
        val marginInPx = UIUtils.dpToPx(4f, res)
        layout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
        val images = liveImage.tabs[position].images
        for (i in 0 until images.size) {
            val liveImageInfo = images[i]
            val imageURL: String = if (isNightMode && liveImageInfo.darkModeUrl != null) liveImageInfo.darkModeUrl else liveImageInfo.url
            val imageView = LiveImageView(context)
            imageView.setImage(imageURL, imageService, refreshInterval)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//            // Add large margin on consecutive images
            lp.topMargin = marginInPx
            lp.bottomMargin = marginInPx
            imageView.layoutParams = lp
            // image adjust view bounds
            imageView.adjustViewBounds = true
            imageView.tag = imageURL
            imageView.contentDescription = liveImageInfo.alternateText
            layout.addView(imageView)
            if (ArtPosition.BELOW_HEADLINE == artPosition || ArtPosition.HIGH == artPosition || ArtPosition.LOW == artPosition) {
                imageView.minimumHeight = (availableWidth / liveImageInfo.aspectRatio).roundToInt()
            }
        }

        layout.setOnClickListener(pageClickListener)
        container.addView(layout, position)
        return layout
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View?)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (LiveImageType.CAROUSEL == liveImage.type || liveImage.tabs.size == 1) null else liveImage.tabs[position].text
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return liveImage.tabs?.count() ?: 0
    }

}