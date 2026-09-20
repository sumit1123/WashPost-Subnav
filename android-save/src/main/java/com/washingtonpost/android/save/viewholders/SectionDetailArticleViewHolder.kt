package com.washingtonpost.android.save.viewholders

import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wapo.android.commons.util.ContentType
import com.wapo.android.commons.util.monthDayYearFormat
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.DetailItem
import com.washingtonpost.android.save.models.MyPostArticleItem
import com.washingtonpost.android.save.types.MyPostSection
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.RippleHelper
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.MyPostSectionDetailBinding
import com.washingtonpost.android.save.models.roundUpPercentageConsumed
import java.sql.Date

class SectionDetailArticleViewHolder(
    val binding: MyPostSectionDetailBinding,
    private val onArticleItemClick: ((ArticleActionItem) -> Unit)?,
    private val onSaveClick: ((ArticleActionItem) -> Unit)?,
    private val onOptionsClick: ((ArticleActionItem) -> Unit)?,
) :
    DetailsItemViewHolder(binding.root) {

    override fun bind(detailItem: DetailItem) {
        updateItem(detailItem as? DetailItem.Article)
    }

    private fun updateItem(item: DetailItem.Article?) {
        item?.articleItem?.let {
            val requestOption = RequestOptions().centerCrop()
                .error(R.drawable.wp_placeholder_item)
            Glide.with(binding.root).load(it.imageUrl)
                .apply(requestOption).into(binding.sectionDetailItem.ivHero)

            binding.sectionDetailItem.tvHeroSection.apply {
                if (!it.kicker.isNullOrEmpty()) {
                    text = it.kicker
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            binding.sectionDetailItem.tvHeroTransparency.apply {
                if (!it.transparency.isNullOrEmpty()) {
                    text = it.transparency
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            binding.sectionDetailItem.tvHeroHeadline.apply {
                text = headlinePrefixStyling(it)
                visibility = if (!TextUtils.isEmpty(it.headline)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            if (item.myPostSection == MyPostSection.READING_HISTORY) {

                binding.sectionDetailItem.audioImageButton.apply {
                    if ((item.articleItem.listenDepthSec != null && item.articleItem.listenDepthSec != 0L)
                        || item.articleItem.contentType == ContentType.PODCAST) {

                        visibility = View.VISIBLE
                        setOnClickListener {
                            val url = if (item.articleItem.contentType == ContentType.PODCAST) {
                                item.articleItem.streamUrl
                            } else {
                                item.articleItem.contentUrl
                            }
                            onArticleItemClick?.invoke(ArticleActionItem(item.myPostSection, url = url ?: "",
                                itemClicked = item.articleItem, shouldPlayAudioArticle = item.articleItem.contentType == ContentType.ARTICLE))
                        }
                    } else {
                        visibility = GONE
                    }
                }

                binding.sectionDetailItem.percentConsumedTextView.apply {
                    item.articleItem.roundUpPercentageConsumed(binding.root.resources)?.let { percentConsumed ->
                        visibility = VISIBLE
                        text = percentConsumed
                    } ?: run {
                        visibility = GONE
                    }
                }

                if (item.articleItem.contentType == ContentType.PODCAST) {
                    binding.sectionDetailItem.ibUtilityMenu.visibility = GONE
                    binding.sectionDetailItem.tvHeroSignature.visibility = VISIBLE
                    binding.sectionDetailItem.tvHeroSignature.text = item.articleItem.label
                } else {
                    binding.sectionDetailItem.ibUtilityMenu.visibility = VISIBLE

                    if (!it.byline.isNullOrEmpty()) {
                        binding.sectionDetailItem.tvHeroSignature.text = it.byline
                        binding.sectionDetailItem.tvHeroSignature.visibility = View.VISIBLE
                    } else {
                        binding.sectionDetailItem.tvHeroSignature.visibility = View.GONE
                    }
                }
            }

            if (item.myPostSection == MyPostSection.SAVED_STORIES) {
                binding.sectionDetailItem.ibSave.visibility = View.VISIBLE
                binding.sectionDetailItem.ibSave.setOnClickListener { _ ->
                    onSaveClick?.invoke(ArticleActionItem(item.myPostSection, it.contentUrl))
                }
            } else {
                binding.sectionDetailItem.ibSave.visibility = View.GONE
            }

            binding.sectionDetailItem.tvDateTime.apply {
                if (item.myPostSection == MyPostSection.SAVED_STORIES) {
                    if (it.dateTime != null) {
                        text = DateUtils.getRelativeTimeSpanString(
                            it.dateTime,
                            System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
                        )
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                    }
                } else {
                    visibility = View.GONE
                }
            }

            binding.sectionDetailItem.ibUtilityMenu.setOnClickListener { _ ->
                onOptionsClick?.invoke(ArticleActionItem(item.myPostSection, it.contentUrl))
            }

            binding.sectionDetailItem.layoutPreviewHero.setOnClickListener { _ ->
                if (it.contentType == ContentType.ARTICLE) {
                    if (item.myPostSection == MyPostSection.PURCHASE){
                        onArticleItemClick?.invoke(ArticleActionItem(item.myPostSection, it.contentUrl))
                    } else {
                        onArticleItemClick?.invoke(ArticleActionItem(item.myPostSection, it.contentUrl, isPreviewHeroArticle = true))
                    }
                } else if (it.contentType == ContentType.PODCAST && !it.streamUrl.isNullOrEmpty()) {
                    onArticleItemClick?.invoke(ArticleActionItem(item.myPostSection, url = it.streamUrl!!, itemClicked = it))
                }
            }

            binding.sectionDetailItem.tvHeroPurchaseDate.apply {
                if (item.myPostSection == MyPostSection.PURCHASE && it.dateTime != null) {
                    text = this.context.getString(
                        R.string.my_post_purchased_articles_date,
                        monthDayYearFormat(Date(it.dateTime))
                    )
                } else {
                    binding.sectionDetailItem.tvHeroPurchaseDate.visibility = GONE
                }
            }

            RippleHelper.addRippleEffectToView(binding.sectionDetailItem.root)
        }
    }

    private fun headlinePrefixStyling(myPostArticleItem: MyPostArticleItem): CharSequence? {
        if (!TextUtils.isEmpty(myPostArticleItem.headline)) {
            if (!TextUtils.isEmpty(myPostArticleItem.headlinePrefix)) {
                val spannableString =
                    SpannableString("${myPostArticleItem.headlinePrefix} | ${myPostArticleItem.headline}")
                spannableString.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        com.washingtonpost.android.recirculation.R.style.carousel_item_headline_prefix_style
                    ),
                    0, (myPostArticleItem.headlinePrefix?.length) ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableString.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        com.washingtonpost.android.recirculation.R.style.for_you_headline_prefix_separator_style
                    ),
                    (myPostArticleItem.headlinePrefix?.length)?.plus(
                        1
                    ) ?: 0, (myPostArticleItem.headlinePrefix?.length)?.plus(2) ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spannableString
            } else {
                return myPostArticleItem.headline
            }
        }
        return null
    }

}