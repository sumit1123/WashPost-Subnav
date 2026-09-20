package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.graphics.Paint
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.LineHeightSpan
import android.text.style.QuoteSpan
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.QuestionSet
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.models.deserialized.Style
import com.wapo.flagship.features.articles2.models.deserialized.Truncate
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.text.BlockquoteSpan
import com.wapo.text.GlobalFontAdjustmentSpan
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.R
import kotlin.math.roundToInt

class SanitizedHtmlTextFormatter(
    val context: Context,
    val articlesInteractionHelper: ArticlesInteractionHelper,
) {
    var textStyleProducer: (SanitizedHtml?) -> Int = {
        StylesHelper.getTextItemStyle(
            context,
            Style.getValue(it?.style),
        )
    }
    var subheadTextStyleProducer: (SanitizedHtml?) -> Int = {
        StylesHelper.getTextItemSubheadStyle(
            context,
            Style.getValue(it?.style),
            it?.subheadLevel,
        )
    }

    fun format(
        sanitizedHtml: SanitizedHtml?,
        isBriefExclusiveLabel: Boolean? = null,
        questionSet: List<QuestionSet>? = null,
    ): CharSequence {
        val sequence =
            when {
                sanitizedHtml?.content != null -> {
                    if ("text/plain" == sanitizedHtml.mime) {
                        if (isBriefExclusiveLabel == true) {
                            val allCaps = sanitizedHtml.content.uppercase()
                            allCaps
                        } else {
                            sanitizedHtml.content
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Html.fromHtml(sanitizedHtml.content, Html.FROM_HTML_MODE_LEGACY)
                        } else {
                            Html.fromHtml(sanitizedHtml.content)
                        }
                    }
                }
                else -> {
                    ""
                }
            }
        val strBuilder =
            if (sequence is SpannableStringBuilder) {
                sequence
            } else {
                SpannableStringBuilder(
                    sequence,
                )
            }
        val singleNumber = isSingleNumber(sanitizedHtml, strBuilder)
        val style =
            if (singleNumber) {
                StylesHelper.getTextItemBriefsNumberStyle(context)
            } else {
                when (sanitizedHtml?.subtype) {
                    "subhead" -> subheadTextStyleProducer(sanitizedHtml)
                    "extra", "trailer" -> StylesHelper.getTextItemFooterStyle(context)
                    "intro" -> StylesHelper.getTextItemIntroStyle(context)
                    "letter" -> StylesHelper.getTextItemLetterStyle(context)
                    "metatext" -> StylesHelper.getTextItemMetaTextStyle(context)
                    "expanded-byline" -> StylesHelper.getExpendedBylineTextItemStyle(context)
                    "blockquote" -> {
                        val blockQuoteSpan =
                            BlockquoteSpan(
                                StylesHelper.getBlockQuoteLineColor(context),
                                StylesHelper.getBlockQuoteMargin(context),
                                StylesHelper.getBlockQuoteLineWidth(context),
                            )
                        strBuilder.setSpan(
                            blockQuoteSpan,
                            0,
                            strBuilder.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        textStyleProducer(sanitizedHtml)
                    }
                    else -> {
                        val quoteSpans: Array<QuoteSpan>? =
                            strBuilder.getSpans(
                                0,
                                strBuilder.length,
                                QuoteSpan::class.java,
                            )
                        if (quoteSpans != null) {
                            for (quoteSpan in quoteSpans) {
                                strBuilder.removeSpan(quoteSpan)
                            }
                        }
                        textStyleProducer(sanitizedHtml)
                    }
                }
            }

        val isBriefsExclusiveLabel = Style.getValue(sanitizedHtml?.style) == Style.BRIEFS && sanitizedHtml?.subheadLevel == 3

        if (sanitizedHtml?.subheadLevel != null && !singleNumber && !isBriefsExclusiveLabel) {
            strBuilder.setSpan(
                WpTextAppearanceSpan(
                    context,
                    style,
                    StylesHelper.getParagraphHeadingSize(context, sanitizedHtml.subheadLevel),
                ),
                0,
                strBuilder.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else {
            strBuilder.setSpan(
                WpTextAppearanceSpan(context, style),
                0,
                strBuilder.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        val questionIds = questionSet?.map { questionObj -> questionObj.id }
        val stringBuilderWithLinks =
            StylesHelper.makeLinkClickable(
                strBuilder,
                context,
                articlesInteractionHelper,
                questionIds,
                sanitizedHtml?.sourceAnnotations,
            )

        val content = SpannableString(stringBuilderWithLinks)
        content.setSpan(
            GlobalFontAdjustmentSpan(),
            0,
            content.length,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE,
        )
        return content
    }

    fun isSingleNumber(
        item: SanitizedHtml?,
        formattexText: CharSequence,
    ): Boolean {
        item ?: return false
        val style = Style.getValue(item.style)
        return style == Style.BRIEFS && formattexText.toString().length == 1 && formattexText.toString().isDigitsOnly()
    }

    fun formatTruncate(
        truncate: Truncate?,
        isTruncated: Boolean,
        paragraphText: CharSequence,
    ): CharSequence {
        val formattedText = SpannableStringBuilder()
        truncate ?: return formattedText
        // Find Label based on card state (isTruncated)
        var label = StringBuilder()
        var labelText =
            if (isTruncated) truncate.truncatedLabel else truncate.expandedLabel
        // Fallback to the default labels
        labelText =
            if (labelText.isNullOrEmpty()) {
                if (isTruncated) {
                    context.getString(R.string.article_truncated_label)
                } else {
                    context.getString(R.string.articles_expanded_label)
                }
            } else {
                labelText
            }
        label.append(labelText)
        label.append('\u00A0')
        val imagePlaceholderText = "[arrow]"
        label.append(imagePlaceholderText)
        // Insert Narrow No-Break Space where there is a space to display label text in one line.
        val truncateLabel = label.toString().replace(' ', '\u202F')
        // apply style to the label
        formattedText.append(formatTruncateLabel(truncateLabel, paragraphText))
        // add arrow icon
        val drawableResId =
            if (isTruncated) R.drawable.expand_more_24px else R.drawable.expand_less_24px
        ContextCompat.getDrawable(context, drawableResId)?.let { drawable ->
            drawable.setBounds(
                0,
                0,
                context.resources.getDimensionPixelSize(
                    R.dimen.native_article_truncate_expand_icon_size,
                ),
                context.resources.getDimensionPixelSize(
                    R.dimen.native_article_truncate_expand_icon_size,
                ),
            )
            val image = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
            val placeholderIndex = formattedText.indexOf(imagePlaceholderText)
            formattedText.setSpan(
                image,
                placeholderIndex,
                placeholderIndex + imagePlaceholderText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return formattedText
    }

    private fun formatTruncateLabel(
        label: CharSequence,
        paragraphText: CharSequence,
    ): CharSequence {
        val labelHeight =
            context.resources.getDimensionPixelSize(
                R.dimen.native_article_truncate_expand_text_height,
            )
        val formattedText = SpannableString(label)
        val style = StylesHelper.getTruncateStyle(context)
        formattedText.setSpan(
            WpTextAppearanceSpan(context, style),
            0,
            formattedText.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        formattedText.setSpan(
            object : LineHeightSpan {
                override fun chooseHeight(
                    text: CharSequence?,
                    start: Int,
                    end: Int,
                    spanstartv: Int,
                    lineHeight: Int,
                    fm: Paint.FontMetricsInt?,
                ) {
                    fm?.apply {
                        if (start == paragraphText.length - label.length) {
                            val originalHeight = fm.descent - fm.ascent
                            // If original height is not positive, do nothing.
                            if (originalHeight <= 0) {
                                return
                            }
                            val ratio = labelHeight * 1.0f / originalHeight
                            fm.descent = (fm.descent * ratio).roundToInt()
                            fm.ascent = fm.descent - labelHeight
                        }
                    }
                }
            },
            0,
            formattedText.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return formattedText
    }
}
