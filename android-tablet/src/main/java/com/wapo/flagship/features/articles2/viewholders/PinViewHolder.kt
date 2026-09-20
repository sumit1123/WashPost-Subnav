package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Pin
import com.washingtonpost.android.databinding.ItemPinBinding

class PinViewHolder(
    private val binding: ItemPinBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Pin>(binding.root) {
    override fun bind(
        item: Pin,
        position: Int,
    ) {
        binding.pinText.text = item.content
    }
}
