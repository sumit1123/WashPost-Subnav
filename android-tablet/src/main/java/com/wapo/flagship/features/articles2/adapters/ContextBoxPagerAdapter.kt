package com.wapo.flagship.features.articles2.adapters

import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.ContentPagesItem
import com.wapo.flagship.features.articles2.models.deserialized.ContextBox
import com.wapo.flagship.features.articles2.models.deserialized.ContextBoxAlignment
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.models.deserialized.ListItem
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderView
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.flagship.features.articles2.viewholders.ContextBoxStyleHelper
import com.wapo.flagship.features.articles2.viewholders.ListViewHolder
import com.wapo.flagship.features.articles2.viewholders.SanitizedHtmlTextFormatter
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import com.wapo.flagship.features.sections.utils.UIUtils
import com.wapo.flagship.glide.VolleyStreamFetcher
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.selection.SelectableTextView
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ContextBoxPageBinding
import kotlinx.coroutines.selects.select

/**
 * Pager Adapter to create and manage various pages of context box.
 */
class ContextBoxPagerAdapter(
    val contentElements: List<ContentPagesItem>,
    val articlesInteractionHelper: ArticlesInteractionHelper,
    val styleHelper: ContextBoxStyleHelper,
    val isLowDataModeEnable: Boolean,
    val lowDataModeLive: LiveData<LowDataBanner>?,
) : RecyclerView.Adapter<ContextBoxPagerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ContextBoxPageBinding.inflate(inflater, parent, false)
        return ViewHolder(
            binding,
            articlesInteractionHelper,
            styleHelper,
            isLowDataModeEnable,
            lowDataModeLive,
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.onBind(contentElements[position])
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        holder.unbind()
    }

    override fun getItemCount(): Int = contentElements.size

    /**
     * View holder for each page in context box
     */
    class ViewHolder(
        val binding: ContextBoxPageBinding,
        val articlesInteractionHelper: ArticlesInteractionHelper,
        val styleHelper: ContextBoxStyleHelper,
        val isLowDataModeEnable: Boolean,
        val lowDataModeLive: LiveData<LowDataBanner>?,
    ) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<ContextBox>(binding.root) {
        var observer: Observer<LowDataBanner>? = null

        fun onBind(page: ContentPagesItem) {
            loadPage(page)
        }

        /**
         * Load various elements into context box
         * - SanitizedHtml
         * - ListItem
         * - Image
         */
        private fun loadPage(page: ContentPagesItem) {
            binding.articleMediaSlot.setVisible(false)
            binding.articleMediaCaption.setVisible(false)
            initTablet(page.contentElements?.any { it is Image } ?: false)
            page.contentElements?.forEachIndexed { index, item ->
                var isLastElement = index == page.contentElements.size-1
                item?.let {
                    when (it) {
                        is SanitizedHtml -> addText(it, isLastElement)
                        is ListItem -> addList(it, index)
                        is Image ->
                            addImage(
                                it,
                                page.alignment,
                                isLowDataModeEnable,
                                lowDataModeLive,
                            )
                        else -> {}
                    }
                }
            }
        }

        private fun initTablet(hasImage: Boolean) {
            if (DeviceUtils.isTablet(binding.root.context) && !hasImage) {
                binding.mainContainer.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                binding.mainContainer.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        /**
         * Add sanitizedHtml item with appropriate styling
         */
        private fun addText(text: SanitizedHtml, isLastIndex: Boolean) {
            val selectTextView = SelectableTextView(binding.root.context)
            selectTextView.movementMethod = LinkMovementMethod.getInstance()
            applyContextBoxFormatting(selectTextView, isLastIndex)
            val sanitizedHtmlTextFormatter =
                SanitizedHtmlTextFormatter(binding.root.context, articlesInteractionHelper)
            sanitizedHtmlTextFormatter.textStyleProducer = { styleHelper.contextBoxTextStyle }
            val formattedText = sanitizedHtmlTextFormatter.format(text)
            selectTextView.text = formattedText
            binding.mainContainer.addView(selectTextView)
        }

        private fun applyContextBoxFormatting(
            selectTextView: SelectableTextView,
            isLastIndex: Boolean
        ) {
            val isTablet = AppContextUtils.isTablet()
            val isCaptionGone = !binding.articleMediaCaption.isVisible
            val isImageGone = !binding.articleMediaSlot.isVisible
            var extraTopPadding = 0
            var extraBottomPadding = 0

            if (!isTablet && !isImageGone) {
                extraTopPadding = UIUtils.dpToPx(16f, binding.root.resources)
            }

            if (!isLastIndex) {
                extraBottomPadding = UIUtils.dpToPx(16f, binding.root.resources)
            }

            selectTextView.setPadding(
                0,
                extraTopPadding,
                0,
                extraBottomPadding
            )

            if (isCaptionGone && isImageGone) {
                val layoutParams = binding.mainContainer.layoutParams
                if (layoutParams is ConstraintLayout.LayoutParams) {
                    layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    binding.mainContainer.layoutParams = layoutParams
                }
                binding.mainContainer.setPadding(0, 0, 0, 0)
            }
        }

        /**
         * Add list Item with appropriate styling
         */
        private fun addList(
            list: ListItem,
            position: Int,
        ) {
            val selectTextView = SelectableTextView(binding.root.context)
            selectTextView.movementMethod = LinkMovementMethod.getInstance()
            val paddingLeft: Int = UIUtils.dpToPx(4f, binding.root.resources)
            val padding: Int = UIUtils.dpToPx(16f, binding.root.resources)
            selectTextView.setPadding(paddingLeft, 0, padding, padding)
            val builder = ListViewHolder.processListItems(list)
            builder.setSpan(
                WpTextAppearanceSpan(binding.root.context, styleHelper.contextBoxTextStyle),
                0,
                builder.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            val stringBuilderWithLinks =
                StylesHelper.makeLinkClickable(
                    builder,
                    binding.root.context,
                    articlesInteractionHelper,
                )
            selectTextView.setLineSpacing(
                StylesHelper.getTextSpacingExtra(binding.root.context),
                StylesHelper.getTextSpacingMult(binding.root.context),
            )
            selectTextView.text = stringBuilderWithLinks
            selectTextView.key = KeyHelper.createKey(position, stringBuilderWithLinks.toString())
            binding.mainContainer.addView(selectTextView)
        }

        /**
         * Add image
         */
        private fun addImage(
            item: Image,
            alignment: ContextBoxAlignment?,
            isLowDataModeEnable: Boolean,
            lowDataModeLive: LiveData<LowDataBanner>?,
        ) {
            observer =
                Observer { value ->
                    if (value.isLowDataBannerEnable && !item.isItemAlreadyShowed) {
                        setLowDataMode(binding, item)
                    } else {
                        setImage(binding, item)
                    }
                }

            binding.apply {
                root.findComponentActivity()?.let {
                    observer?.let { notNullObserver ->
                        lowDataModeLive?.observe(it, notNullObserver)
                    }
                }

                articleMediaSlot.setVisible(true)
                articleMediaCaption.setVisible(true)
                setTabletAlignment(alignment)
                setMediaSlotAspectRatio(item.imageWidth, item.imageHeight)

                if (isLowDataModeEnable && !item.isItemAlreadyShowed) {
                    item.placeHolderState =
                        MutableLiveData(
                            Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder.PlaceHolderState.PlaceHolderEnable,
                        )
                    setLowDataMode(this, item)
                } else {
                    setImage(this, item)
                }

                articleMediaCaption.text = item.fullCaption
                if (item.fullCaption.isNullOrBlank()) {
                    articleMediaCaption.setVisible(false)
                } else {
                    articleMediaCaption.setVisible(true)
                }
            }
        }

        private fun setLowDataMode(
            binding: ContextBoxPageBinding,
            item: Image,
        ) {
            binding.apply {
                imageView.isVisible = false
                placeholder?.isVisible = true

                val resources = root.context
                placeholder?.setContent {
                    PlaceHolderView(
                        isVisible = true,
                        data =
                            PlaceHolderData(
                                message = resources.getString(R.string.low_data_mode_image_message),
                            ) {
                                setImage(this, item)
                            },
                        state =
                            item.placeHolderState ?: MutableLiveData(
                                Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder.PlaceHolderState.PlaceHolderEnable,
                            ),
                    ) {
                        item.placeHolderState?.postValue(
                            Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder.PlaceHolderState.PlaceHolderLoading,
                        )
                        setImage(this, item)
                    }
                }
            }
        }

        private fun setImage(
            binding: ContextBoxPageBinding,
            item: Image,
        ) {
            binding.apply {
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
                        object :
                            RequestListener<Drawable> {
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
                                item.placeHolderState?.postValue(
                                    Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder.PlaceHolderState.PlaceHolderDisable,
                                )
                                item.isItemAlreadyShowed = true
                                imageView.isVisible = true
                                placeholder?.isVisible = false
                                return false
                            }
                        },
                    ).apply(requestOption)
                    .into(imageView)
            }
        }

        private fun setTabletAlignment(alignment: ContextBoxAlignment?) {
            if (DeviceUtils.isTablet(binding.root.context) && alignment == ContextBoxAlignment.RIGHT) {
                val image = binding.imageContainer
                val text = binding.mainContainer
                (binding.root as? ConstraintLayout)?.apply {
                    val imageSet = ConstraintSet()
                    imageSet.clone(this)
                    imageSet.clear(image.id, ConstraintSet.LEFT)
                    imageSet.connect(
                        image.id,
                        ConstraintSet.RIGHT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.RIGHT,
                    )
                    imageSet.applyTo(this)

                    val textSet = ConstraintSet()
                    textSet.clone(this)
                    textSet.clear(text.id, ConstraintSet.RIGHT)
                    textSet.connect(
                        text.id,
                        ConstraintSet.LEFT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.LEFT,
                    )
                    textSet.applyTo(this)
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

        override fun unbind() {
            observer = null
        }
    }
}
