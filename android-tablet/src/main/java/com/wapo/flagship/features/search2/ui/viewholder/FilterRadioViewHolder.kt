package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.model.FilterRadioItem
import com.wapo.flagship.features.search2.ui.adapter.FilterAdapter
import com.washingtonpost.android.databinding.FilterRadioBinding

class FilterRadioViewHolder(
    val binding: FilterRadioBinding,
    val onItemClick: (FilterEvent) -> Unit,
) : FilterAdapter.FilterViewHolder<FilterRadioItem>(binding.root) {
    override fun bind(item: FilterRadioItem) {
        super.bind(item)
        binding.radio.text = item.label

        binding.radio.isChecked = item.isChecked

        binding.radio.setOnClickListener {
            onItemClick(FilterEvent.Radio(item))
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
    }
}
