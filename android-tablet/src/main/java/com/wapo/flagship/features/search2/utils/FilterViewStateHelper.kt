package com.wapo.flagship.features.search2.utils

import android.content.Context
import com.wapo.flagship.features.search2.model.FilterCheckItem
import com.wapo.flagship.features.search2.model.FilterHeaderItem
import com.wapo.flagship.features.search2.model.FilterItem
import com.wapo.flagship.features.search2.model.FilterRadioItem
import com.wapo.flagship.features.search2.ui.adapter.FilterAdapter
import com.washingtonpost.android.databinding.FragmentSearch2FilterBinding

class FilterViewStateHelper(
    val binding: FragmentSearch2FilterBinding,
    val context: Context,
) {
    fun loadFilters(map: Map<FilterHeaderItem, List<FilterItem>>) {
        val list = mutableListOf<FilterItem>()

        map.forEach {
            list.add(it.key.copy())
            if (it.key.isCollapsed != true) {
                it.value.forEach { item ->
                    when (item) {
                        is FilterRadioItem -> list.add(item.copy())
                        is FilterCheckItem -> list.add(item.copy())
                        else -> {
                        }
                    }
                }
            }
        }
        (binding.rvFilter.adapter as? FilterAdapter)?.apply {
            submitList(list)
        }
    }

    companion object {
        private const val SORT_BY_HEADER = "Sort By"
        private const val DATE_HEADER = "Date"
        private const val SECTIONS_HEADER = "Sections"
    }
}
