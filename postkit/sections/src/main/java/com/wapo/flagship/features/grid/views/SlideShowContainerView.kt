/*
 * Copyright (C) 2021 Washington Post Android Application
 */
package com.wapo.flagship.features.grid.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import androidx.viewpager.widget.ViewPager
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.model.ArtOverlayIcon
import com.wapo.flagship.features.grid.model.ArtPosition
import com.wapo.flagship.features.grid.model.ArtWidth
import com.wapo.flagship.features.grid.model.SlideShow
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.sections.databinding.GridSlideshowImageBinding

class SlideShowContainerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ViewPager.OnPageChangeListener, View.OnClickListener {

    private var captions: List<String>? = arrayListOf()
    private var slideImages: SlideShow? = null
    var slideShowOverlayCallback: SlideShowOverlayCallback? = null
    private var adapter: SlideShowPagerAdapter? = null
    private var _binding: GridSlideshowImageBinding? = null
    private var savedPositions = mutableMapOf<String, Int>()
    private var prevOrientation: Int = -1
    private var currentOrientation: Int = -1
    private val trackForwardSwipe = "slideshow_forward"
    private val trackBackwardSwipe = "slideshow_back"
    private val trackLastImage = "slideshow_last_image"
    private var slideShowPos:Int =0


    init {
        val inflater = LayoutInflater.from(context)
        _binding = GridSlideshowImageBinding.inflate(inflater, this, true)
    }

    fun setSlideShowImage(slideShowImages: SlideShow, availableWidth: Int, artPosition: ArtPosition, artWidth: ArtWidth) {
        this.slideImages = slideShowImages
        setUpViews(availableWidth, artPosition, artWidth)
    }

    private fun setUpViews(availableWidth: Int, artPosition: ArtPosition, artWidth: ArtWidth) {
        handleRotation()
        resetViews()
        if (slideImages?.overlay != null && !slideImages?.overlay?.text.isNullOrEmpty()) {
            val overlay = slideImages?.overlay
            _binding?.slideShowOverlay?.visibility = View.VISIBLE
            _binding?.slideShowOverlay?.text = overlay?.text
            _binding?.slideShowOverlay?.setOnClickListener(this)
            if (overlay?.prefixIcon != null) {
                _binding?.slideShowOverlay?.setCompoundDrawablesWithIntrinsicBounds(
                    getIcon(overlay.prefixIcon),
                    null,
                    null,
                    null
                )
                _binding?.slideShowOverlay?.compoundDrawablePadding =
                    context.resources.getDimensionPixelSize(R.dimen.slide_show_overlay_prefix_icon_padding)
            }
            _binding?.slideShowImageBox?.post {
                _binding?.slideShowOverlay?.maxWidth =
                    (_binding?.slideShowImageBox?.measuredWidth as Int) - context.resources.getDimensionPixelSize(
                        R.dimen.grid_gutter_width
                    )
            }
        } else {
            _binding?.slideShowOverlay?.visibility = View.INVISIBLE
        }
        adapter = SlideShowPagerAdapter(slideImages, availableWidth, artPosition, artWidth)
        _binding?.slideShowImagePager?.offscreenPageLimit = slideImages?.images?.size!!
        _binding?.slideShowImagePager?.adapter = adapter
        _binding?.slideShowImagePager?.addOnPageChangeListener(this)

        _binding?.slideShowImageCarousel?.visibility = View.VISIBLE
        _binding?.slideShowImageCarousel?.setupWithViewPager(_binding?.slideShowImagePager)
        _binding?.slideShowImagePager?.post {
            getSavedPosition()
        }
    }

    private fun getIcon(artOverlayIcon: ArtOverlayIcon): Drawable? {
        return when (artOverlayIcon) {
            ArtOverlayIcon.ARROW -> {
                val arrow =
                    ContextCompat.getDrawable(context, R.drawable.ic_arrow_right_black)?.mutate()
                if (arrow != null) {
                    DrawableCompat.setTint(arrow, ContextCompat.getColor(context, com.wapo.flagship.features.audio.R.color.white))
                }
                arrow
            }
            ArtOverlayIcon.CAMERA -> {
                val camera = VectorDrawableCompat.create(
                    context.resources,
                    R.drawable.ic_label_camera,
                    context.theme
                )?.mutate()
                if (camera != null) {
                    DrawableCompat.setTint(camera, ContextCompat.getColor(context, com.wapo.flagship.features.audio.R.color.white))
                }
                camera
            }
            ArtOverlayIcon.PLAY -> ContextCompat.getDrawable(
                context, com.wpds.wpds.R.drawable.play
            )
            ArtOverlayIcon.MOBILE -> ContextCompat.getDrawable(
                context, R.drawable.stamp_icon
            )
            else -> {
                return null
            }
        }
    }

    private fun resetViews() {
        _binding?.slideShowImageCarousel?.visibility = GONE
        _binding?.slideShowImageBox?.setBackgroundColor(Color.TRANSPARENT)
        setBackgroundColor(Color.TRANSPARENT)
    }


    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        val slideShowType = if (slideShowPos > position) trackBackwardSwipe else trackForwardSwipe
        _binding?.slideShowImagePager?.reMeasureCurrentPage(_binding?.slideShowImagePager?.currentItem as Int)
        updateCaptionText(position)
        if (slideImages?.overlay != null) {
            if (position == 0) {
                _binding?.slideShowOverlay?.visibility = View.VISIBLE
            } else {
                _binding?.slideShowOverlay?.visibility = View.INVISIBLE
            }
        }
        slideShowPos = position + 1
        val navigationBehaviour =
            if (slideImages?.images?.size == slideShowPos) trackLastImage else slideShowType
        SectionTrackerFactory.get(context)
            .trackSlideShowSwipe(slideShowPos, navigationBehaviour)
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        val gridActivity = context.findActivityOfType<GridActivity>()
        if (gridActivity != null) {
            val pagerView = gridActivity.getGridEnvironment().getPager()
            pagerView?.setShouldAllowScroll(!disallowIntercept)
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    interface SlideShowOverlayCallback {
        fun onSlideShowOverlayClicked()
    }

    override fun onClick(v: View?) {
        slideShowOverlayCallback?.onSlideShowOverlayClicked()
        val contentUrl = slideImages?.link?.url
        SectionTrackerFactory.get(context).trackSlideShowOverlayClick(contentUrl)
    }

    /* Caption helper code */
    /** Assigns caption strings to the captions list.
     *  Sets TextView height to size of largest caption,
     *  then sets caption text to the first caption. */
    fun initializeCaptions(slideShow: SlideShow) {
        captions = slideShow.images?.map { it?.caption ?: "" }
        _binding?.slideShowCaption?.text = getLongestCaption()
        _binding?.slideShowCaption?.post {
            setCaptionViewHeight()
        }
    }

    fun updateCaptionText(position: Int) {
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 100
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation) {
                _binding?.slideShowCaption?.visibility = View.INVISIBLE
                _binding?.slideShowCaption?.text = captions?.get(position)
                fadeCaptionIn()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        _binding?.slideShowCaption?.startAnimation(fadeOut)
    }

    private fun fadeCaptionIn() {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 100
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                _binding?.slideShowCaption?.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        _binding?.slideShowCaption?.startAnimation(fadeIn)
    }

    private fun setCaptionViewHeight() {
        _binding?.slideShowCaption?.measuredHeight?.let {
            _binding?.slideShowCaption?.layoutParams?.height = it
        }
        _binding?.slideShowCaption?.text = captions?.get(0)
    }

    private fun getLongestCaption(): String? {
        return captions?.maxByOrNull { it.length }
    }

    fun getCaptionView(): TextView? {
        return _binding?.slideShowCaption
    }

    fun getImagePager(): SlideShowViewPager? {
        return _binding?.slideShowImagePager
    }

    fun getOverlayTextView(): TextView? {
        return _binding?.slideShowOverlay
    }

    /* Rotation helper code */
    /**
     * If the current orientation is not equal to the previous orientation,
     * save the current position in the slideshow so that it is not lost when the views are reset.
     */
    private fun handleRotation() {
        currentOrientation = Configuration(resources.configuration).orientation
        if (prevOrientation != -1 && currentOrientation != prevOrientation) {
            setSavedPosition()
        }
        prevOrientation = currentOrientation
    }

    /**
     * Using the slideshow's image URLs concatenated into a string as the key, store the current slideshow position in savedPositions.
     * */
    private fun setSavedPosition() {
        slideImages
        val imageUrls = slideImages?.images?.map { it?.url }?.joinToString()
        imageUrls?.let {
            savedPositions[imageUrls] = _binding?.slideShowImagePager?.currentItem ?: 0
        }
    }

    private fun getSavedPosition() {
        val imageUrls = slideImages?.images?.map { it?.url }?.joinToString()
        imageUrls?.let {
            _binding?.slideShowImagePager?.currentItem = savedPositions[imageUrls] ?: 0
        }
    }
}