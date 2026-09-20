package com.wapo.flagship.features.articles2.viewholders

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.grid.views.LiveImageView
import com.wapo.flagship.features.nightmode.NightModeController
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.LufLiveImageBinding
import com.washingtonpost.android.wapocontent.ILoader

class ArticleLiveImageViewHolder(
    private val binding: LufLiveImageBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
    var imageLoader: ILoader?,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<Image>(
        binding.root,
        binding.placeholder,
    ) {
    private val DEFAULT_REFRESH_RATE: Long = 60 * 1000

    override fun onBindItem(
        item: Image,
        position: Int,
    ) {
        binding.articleMediaCaption.text = item.fullCaption
    }

    override fun setPlaceHolderData(item: Image): PlaceHolderData {
        val resources = binding.root.context.resources
        return PlaceHolderData(
            message = resources.getString(R.string.low_data_mode_live_image_message),
            aspectRatio = getAspectRatio(item.imageWidth, item.imageHeight),
        ) {
            setLiveImage(item)
        }
    }

    override fun onLowDataModeEnable(item: Image) {
        binding.apply {
            placeholder.isVisible = true
            lufLiveImage.isVisible = false
        }
    }

    override fun onLowDataModeDisable(item: Image) {
        setLiveImage(item)
    }

    private fun setLiveImage(item: Image) {
        binding.apply {
            val marginParams = binding.articleMediaSlot.layoutParams as ViewGroup.MarginLayoutParams
            marginParams.setMargins(0, 0, 0, 0)
            if (item.widthFactor == "default") {
                marginParams.setMargins(
                    binding.articleMediaSlot.context.resources.getDimensionPixelSize(
                        R.dimen.native_article_image_graphic_margin,
                    ),
                    0,
                    binding.articleMediaSlot.context.resources.getDimensionPixelSize(
                        R.dimen.native_article_image_graphic_margin,
                    ),
                    0,
                )
            }
            binding.articleMediaSlot.layoutParams = marginParams
            // Clear the previous image
            binding.lufLiveImage.setImageDrawable(null)
            /***
             * Supporting Night mode image urls for the Live images
             */
            val isNightMode = (binding.root.context.applicationContext as? NightModeController)?.isNightModeEnabled() ?: false
            val imagerUrl: String =
                if (isNightMode &&
                    !item.darkModeImageUrl.isNullOrEmpty()
                ) {
                    item.darkModeImageUrl
                } else {
                    item.imageURL ?: ""
                }
            imageLoader?.let {
                binding.lufLiveImage.setListener(
                    object : LiveImageView.Listener {
                        override fun onLiveImageReady() {
                            onItemReady(item)
                            lufLiveImage.isVisible = true
                        }
                    },
                )
                binding.lufLiveImage.setImage(
                    imagerUrl,
                    it,
                    DEFAULT_REFRESH_RATE,
                )
            }
            binding.lufLiveImage.setOnClickListener {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.ImageClickEvent(item.imageURL!!),
                )
            }
        }
    }

    private fun setMediaSlotAspectRatio(
        imageWidth: Int?,
        imageHeight: Int?,
    ) {
        val layoutParams: ConstraintLayout.LayoutParams = binding.lufLiveImage.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.dimensionRatio = getAspectRatio(imageWidth, imageHeight).toString()
        binding.lufLiveImage.layoutParams = layoutParams
    }

    private fun getAspectRatio(
        imageWidth: Int?,
        imageHeight: Int?,
    ): Float {
        var aspectRatio = 0f
        val imageW = imageWidth ?: 0
        val imageH = imageHeight ?: 0
        if (imageW > 0 && imageH > 0) {
            aspectRatio = imageW.toFloat() / imageH
        }
        return aspectRatio
    }
}
