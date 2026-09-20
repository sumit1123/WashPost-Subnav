package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.ExpandableItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.SearchExpandableItemBinding

class ExpandableItemViewHolder(
    val binding: SearchExpandableItemBinding,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<ExpandableItem>(binding.root) {
    override fun bind(item: ExpandableItem) {
        super.bind(item)
        binding.expandableLabel.text = item.label
        if (item.expanded) {
            binding.expandableImage.setBackgroundResource(R.drawable.see_less_arrow)
        } else {
            binding.expandableImage.setBackgroundResource(R.drawable.see_more_arrow)
        }

        binding.root.setOnClickListener {
            item.expanded = !item.expanded
            onItemClick(UserEvent.ExpandableItemClick(item))
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
    }

    fun getItemViewType(position: Int): Int = position
}
