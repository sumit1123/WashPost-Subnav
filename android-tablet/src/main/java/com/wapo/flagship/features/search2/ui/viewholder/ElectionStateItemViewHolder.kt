package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.ElectionItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchElectionItemBinding

class ElectionStateItemViewHolder(
    val binding: SearchElectionItemBinding,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<ElectionItem>(binding.root) {
    override fun bind(item: ElectionItem) {
        super.bind(item)
        binding.label.text = item.name

        binding.root.setOnClickListener {
            onItemClick(UserEvent.SearchElectionItemClick(item, bindingAdapterPosition))
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
    }
}
