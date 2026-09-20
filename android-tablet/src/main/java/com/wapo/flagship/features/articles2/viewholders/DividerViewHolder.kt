package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Divider
import com.washingtonpost.android.databinding.ItemDividerBinding

class DividerViewHolder(
    private val binding: ItemDividerBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Divider>(binding.root) {
    override fun bind(
        item: Divider,
        position: Int,
    ) {
    }
}
