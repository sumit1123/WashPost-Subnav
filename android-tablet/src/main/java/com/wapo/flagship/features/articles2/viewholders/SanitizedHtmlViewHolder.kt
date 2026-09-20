package com.wapo.flagship.features.articles2.viewholders

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.QuestionSet
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemSanitizedHtmlBinding

class SanitizedHtmlViewHolder(
    val binding: ItemSanitizedHtmlBinding,
    private val questionSet: List<QuestionSet>?,
    val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<SanitizedHtml>(binding.root) {
    var isSingleNumber = false
    var isBrief = false
    var isBriefExclusiveLabel = false
    var arcId: String? = null
    var isExpandedByline: Boolean = false

    override fun bind(
        item: SanitizedHtml,
        position: Int,
    ) {
        arcId = item.arcId
        binding.articleText.movementMethod = LinkMovementMethod.getInstance()
        val sanitizedHtmlTextFormatter =
            SanitizedHtmlTextFormatter(
                binding.root.context,
                articlesInteractionHelper,
            )
        isBrief = item.style == "briefs"
        isBriefExclusiveLabel = isBrief && item.subheadLevel == 3
        var formattedText = sanitizedHtmlTextFormatter.format(item, isBriefExclusiveLabel, questionSet)
        formattedText =
            applyTruncateLabelIfRequired(
                item,
                position,
                formattedText,
                sanitizedHtmlTextFormatter,
            )
        isSingleNumber = sanitizedHtmlTextFormatter.isSingleNumber(item, formattedText)
        setLeftDrawable(null)
        binding.articleText.background = null
        binding.articleText.gravity = Gravity.NO_GRAVITY

        if (isSingleNumber) {
            binding.articleText.setBackgroundResource(R.drawable.briefs_circle)
            binding.articleText.gravity = Gravity.CENTER
        } else if (isBriefExclusiveLabel) {
            setLeftDrawable(com.washingtonpost.android.sections.R.drawable.ic_wp_briefs_exclusive_label)
        }

        binding.articleText.setLineSpacing(
            StylesHelper.getTextSpacingExtra(binding.root.context),
            StylesHelper.getTextSpacingMult(binding.root.context),
        )

        binding.articleText.text = formattedText

        isExpandedByline = (item.subtype == SanitizedHtml.SubType.EXPANDED_BYLINE.value)
        binding.articleText.text = formattedText
        binding.articleText.key = KeyHelper.createKey(position, formattedText.toString())
    }

    private fun setLeftDrawable(id: Int? = null) {
        id?.let {
            binding.articleText.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(binding.root.context, it),
                null,
                null,
                null,
            )
            binding.articleText.compoundDrawablePadding =
                binding.root.context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.sections.R.dimen.compound_drawable_padding,
                )
        } ?: run {
            binding.articleText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            binding.articleText.compoundDrawablePadding = 0
        }
    }

    private fun applyTruncateLabelIfRequired(
        item: SanitizedHtml,
        position: Int,
        text: CharSequence,
        sanitizedHtmlTextFormatter: SanitizedHtmlTextFormatter,
    ): CharSequence {
        val formattedText = SpannableStringBuilder(text)
        val itemState = item.state
        if (itemState != null &&
            (
                (itemState.isTruncatedLabelItem(item) && itemState.isTruncated()) ||
                    (itemState.isExpandedLabelItem(item) && !itemState.isTruncated())
            )
        ) {
            val truncate = itemState.truncate
            val truncated = itemState.isTruncated()
            val truncateLabel =
                sanitizedHtmlTextFormatter.formatTruncate(
                    truncate,
                    truncated,
                    formattedText,
                )
            if (truncateLabel.isNotEmpty()) {
                val truncateSpannableLabel = SpannableStringBuilder()
                truncateSpannableLabel.append(truncateLabel)
                truncateSpannableLabel.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            articlesInteractionHelper.onEventFired(
                                ArticleInteractionEvent.ArticleTruncateExpandCollapse(item),
                            )
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.isUnderlineText = false
                        }
                    },
                    0,
                    truncateLabel.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                formattedText.append("   ")
                formattedText.append(truncateSpannableLabel)
            }
        }
        return formattedText
    }
}
