package com.wapo.flagship.features.articles2.viewholders

import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.view.View
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Title
import com.wapo.flagship.features.articles2.utils.HeadLinesStyleHelper
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.text.GlobalFontAdjustmentSpan
import com.wapo.text.WpTextAppearanceSpan
import com.wapo.text.WpTextFormatter
import com.washingtonpost.android.databinding.ItemTitleBinding

class TitleViewHolder(
    private val binding: ItemTitleBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Title>(binding.root) {
    override fun bind(
        item: Title,
        position: Int,
    ) {
        var titleText: String? = null
        var headlineStyle = HeadLinesStyleHelper.getHeadLineStyle(binding.root.context)
        if (item is Title) {
            titleText = item.liveContent ?: item.content
            if (item.style == Title.Style.STYLE.value) {
                headlineStyle = HeadLinesStyleHelper.getTextHeadLineStyleText(binding.root.context)
            } else if (item.subtype == Title.SubType.H1.value) {
                headlineStyle = HeadLinesStyleHelper.getTextHeadLineStyle1(binding.root.context)
            } else if (item.subtype == Title.SubType.H2.value) {
                headlineStyle = HeadLinesStyleHelper.getTextHeadLineStyle2(binding.root.context)
            } else if (item.subtype == Title.SubType.LIVE_REPORTER_INSIGHTS.value) {
                headlineStyle =
                    HeadLinesStyleHelper.getArticleLiveHeadLineStyle(binding.root.context)
            }
        }
        if (TextUtils.isEmpty(titleText)) {
            binding.articleHeadingHeadline.visibility = View.GONE
        } else {
            val title =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    SpannableString(Html.fromHtml(titleText, Html.FROM_HTML_MODE_LEGACY))
                } else {
                    SpannableString(Html.fromHtml(titleText))
                }
            title.setSpan(
                WpTextAppearanceSpan(itemView.context, headlineStyle),
                0,
                title.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            title.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                title.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            WpTextFormatter.applyLineSpacing(binding.articleHeadingHeadline, headlineStyle)
            binding.articleHeadingHeadline.text = title
            binding.articleHeadingHeadline.key = KeyHelper.createKey(position, title.toString())
            binding.articleHeadingHeadline.visibility = View.VISIBLE
        }
    }
}
