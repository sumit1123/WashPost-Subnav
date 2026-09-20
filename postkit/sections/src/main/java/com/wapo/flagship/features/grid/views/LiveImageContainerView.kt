/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.model.ArtPosition
import com.wapo.flagship.features.grid.model.LiveImage
import com.wapo.flagship.features.grid.model.LiveImageType
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory
import com.wapo.view.SlidingTabLayout
import com.wapo.view.segmentedview.SegmentedView
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.wapocontent.LoaderProvider
import java.net.MalformedURLException
import java.net.URL


class LiveImageContainerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ViewPager.OnPageChangeListener, View.OnClickListener {

    private val TAG = LiveImageContainerView::class.simpleName
    private val liveImagePager: LiveImageViewPager
    private val liveImageBox: LinearLayout
    private var liveImageTabs: SlidingTabLayout? = null
    private val liveImageCta: TextView
    private var liveImageSegmentedView: SegmentedView? = null
    private var liveImageCarousel: TabLayout? = null
    private lateinit var liveImage: LiveImage
    private var adapter: LiveImagePagerAdapter? = null
    var liveImageCallback: LiveImageCallback? = null
    private var headline: String? = null
    private var isNightMode: Boolean = false //TODO remove nightmode boolean dependency when resources are in nightmode folders
    private val DEFAULT_REFRESH_RATE: Long = 60 * 1000

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.grid_live_image, this, true)
        liveImageBox = findViewById(R.id.live_image_box)
        liveImagePager = liveImageBox.findViewById(R.id.live_imager_pager)
        liveImageCta = liveImageBox.findViewById(R.id.live_image_cta)
    }

    fun setLiveImage(liveImage: LiveImage, availableWidth: Int, artPosition: ArtPosition, headline: String?, isNightMode: Boolean) {
        this.liveImage = liveImage
        this.headline = headline
        this.isNightMode = isNightMode
        setUpViews(availableWidth, artPosition)
    }

    private fun setUpViews(availableWidth: Int, artPosition: ArtPosition) {
        resetViews()
        liveImage.cta?.let {
            liveImageCta.text = liveImage.cta
            liveImageCta.visibility = View.VISIBLE
            liveImageCta.setOnClickListener(this)
        }
        val imageLoader = findActivityOfType<LoaderProvider>()?.loader
            ?: throw IllegalStateException("Activity with LoaderProvider is required")
        adapter = LiveImagePagerAdapter(liveImage, imageLoader, DEFAULT_REFRESH_RATE, availableWidth, artPosition, this, isNightMode)
        liveImagePager.offscreenPageLimit = liveImage.tabs.size
        liveImagePager.adapter = adapter
        liveImagePager.addOnPageChangeListener(this)

        when {
            liveImage.tabs.size > 1 -> {
                liveImageBox.background = ContextCompat.getDrawable(context, com.wapo.view.R.drawable.live_image_container_background)
                val arrow = ContextCompat.getDrawable(context, R.drawable.ic_arrow_right_black)?.mutate()
                arrow?.let {
                    DrawableCompat.setTint(it, ContextCompat.getColor(context, R.color.cell_homepagestory_live_image_cta))
                }
                liveImageCta.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null)

                //layout live image controls
                when(liveImage.type) {
                    LiveImageType.TAB -> {
                        liveImageTabs = (liveImageBox.findViewById(R.id.live_image_tabs) as? ViewStub)?.inflate() as? SlidingTabLayout
                        liveImageTabs?.visibility = View.VISIBLE
                        liveImageTabs?.setCustomTabColorizer { ContextCompat.getColor(context, R.color.articles_text_color) }
                        liveImageTabs?.setViewPager(liveImagePager)
                    }
                    LiveImageType.SEGMENT -> {
                        liveImageSegmentedView = (liveImageBox.findViewById(R.id.live_image_segmented_buttons) as? ViewStub)?.inflate() as? SegmentedView
                        liveImageSegmentedView?.visibility = View.VISIBLE
                        liveImageSegmentedView?.setupWithViewPager(liveImagePager)
                    }
                    LiveImageType.CAROUSEL -> {
                        liveImageCarousel = (liveImageBox.findViewById(R.id.live_image_carousel) as? ViewStub)?.inflate() as? TabLayout
                        liveImageCarousel?.visibility = View.VISIBLE
                        liveImageCarousel?.setupWithViewPager(liveImagePager)
                    }
                }
            }
            liveImage.tabs[0].images.size > 1 -> {
                liveImageBox.background = ContextCompat.getDrawable(context, com.wapo.view.R.drawable.live_image_container_background)
            }
            else -> {
                liveImageCta.apply {
                    if (isNightMode) {
                        val rightIconNightMode = ContextCompat.getDrawable(context, R.drawable.ic_arrow_right_black)?.mutate()
                        rightIconNightMode?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, rightIconNightMode, null)
                    }
                }
            }
        }
    }

    private fun resetViews() {
        liveImageCarousel?.visibility = GONE
        liveImageCarousel = null
        liveImageSegmentedView?.visibility = GONE
        liveImageSegmentedView = null
        liveImageTabs?.visibility = GONE
        liveImageTabs = null
        liveImageCta.visibility = View.GONE
        liveImageBox.setBackgroundColor(Color.TRANSPARENT)
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        liveImagePager.reMeasureCurrentPage(liveImagePager.currentItem)
        val liveImageTrackingName = getLiveImageTrackingName(position)
        SectionTrackerFactory.get(context).trackLiveImageToggle(liveImageTrackingName)
    }

    interface LiveImageCallback {
        fun onLiveImageClicked()
    }

    private fun getLiveImageTrackingName(currentSelectedPosition: Int): String? {
        val liveImagesNameBuilder: StringBuilder = StringBuilder()
        liveImagesNameBuilder.append(headline)
        val tab = liveImage.tabs[currentSelectedPosition]
        tab.images.forEach {
            try {
                val url = if (isNightMode && it.darkModeUrl != null) URL(it.darkModeUrl) else URL(it.url)
                liveImagesNameBuilder.append("::")
                liveImagesNameBuilder.append(url.file)
            } catch (e: MalformedURLException) {
            }
        }
        return liveImagesNameBuilder.toString()
    }

    override fun onClick(v: View?) {
        liveImageCallback?.onLiveImageClicked()
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        val gridActivity = context.findActivityOfType<GridActivity>()
        if (gridActivity != null) {
            val pagerView = gridActivity.getGridEnvironment().getPager()
            pagerView?.setShouldAllowScroll(!disallowIntercept)
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}

