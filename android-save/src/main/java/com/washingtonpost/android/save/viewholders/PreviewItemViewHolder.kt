package com.washingtonpost.android.save.viewholders

import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.format.DateUtils
import com.wapo.android.commons.util.*
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wapo.android.commons.util.isRecentMinutes
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.view.RippleHelper
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.MyPostCarouselViewItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.recirculation.databinding.MypostCarouselItemBinding
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.TIME_STAMP_RECENCY_THRESHOLD
import com.washingtonpost.android.save.databinding.MyPostSectionPreviewBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.MyPostArticleItem
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.models.PreviewItem.SectionPreviewItem
import com.washingtonpost.android.save.models.roundUpPercentageConsumed
import com.washingtonpost.android.save.types.MyPostSection
import java.sql.Date
import kotlin.math.max


abstract class PreviewItemViewHolder(
    open val binding: MyPostSectionPreviewBinding,
    private val onArticleItemClick: ((ArticleActionItem) -> Unit)?,
    private val onAuthorClick: ((ArticleActionItem) -> Unit)?,
    private val onSaveClick: ((ArticleActionItem) -> Unit)?,
    private val onOptionsClick: ((ArticleActionItem) -> Unit)?,
) : ItemViewHolder(binding.root) {

    var dummyView: MypostCarouselItemBinding? = null

    override fun bind(sectionPreviewItem: PreviewItem) {

        val liveEntry = sectionPreviewItem as? SectionPreviewItem ?: return

        updateFirstItem(liveEntry)

        val size = liveEntry.carouselList?.size ?: 0
        when (size) {
            in 2..size -> {
                // Show carousel when there are min 2 items for Carousel
                binding.carouselView.visibility = VISIBLE

                val cardWidth =
                    itemView.resources.getDimensionPixelSize(R.dimen.my_post_carousel_card_size)
                val cardMinHeight =
                    itemView.resources.getDimensionPixelSize(R.dimen.my_post_carousel_card_min_height)
                val cardPadding =
                    itemView.resources.getDimensionPixelSize(R.dimen.my_post_carousel_card_padding)

                val clickListener
                        : OnCarouselClickedListener = object : OnCarouselClickedListener {
                    override fun onCardClicked(url: String?, positionInCarousel: Int) {
                        if (!liveEntry.carouselList.isNullOrEmpty() && (liveEntry.carouselList[positionInCarousel] as MyPostCarouselViewItem).contentType == ContentType.PODCAST) {
                            val item = liveEntry.carouselList[positionInCarousel] as MyPostCarouselViewItem
                            val carouselToArticleItem = MyPostArticleItem(
                                mediaId = item.mediaId,
                                streamUrl = item.streamUrl,
                                contentType = ContentType.PODCAST,
                                listenDepthSec = item.listenDepthSec
                            )
                            onArticleItemClick?.invoke(
                                ArticleActionItem(
                                    liveEntry.myPostSection,
                                    "",
                                    itemClicked = carouselToArticleItem
                                )
                            )
                        } else {
                            url?.let {
                                onArticleItemClick?.invoke(
                                    ArticleActionItem(
                                        liveEntry.myPostSection,
                                        url
                                    )
                                )
                            }
                        }
                    }
                }

                binding.carouselView.initLayout(
                    null, clickListener,
                    object : CarouselView.CarouselConsumeTouchEventRule {
                        override fun canCarouselConsumeTouchEvent(): Boolean {
                            return false
                        }
                    }, false, false,
                    liveEntry.myPostSection == MyPostSection.SAVED_STORIES,
                    cardWidth, cardPadding,
                    if (liveEntry.myPostSection == MyPostSection.READING_HISTORY) {
                        {
                            onAuthorClick?.invoke(
                                ArticleActionItem(
                                    liveEntry.myPostSection,
                                    it
                                )
                            )
                        }
                    } else {
                        null
                    },
                    if (liveEntry.myPostSection == MyPostSection.SAVED_STORIES) {
                        {
                            onSaveClick?.invoke(ArticleActionItem(liveEntry.myPostSection, it))
                        }
                    } else {
                        null
                    },
                    {
                        onOptionsClick?.invoke(ArticleActionItem(liveEntry.myPostSection, it))
                    },
                    onAudioIconClicked = { item ->
                        if (item.contentType == ContentType.PODCAST) {
                            val carouselToArticleItem = MyPostArticleItem(
                                mediaId = item.mediaId,
                                streamUrl = item.streamUrl,
                                contentType = ContentType.PODCAST,
                                listenDepthSec = item.listenDepthSec
                            )
                            onArticleItemClick?.invoke(
                                ArticleActionItem(
                                    liveEntry.myPostSection,
                                    "",
                                    itemClicked = carouselToArticleItem
                                )
                            )
                        } else {
                            onArticleItemClick?.invoke(
                                ArticleActionItem(
                                    liveEntry.myPostSection,
                                    item.contentUrl,
                                    shouldPlayAudioArticle = true
                                )
                            )
                        }
                    },
                    animationDuration = 0
                )
                binding.carouselView.recyclerView.apply {
                    clipToPadding = false
                    val padding =
                        resources.getDimensionPixelSize(R.dimen.my_post_card_content_left_margin)
                    setPadding(padding, paddingTop, padding, paddingBottom)
                }

                binding.carouselView.recyclerView.apply {
                    minimumHeight = max(
                        cardMinHeight,
                        getPreferredItemMinHeight(liveEntry.myPostSection,
                                liveEntry.carouselList, cardWidth, minimumHeight)
                    )
                }

                binding.carouselView.carouselItemsFetchListener.onCarouselItemsFetched(
                    liveEntry.carouselList
                )
            }
            else -> {
                binding.carouselView.visibility = GONE
            }
        }
    }

    private fun getPreferredItemMinHeight(
        sec: MyPostSection,
        items: List<CarouselViewItem>?,
        cardWidth: Int,
        minimumHeight: Int,
    ): Int {
        items ?: return minimumHeight
        if (dummyView == null) {
            dummyView =
                MypostCarouselItemBinding.inflate(LayoutInflater.from(binding.root.context))
        }
        // find longest kicker, headline and byline to calculate item height
        var lk: String? = null
        var lh: String? = null
        var lb: String? = null
        var ldt: String? = null
        items.forEach {
            if (it is MyPostCarouselViewItem) {
                if (lk == null || (lk?.length?.compareTo(it.kicker?.length ?: 0) ?: 0) < 0) {
                    lk = it.kicker
                }
                var h = it.headline
                if (!it.headlinePrefix.isNullOrEmpty()) {
                    h = "${it.headlinePrefix} ${it.headline}"
                }
                if (lh == null || (lh?.length?.compareTo(h?.length ?: 0) ?: 0) < 0) {
                    lh = h
                }
                if (sec == MyPostSection.READING_HISTORY) {
                    if (lb == null || (lb?.length?.compareTo(it.byline.length) ?: 0) < 0) {
                        lb = it.byline
                    }
                } else {
                    if (ldt == null || isRecentMinutes(it.displayDateMillis, TIME_STAMP_RECENCY_THRESHOLD)) {
                        ldt = it.displayDateMillis.toString()
                    }
                }
            }
        }
        dummyView?.apply {
            if (!lk.isNullOrEmpty()) {
                section.text = lk
                section.visibility = VISIBLE
            } else {
                section.visibility = GONE
                transparency.visibility = GONE
            }
            headline.text = lh
            if (!lb.isNullOrEmpty()) {
                signature.text = lb
                signature.visibility = VISIBLE
            } else {
                signature.visibility = GONE
            }
            if (!ldt.isNullOrEmpty()) {
                tvDateTime.visibility = VISIBLE
            } else {
                tvDateTime.visibility = GONE
            }
            root.measure(
                View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
        }

        return dummyView?.root?.measuredHeight ?: 0
    }

    open fun updateFirstItem(liveEntry: SectionPreviewItem) {
        val firstItem = liveEntry.articleList?.firstOrNull()

        firstItem?.let {
            val requestOption = RequestOptions().centerCrop()
                .error(R.drawable.wp_placeholder_item)
            Glide.with(binding.previewHero.ivHero.context).load(it.imageUrl)
                .apply(requestOption).into(binding.previewHero.ivHero)

            if (it.contentType == ContentType.PODCAST || hasListenDepthSec(it)) {
                binding.previewHero.content.audioImageButton.visibility = VISIBLE

                binding.previewHero.content.audioImageButton.setOnClickListener { _ ->
                    val url = if (it.contentType == ContentType.PODCAST) {
                        it.streamUrl
                    } else {
                        it.contentUrl
                    }
                    onArticleItemClick?.invoke(ArticleActionItem(liveEntry.myPostSection, url = url ?: "",
                        itemClicked = firstItem, shouldPlayAudioArticle = true))
                }
            } else {
                binding.previewHero.content.audioImageButton.visibility = GONE
            }

            if (it.contentType == ContentType.PODCAST) {
                binding.previewHero.content.ibUtilityMenu.visibility = View.GONE
            } else {
                binding.previewHero.content.ibUtilityMenu.visibility = View.VISIBLE
            }

            firstItem.roundUpPercentageConsumed(binding.root.resources)?.let {
                binding.previewHero.content.percentConsumedTextView.visibility = VISIBLE
                binding.previewHero.content.percentConsumedTextView.text = it
            } ?: run {
                binding.previewHero.content.percentConsumedTextView.visibility = GONE
            }

            binding.previewHero.content.tvHeroSection.apply {
                if (!it.kicker.isNullOrEmpty()) {
                    text = it.kicker
                    visibility = VISIBLE
                } else {
                    visibility = GONE
                }
            }

            binding.previewHero.content.tvHeroTransparency.apply {
                if (!it.transparency.isNullOrEmpty()) {
                    text = it.transparency
                    visibility = VISIBLE
                } else {
                    visibility = GONE
                }
            }

            binding.previewHero.content.tvHeroHeadline.apply {
                text = headlinePrefixStyling(it)
                visibility = if (!TextUtils.isEmpty(it.headline)){
                    VISIBLE
                } else {
                    GONE
                }
            }

            binding.previewHero.content.tvHeroBlurb.apply {
                if (itemView.resources.getInteger(R.integer.my_post_blurb_visibility) == 0) {
                    if (!it.blurb.isNullOrEmpty()) {
                        text = it.blurb
                        visibility = VISIBLE
                    } else {
                        visibility = GONE
                    }
                } else {
                    visibility = GONE
                }
            }

            binding.previewHero.content.tvHeroSignature.apply {
                if (liveEntry.myPostSection == MyPostSection.READING_HISTORY
                ) {
                    if (it.contentType == ContentType.PODCAST) {
                        visibility = VISIBLE
                        text = it.label
                    } else {
                        if (!it.byline.isNullOrEmpty()) {
                            text = it.byline
                            visibility = VISIBLE
                        } else {
                            visibility = GONE
                        }
                    }
                } else {
                    visibility = GONE
                }
            }

            if (liveEntry.myPostSection == MyPostSection.SAVED_STORIES) {
                binding.previewHero.content.ibSave.visibility = VISIBLE
                binding.previewHero.content.ibSave.setOnClickListener { _ ->
                    onSaveClick?.invoke(ArticleActionItem(liveEntry.myPostSection, it.contentUrl))
                }
            } else {
                binding.previewHero.content.ibSave.visibility = GONE
            }

            binding.previewHero.content.tvDateTime.apply {
                if (liveEntry.myPostSection == MyPostSection.SAVED_STORIES) {
                    if (it.dateTime != null && isRecentMinutes(
                            it.dateTime, TIME_STAMP_RECENCY_THRESHOLD
                        )
                    ) {
                        text = DateUtils.getRelativeTimeSpanString(
                            it.dateTime,
                            System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
                        )
                        visibility = VISIBLE
                    } else {
                        visibility = GONE
                    }
                } else {
                    visibility = GONE
                }
            }

            binding.previewHero.content.tvHeroPurchaseDate.apply {
                if (liveEntry.myPostSection == MyPostSection.PURCHASE && it.dateTime != null) {
                    text = binding.previewHero.content.tvHeroPurchaseDate.context.getString(R.string.my_post_purchased_articles_date,
                        monthDayYearFormat(Date(it.dateTime))
                    )
                }else {
                    binding.previewHero.content.tvHeroPurchaseDate.visibility = GONE
                }
            }

            binding.previewHero.content.ibUtilityMenu.setOnClickListener { _ ->
                onOptionsClick?.invoke(ArticleActionItem(liveEntry.myPostSection, it.contentUrl))
            }

            binding.previewHero.layoutPreviewHero.setOnClickListener { _ ->
                onArticleItemClick?.invoke(
                    ArticleActionItem(
                        liveEntry.myPostSection,
                        it.contentUrl,
                        itemClicked = it,
                        isPreviewHeroArticle = true
                    )
                )
            }

            RippleHelper.addRippleEffectToView(binding.previewHero.root)
        }
    }

    private fun hasListenDepthSec(item: MyPostArticleItem): Boolean {
        item.listenDepthSec?.let {
            if (it > 0L) return true
        }
        return false
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
