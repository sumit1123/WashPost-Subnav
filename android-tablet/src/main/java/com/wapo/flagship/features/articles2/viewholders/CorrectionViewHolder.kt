package com.wapo.flagship.features.articles2.viewholders

import android.content.Context
import android.os.Build
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.Correction
import com.wapo.flagship.features.articles2.utils.CorrectionStyleHelper
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemCorrectionBinding

class CorrectionViewHolder(
    private val binding: ItemCorrectionBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Correction>(
        binding.root,
    ) {
    private val correctionTypeView = binding.articleCorrectionType
    private val correctionTextView = binding.articleCorrectionText
    private val correctionBackgroundView = binding.articleCorrectionBackground

    init {
        correctionTypeView.movementMethod = LinkMovementMethod.getInstance()
        correctionTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun bind(
        item: Correction,
        position: Int,
    ) {
        val context = itemView.context
        correctionBackgroundView.setBackgroundColor(
            ContextCompat.getColor(context, com.washingtonpost.android.articles.R.color.article_correction_background),
        )
        if (item.correctionType != null && item.content != null) {
            itemView.visibility = View.VISIBLE
            itemView.layoutParams =
                RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
        } else {
            itemView.visibility = View.GONE
            itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }
        if (item.correctionType != null) {
            val correctionTitleSpanBuilder = SpannableStringBuilder()
            val wpTitleTextAppearanceSpan =
                WpTextAppearanceSpan(
                    context,
                    CorrectionStyleHelper.getCorrectionTitleStyle(binding.root.context),
                )
            correctionTitleSpanBuilder.append(fromHtml(item.correctionType))
            correctionTitleSpanBuilder.setSpan(
                wpTitleTextAppearanceSpan,
                0,
                correctionTitleSpanBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            correctionTypeView.text = correctionTitleSpanBuilder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                correctionTypeView.letterSpacing = 0.06f
            }
        } else {
            correctionTypeView.visibility = View.GONE
        }
        if (item.content != null) {
            val correctionBodySpanBuilder = SpannableStringBuilder()
            val wpTitleTextAppearanceSpan =
                WpTextAppearanceSpan(
                    context,
                    CorrectionStyleHelper.getCorrectionBodyStyle(binding.root.context),
                )
            correctionBodySpanBuilder.append(fromHtml(item.content))
            correctionBodySpanBuilder.setSpan(
                wpTitleTextAppearanceSpan,
                0,
                correctionBodySpanBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            correctionTextView.text = correctionBodySpanBuilder
            stripUnderlines(correctionTextView)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                correctionTextView.letterSpacing = 0.05f
            }
        } else {
            correctionTextView.visibility = View.GONE
        }
    }

    private fun fromHtml(html: String?): Spanned? =
        if (html == null) {
            SpannableString("")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }
        }

    private fun stripUnderlines(textView: TextView) {
        val s: Spannable = SpannableString(textView.text)
        val spans = s.getSpans(0, s.length, URLSpan::class.java)
        for (span in spans) {
            val start = s.getSpanStart(span)
            val end = s.getSpanEnd(span)
            s.removeSpan(span)
            val spanCopy =
                URLSpanNoUnderline(
                    span.url,
                    binding.root.context,
                    articlesInteractionHelper,
                )
            s.setSpan(spanCopy, start, end, 0)
        }
        textView.text = s
    }

    private class URLSpanNoUnderline(
        url: String?,
        val context: Context,
        val articlesInteractionHelper: ArticlesInteractionHelper,
    ) : URLSpan(
            url,
        ) {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
            ds.color = ContextCompat.getColor(context, com.wapo.view.R.color.link_color)
        }

        override fun onClick(widget: View) {
            articlesInteractionHelper.onEventFired(ArticleInteractionEvent.LinkClickEvent(url))
        }
    }
}
