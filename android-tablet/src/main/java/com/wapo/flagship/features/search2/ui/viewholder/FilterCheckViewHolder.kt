package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.model.FilterCheckItem
import com.wapo.flagship.features.search2.ui.adapter.FilterAdapter
import com.washingtonpost.android.databinding.FilterCheckBinding

class FilterCheckViewHolder(
    val binding: FilterCheckBinding,
    val onItemClick: (FilterEvent) -> Unit,
) : FilterAdapter.FilterViewHolder<FilterCheckItem>(binding.root) {
    override fun bind(item: FilterCheckItem) {
        super.bind(item)
        binding.check.text = item.label

        binding.check.isChecked = item.isChecked

        binding.check.setOnClickListener {
            item.isChecked = !item.isChecked
            onItemClick(FilterEvent.Check(item))
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
    }
}
