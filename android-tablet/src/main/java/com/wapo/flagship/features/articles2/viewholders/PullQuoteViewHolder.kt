package com.wapo.flagship.features.articles2.viewholders

import android.text.SpannableString
import android.text.Spanned
import android.view.View
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.PullQuote
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.PullQuoteStyleHelper
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.databinding.ItemPullQuoteBinding

class PullQuoteViewHolder(
    private val binding: ItemPullQuoteBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<PullQuote>(binding.root) {
    override fun bind(
        item: PullQuote,
        position: Int,
    ) {
        super.bind(item, position)

        val context = binding.root.context
        val quoteStyle: Int = PullQuoteStyleHelper.getPullQuoteStyle(context)
        val captionStyle: Int = PullQuoteStyleHelper.getPullQuoteCaptionStyle(context)

        if (!item.content.isNullOrEmpty()) {
            val quoteSpan = SpannableString.valueOf(item.content)
            quoteSpan.setSpan(
                WpTextAppearanceSpan(context, quoteStyle),
                0,
                quoteSpan.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            binding.pullQuoteText.text = quoteSpan
            binding.pullQuoteText.key = KeyHelper.createKey(position, quoteSpan.toString())
            binding.pullQuoteText.visibility = View.VISIBLE
        } else {
            binding.pullQuoteText.visibility = View.GONE
        }

        val attribution: String? = item.attribution
        if (attribution != null && attribution.trim { it <= ' ' }.isNotEmpty()) {
            val captionSpan = SpannableString.valueOf(attribution)
            captionSpan.setSpan(
                WpTextAppearanceSpan(context, captionStyle),
                0,
                captionSpan.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            binding.pullQuoteCaptionText.text = captionSpan
            binding.pullQuoteCaptionText.key = KeyHelper.createKey(position, captionSpan.toString())
            binding.pullQuoteCaptionText.visibility = View.VISIBLE
        } else {
            binding.pullQuoteCaptionText.visibility = View.GONE
        }
    }
}
