package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Link
import com.washingtonpost.android.databinding.ItemLinkBinding

class LinkViewHolder(
    private val binding: ItemLinkBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Link>(
        binding.root,
    ) {
    override fun bind(
        item: Link,
        position: Int,
    ) {
        super.bind(item, position)
    }
}
