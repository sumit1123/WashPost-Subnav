package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.HeaderItem
import com.wapo.flagship.features.search2.model.SearchQueryItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchHeaderBinding

class HeaderViewHolder(
    val binding: SearchHeaderBinding,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<HeaderItem>(binding.root) {
    override fun bind(item: HeaderItem) {
        super.bind(item)
        binding.label.text = item.label
        initClearAll(item)
    }

    private fun initClearAll(item: HeaderItem) {
        val visible =
            when (item.groupType) {
                SearchQueryItem::class -> true
                else -> false
            }

        binding.clearAll.setVisible(visible)

        item.groupType?.let { type ->
            binding.clearAll.setOnClickListener {
                onItemClick(UserEvent.RemoveAllItemsClick(type))
            }
        }
    }
}
