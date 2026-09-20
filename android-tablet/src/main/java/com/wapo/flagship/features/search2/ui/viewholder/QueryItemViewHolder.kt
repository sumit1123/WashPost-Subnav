package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.SearchQueryItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchQueryItemBinding

class QueryItemViewHolder(
    val binding: SearchQueryItemBinding,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<SearchQueryItem>(binding.root) {
    override fun bind(item: SearchQueryItem) {
        super.bind(item)
        binding.label.text = item.query

        binding.root.setOnClickListener {
            onItemClick(UserEvent.SearchQueryItemClick(item))
        }

        binding.removeSearchItem.setOnClickListener {
            onItemClick(UserEvent.RemoveItemClick(item))
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
        binding.removeSearchItem.setOnClickListener(null)
    }
}
