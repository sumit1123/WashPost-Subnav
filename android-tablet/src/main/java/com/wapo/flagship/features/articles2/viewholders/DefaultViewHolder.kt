package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.Item
import com.washingtonpost.android.databinding.ItemDefaultBinding

class DefaultViewHolder(
    private val binding: ItemDefaultBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Item>(
        binding.root,
    ) {
    override fun bind(
        item: Item,
        position: Int,
    ) {
        // This is only to test the type if any unexpected type were to be to seen. Uncomment lines below in that case.
        // binding.text.visibility = View.VISIBLE
        // binding.text.text = item.type
    }
}
