package com.wapo.flagship.features.articles2.viewholders

import android.os.Build
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.deserialized.ByLine
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.articles2.utils.ByLineStyleHelper
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.text.GlobalFontAdjustmentSpan
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.WpTextFormatter
import com.wapo.text.WpTextUnderlineSpan
import com.washingtonpost.android.articles.R
import com.washingtonpost.android.databinding.ItemBylineBinding
import java.util.*

class ByLineViewHolder(
    private val binding: ItemBylineBinding,
    val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<ByLine>(binding.root, null) {
    private var isClickable: Boolean = false

    private var isLiveUpdate = false
    private var isLiveReporterInsight = false

    override fun onBindItem(
        item: ByLine,
        position: Int,
    ) {
        val context = itemView.context
        val byline = getByline(item)
        val byLineText = SpannableStringBuilder()
        var bylineStyle = ByLineStyleHelper.getByLineStyle(binding.root.context)
        isLiveUpdate = item.subtype == ByLine.SubType.LIVE_UPDATE.value ||
            item.subtype == ByLine.SubType.LIVE_REPORTER_INSIGHT.value
        isLiveReporterInsight = item.subtype == ByLine.SubType.LIVE_REPORTER_INSIGHT.value
        if (isLiveUpdate) {
            bylineStyle = ByLineStyleHelper.getByLineLiveUpdateStyle(binding.root.context)
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
        if (byLineText.isNotEmpty()) {
            byLineText.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                byLineText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            WpTextFormatter.applyLineSpacing(binding.content, bylineStyle)
            val isOpinion = item.subtype == ByLine.SubType.OPINION.value
            binding.content.text = makeAuthorsClickable(byLineText, item.authors, isOpinion)
            binding.content.movementMethod = LinkMovementMethod.getInstance()
            binding.content.visibility = View.VISIBLE
            binding.content.key = KeyHelper.createKey(position, byLineText.toString())
            if (isLiveReporterInsight) {
                if (TextUtils.isEmpty(item.authors?.first()?.expertise)) {
                    binding.content.setPadding(
                        0,
                        0,
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.byline_padding_bottom),
                    )
                    binding.subtext.setPadding(0, 0, 0, 0)
                } else {
                    binding.content.setPadding(
                        0,
                        0,
                        0,
                        context.resources.getDimensionPixelSize(
                            R.dimen.byline_content_padding_bottom,
                        ),
                    )
                    binding.subtext.setPadding(
                        0,
                        0,
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.byline_padding_bottom),
                    )
                }
            } else if (isLiveUpdate) {
                if (TextUtils.isEmpty(item.subtext)) {
                    binding.content.setPadding(
                        0,
                        0,
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.byline_padding_bottom),
                    )
                    binding.subtext.setPadding(0, 0, 0, 0)
                } else {
                    binding.content.setPadding(
                        0,
                        0,
                        0,
                        context.resources.getDimensionPixelSize(
                            R.dimen.byline_content_padding_bottom,
                        ),
                    )
                    binding.subtext.setPadding(
                        0,
                        0,
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.byline_padding_bottom),
                    )
                }
            } else {
                binding.content.setPadding(
                    0,
                    0,
                    0,
                    context.resources.getDimensionPixelSize(R.dimen.byline_content_padding_bottom),
                )
                binding.subtext.setPadding(0, 0, 0, 0)
            }
            bindSubtext(item)
            itemView.visibility = View.VISIBLE
        } else {
            itemView.visibility = View.GONE
        }
    }

    private fun getByline(bylineItem: ByLine?): Spanned? {
        var spannedString: Spanned? = null
        val allCaps =
            StylesHelper.isAllCaps(
                ByLineStyleHelper.getByLineStyle(binding.root.context),
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
     * Display subtext when available.
     */
    private fun bindSubtext(bylineItem: ByLine) {
        val subtext =
            if (isLiveReporterInsight) {
                bylineItem.authors?.first()?.expertise
            } else {
                bylineItem.subtext
            }
        if (!TextUtils.isEmpty(subtext)) {
            val subTextSpan = SpannableString(subtext)
            subTextSpan.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                subtext?.length ?: 0,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            if (isLiveReporterInsight) {
                subTextSpan.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        ByLineStyleHelper.getByLineSubtextLiveUpdateStyle(binding.root.context),
                    ),
                    0,
                    subtext?.length ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            } else {
                subTextSpan.setSpan(
                    WpTextAppearanceSpan(
                        itemView.context,
                        ByLineStyleHelper.getByLineStyle(binding.root.context),
                    ),
                    0,
                    subtext?.length ?: 0,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            WpTextFormatter.applyLineSpacing(
                binding.content,
                ByLineStyleHelper.getByLineStyle(binding.root.context),
            )
            binding.subtext.text = subTextSpan
            binding.subtext.visibility = View.VISIBLE
        } else {
            binding.subtext.visibility = View.GONE
        }
    }

    private fun makeAuthorsClickable(
        byLine: SpannableStringBuilder,
        authors: List<Author>?,
        isOpinion: Boolean,
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
                        byLine.setSpan(
                            WpTextAppearanceSpan(
                                itemView.context,
                                R.style.ArticleText_Byline_Author,
                            ),
                            start,
                            start + author.name.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        if (isClickable) {
                            val underlineColor = R.color.article_by_line
                            byLine.setSpan(
                                WpTextUnderlineSpan(itemView.context, underlineColor),
                                start,
                                start + author.name.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                            )
                        }
                    }
                }
            }
        }
        return byLine
    }

    override fun setPlaceHolderData(item: ByLine): PlaceHolderData? = null

    override fun onLowDataModeEnable(item: ByLine) {}

    override fun onLowDataModeDisable(item: ByLine) {}

    companion object {
        private const val NUM_IMAGES = 2
    }
}
