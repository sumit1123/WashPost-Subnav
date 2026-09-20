package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.SectionItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchSectionItemBinding

class SectionItemViewHolder(
    val binding: SearchSectionItemBinding,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<SectionItem>(binding.root) {
    override fun bind(item: SectionItem) {
        super.bind(item)
        binding.label.text = item.label

        binding.root.setOnClickListener {
            onItemClick(UserEvent.SectionItemClick(item))
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
    }
}
