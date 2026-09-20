package com.wapo.flagship.features.articles2.viewholders

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.View
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Deck
import com.wapo.flagship.features.articles2.utils.DeckStyleHelper
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.databinding.ItemDeckBinding

class DeckViewHolder(
    private val binding: ItemDeckBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Deck>(
        binding.root,
    ) {
    override fun bind(
        item: Deck,
        position: Int,
    ) {
        var deck: String? = null
        if (item is Deck) {
            deck = item.content
        }
        val spannableStringBuilder = SpannableStringBuilder()
        if (deck != null && deck.isNotEmpty()) {
            spannableStringBuilder.append(deck)
            spannableStringBuilder.setSpan(
                WpTextAppearanceSpan(
                    binding.articleHeadingDeck.context,
                    DeckStyleHelper.getDeckStyle(binding.root.context),
                ),
                0,
                deck.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            binding.articleHeadingDeck.text = spannableStringBuilder
            binding.articleHeadingDeck.visibility = View.VISIBLE
        } else {
            binding.articleHeadingDeck.visibility = View.GONE
        }
    }
}
