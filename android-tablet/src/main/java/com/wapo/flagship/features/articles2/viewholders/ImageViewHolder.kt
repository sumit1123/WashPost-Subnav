package com.wapo.flagship.features.articles2.viewholders

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.glide.VolleyStreamFetcher
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemImageBinding

class ImageViewHolder(
    private val binding: ItemImageBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<Image>(
        binding.root,
        binding.placeholder,
    ) {
    override fun onBindItem(
        item: Image,
        position: Int,
    ) {
        binding.apply {
            if (item.fullCaption.isNullOrEmpty()) {
                articleMediaCaption.visibility = View.GONE
            } else {
                articleMediaCaption.text = item.fullCaption
                articleMediaCaption.visibility = View.VISIBLE

                val articleMargin = AppContextUtils.calculateArticleMargin()
                val layoutParams = articleMediaCaption.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.marginStart = articleMargin
                layoutParams?.marginEnd = articleMargin
                articleMediaCaption.layoutParams = layoutParams
            }
        }
    }

    override fun setPlaceHolderData(item: Image): PlaceHolderData {
        val resources = binding.root.context.resources
        return PlaceHolderData(
            message = resources.getString(R.string.low_data_mode_image_message),
            aspectRatio = getAspectRatio(item.imageWidth, item.imageHeight),
        ) {
            setImage(item)
        }
    }

    override fun onLowDataModeEnable(item: Image) {
        binding.apply {
            placeholder.isVisible = true
            imageView.isVisible = false
        }
    }

    override fun onLowDataModeDisable(item: Image) {
        setImage(item)
    }

    private fun setImage(item: Image) {
        binding.apply {
            val marginParams = articleMediaSlot.layoutParams as ViewGroup.MarginLayoutParams
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
            articleMediaSlot.layoutParams = marginParams
            setMediaSlotAspectRatio(item.imageWidth, item.imageHeight)
            val requestOption = RequestOptions().centerCrop()
            val glideUrl =
                GlideUrl(
                    item.imageURL,
                    LazyHeaders
                        .Builder()
                        .addHeader(
                            VolleyStreamFetcher.HEADER_SHOULD_BYPASS_CACHE,
                            if (item.isLive) "true" else "false",
                        ).build(),
                )

            Glide
                .with(imageView.context)
                .load(glideUrl)
                .listener(
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean,
                        ): Boolean = false

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean {
                            onItemReady(item)
                            imageView.isVisible = true
                            item.isItemAlreadyShowed = true
                            return false
                        }
                    },
                ).apply(requestOption)
                .into(imageView)

            imageView.setOnClickListener {
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
        binding.articleMediaSlot.aspectRatio = getAspectRatio(imageWidth, imageHeight)
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
