package com.wapo.flagship.views

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.articles2.models.Item

abstract class ExpandableListAdapter<T, V : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
) : ListAdapter<T, V>(diffCallback) {
    private val expandableItems = mutableListOf<Item>()
    private var recyclerView: RecyclerView? = null

    open val onGroupToggleClicked: (Int, Item) -> Unit = { position, item ->
        val isExpanded = expandableItems.contains(item)
        if (isExpanded) {
            expandableItems.remove(item)
        } else {
            expandableItems.add(item)
        }

        notifyItemChanged(position)

        if (isExpanded) {
            recyclerView?.scrollToPosition(position)
        }
    }

    open val expandedItemLookUp: (Item) -> Boolean = { element: Item? ->
        expandableItems.contains(
            element,
        )
    }
}
