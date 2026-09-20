package com.wapo.flagship.features.articles2.viewholders

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.InterstitialLink
import com.wapo.flagship.features.articles2.models.deserialized.Style
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.databinding.ItemInterstatialLinkBinding

class InterstatialLinkViewHolder(
    private val binding: ItemInterstatialLinkBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<InterstitialLink>(
        binding.root,
    ) {
    override fun bind(
        item: InterstitialLink,
        position: Int,
    ) {
        super.bind(item, position)
        val content = item.content
        if (content != null) {
            binding.articleInterstitialLink.movementMethod = LinkMovementMethod.getInstance()
            val strBuilder = StringBuilder(content)
            if (!content.startsWith("[")) {
                strBuilder.insert(0, "[")
            }
            if (!content.endsWith("]")) {
                strBuilder.append("]")
            }
            val url = item.url
            val urlSpan: URLSpan =
                object : URLSpan(url) {
                    override fun onClick(view: View) {
                        if (url != null) {
                            articlesInteractionHelper.onEventFired(
                                ArticleInteractionEvent.LinkClickEvent(url),
                            )
                        }
                    }
                }
            val linkStart = strBuilder.indexOf("[") + 1
            val linkEnd = strBuilder.indexOf("]")
            var spannableStringBuilder: SpannableStringBuilder? = SpannableStringBuilder(strBuilder)
            spannableStringBuilder?.setSpan(
                WpTextAppearanceSpan(
                    itemView.context,
                    StylesHelper.getTextItemStyle(binding.root.context, Style.DEFAULT),
                ),
                0,
                strBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            spannableStringBuilder?.setSpan(
                urlSpan,
                linkStart,
                linkEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            spannableStringBuilder =
                StylesHelper.makeLinkClickable(
                    spannableStringBuilder!!,
                    binding.root.context,
                    articlesInteractionHelper,
                )
            binding.articleInterstitialLink.setLineSpacing(
                StylesHelper.getTextSpacingExtra(binding.root.context),
                StylesHelper.getTextSpacingMult(binding.root.context),
            )
            binding.articleInterstitialLink.text = spannableStringBuilder
            binding.articleInterstitialLink.key =
                KeyHelper.createKey(
                    position,
                    spannableStringBuilder.toString(),
                )
        }
    }
}
