package com.wapo.flagship.features.articles2.viewholders

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.deserialized.ByLine
import com.wapo.flagship.features.articles2.models.deserialized.ElevatedByline
import com.wapo.flagship.features.articles2.models.deserialized.Style
import com.wapo.flagship.features.articles2.utils.ByLineStyleHelper
import com.wapo.flagship.features.articles2.utils.KickerStyleHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.text.GlobalFontAdjustmentSpan
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.WpTextFormatter
import com.wapo.text.applyUnderline
import com.washingtonpost.android.articles.R
import com.washingtonpost.android.databinding.ItemElevatedBylineBinding
import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView
import java.util.Locale

class ElevatedBylineViewHolder(
    private val binding: ItemElevatedBylineBinding,
    val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<ElevatedByline>(binding.root) {
    private var isClickable: Boolean = false

    private val images: List<NetworkAnimatedImageView> =
        listOf(binding.firstImage, binding.secondImage)

    override fun bind(
        item: ElevatedByline,
        position: Int,
    ) {
        val context = itemView.context
        val byline = getByline(item.byLine)
        val byLineText = SpannableStringBuilder()
        val bylineStyle = ByLineStyleHelper.getElevatedByLineStyle(binding.root.context)
        val kickerText = SpannableStringBuilder(item.kicker?.displayLabel.orEmpty())
        var kickerStyle =
            if (Style.getValue(item.kicker?.style) == Style.OPINIONS) {
                KickerStyleHelper.getTextKickerElevatedStyle(
                    binding.root.context,
                )
            } else {
                KickerStyleHelper.getTextKickerDefaultStyle(binding.root.context)
            }

        if (!TextUtils.isEmpty(byline)) {
            byLineText.append(byline)
            byLineText.setSpan(
                WpTextAppearanceSpan(context, bylineStyle),
                0,
                byLineText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (kickerText.isNotEmpty()) {
            kickerText.let {
                kickerText.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        kickerStyle,
                    ),
                    0,
                    it.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

            if (Style.getValue(item.kicker?.style) == Style.OPINIONS) {
                kickerText.applyUnderline(
                    context,
                    0,
                    1,
                    com.wpds.wpds.R.color.opinion_spark,
                    context.resources
                        .getInteger(
                            com.wapo.view.R.integer.first_part_opinion_left_padding_underline,
                        ).toFloat(),
                    context.resources
                        .getInteger(
                            com.wapo.view.R.integer.first_part_opinion_right_padding_underline,
                        ).toFloat(),
                    -4f,
                )
                kickerText.applyUnderline(
                    context,
                    2,
                    kickerText.length,
                    com.wpds.wpds.R.color.opinion_spark,
                    context.resources
                        .getInteger(
                            com.wapo.view.R.integer.second_part_opinion_left_padding_underline,
                        ).toFloat(),
                    context.resources
                        .getInteger(
                            com.wapo.view.R.integer.second_part_opinion_right_padding_underline,
                        ).toFloat(),
                    -4f,
                )
            }
            binding.kicker.text = kickerText
            binding.kicker.visibility = View.VISIBLE
            binding.kicker.setPadding(
                0,
                0,
                0,
                0,
            )
            binding.byline.setPadding(0, 0, 0, 0)

            item.byLine?.let { bindImages(it) }
            item.byLine?.let { bindByline(it) }
            itemView.visibility = View.VISIBLE
            if (item.byLine?.content.isNullOrEmpty()) {
                binding.byline.visibility = View.GONE
            } else {
                binding.byline.visibility = View.VISIBLE
            }

            // Added deeplink for kicker
            binding.kicker.setOnClickListener {
                DeepLinksProcessor.processAsync(
                    item.kicker?.path?.let { it1 ->
                        DeepLinksProcessor.sectionPathToDeepLink(
                            it1,
                        )
                    },
                    scope = binding.kicker.findViewTreeLifecycleOwner()?.lifecycleScope,
                )
            }
        } else {
            itemView.visibility = View.GONE
        }
    }

    private fun getByline(bylineItem: ByLine?): Spanned? {
        var spannedString: Spanned? = null
        val allCaps =
            StylesHelper.isAllCaps(
                ByLineStyleHelper.getElevatedByLineStyle(binding.root.context),
                binding.root.context,
            )
        var content = bylineItem?.content
        if (!TextUtils.isEmpty(content)) {
            if (content != null && allCaps) {
                content = content.uppercase(Locale.getDefault())
            }
            spannedString =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(content)
                }
        }
        return spannedString
    }

    /**
     * Displays n author images when available for opinions articles
     */
    private fun bindImages(bylineItem: ByLine) {
        val imageUrls =
            mutableListOf<String>().apply {
                bylineItem.authors?.forEach {
                    if (it.image != null && URLUtil.isNetworkUrl(it.image)) {
                        add(it.image)
                    }
                }
            }
        for (i in 0 until NUM_IMAGES) {
            images[i].apply {
                val url = imageUrls.getOrNull(i)
                setPlaceholder(com.washingtonpost.android.follow.R.drawable.author_placeholder)
                visibility =
                    when (url) {
                        null -> View.GONE
                        else -> View.VISIBLE
                    }
                strokeColor =
                    when (bylineItem.subtype) {
                        ByLine.SubType.OPINION.value ->
                            ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    binding.root.context,
                                    com.wapo.flagship.features.audio.R.color.byline_opinion,
                                ),
                            )
                        else -> ColorStateList.valueOf(Color.TRANSPARENT)
                    }
                setImageUrl(url, FlagshipApplication.getInstance().animatedImageLoader)
            }
        }
        val leftPadding =
            if (imageUrls.isNotEmpty()) {
                itemView.resources.getDimension(R.dimen.byline_padding_start).toInt()
            } else {
                itemView.resources.getDimension(R.dimen.byline_padding_no_start).toInt()
            }
        binding.textContainer.setPadding(leftPadding, 0, 0, 0)
    }

    /**
     * Display byline when available.
     */
    private fun bindByline(bylineItem: ByLine) {
        val author = bylineItem.content
        if (!TextUtils.isEmpty(author)) {
            val subTextSpan = SpannableStringBuilder(author)
            subTextSpan.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                author?.length ?: 0,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )

            subTextSpan.setSpan(
                WpTextAppearanceSpan(
                    itemView.context,
                    ByLineStyleHelper.getElevatedByLineStyle(binding.root.context),
                ),
                0,
                author?.length ?: 0,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )

            WpTextFormatter.applyLineSpacing(
                binding.kicker,
                ByLineStyleHelper.getElevatedByLineStyle(binding.root.context),
            )
            binding.byline.movementMethod = LinkMovementMethod.getInstance()
            binding.byline.text = makeAuthorsClickable(subTextSpan, bylineItem.authors)
            binding.byline.visibility = View.VISIBLE
        } else {
            binding.byline.visibility = View.GONE
        }
    }

    private fun makeAuthorsClickable(
        byLine: SpannableStringBuilder,
        authors: List<Author>?,
    ): Spanned? {
        if (authors != null) {
            for (author in authors) {
                val start = author.name?.let { byLine.indexOf(it) }
                if (start != null) {
                    if (start > -1) {
                        if (author.id != null) {
                            isClickable = true
                            byLine.setSpan(
                                object : ClickableSpan() {
                                    override fun onClick(widget: View) {
//                                        articlesInteractionHelper.onEventFired(
//                                            ArticleInteractionEvent.AuthorNameClickEvent(
//                                                author,
//                                            ),
//                                        )
                                    }

                                    override fun updateDrawState(ds: TextPaint) {
                                        ds.isUnderlineText = false
                                    }
                                },
                                start,
                                start + author.name.length,
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                            )
                        }
                        byLine.replace(
                            start,
                            start + author.name.length,
                            author.name.replace(" ", "\u202F"),
                        )
                    }
                }
            }
        }
        return byLine
    }

    companion object {
        private const val NUM_IMAGES = 2
    }
}
