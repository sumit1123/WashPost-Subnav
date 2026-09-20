package com.wapo.flagship.features.search2.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.search2.events.FilterEvent
import com.wapo.flagship.features.search2.model.*
import com.wapo.flagship.features.search2.ui.viewholder.*
import com.washingtonpost.android.databinding.*

/**
 * Adapter for handling the various items in Search Recycler View
 */
class FilterAdapter(
    val onItemClick: (FilterEvent) -> Unit,
) : ListAdapter<FilterItem, RecyclerView.ViewHolder>(
        DiffUtils(),
    ) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            FilterType.HEADER.id -> {
                val viewBinding = FilterHeaderBinding.inflate(inflater, parent, false)
                FilterHeaderViewHolder(viewBinding, onItemClick)
            }
            FilterType.RADIO.id -> {
                val viewBinding = FilterRadioBinding.inflate(inflater, parent, false)
                FilterRadioViewHolder(viewBinding, onItemClick)
            }
            FilterType.CHECK.id -> {
                val viewBinding = FilterCheckBinding.inflate(inflater, parent, false)
                FilterCheckViewHolder(viewBinding, onItemClick)
            }
            else -> {
                val viewBinding = SearchSpacerBinding.inflate(inflater, parent, false)
                SpacerViewHolder(viewBinding)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val searchHolder = holder as? FilterViewHolder<FilterItem>
        searchHolder?.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is FilterHeaderItem -> FilterType.HEADER.id
            is FilterRadioItem -> FilterType.RADIO.id
            is FilterCheckItem -> FilterType.CHECK.id
            else -> FilterType.UNKNOWN.id
        }

    class DiffUtils : DiffUtil.ItemCallback<FilterItem>() {
        override fun areItemsTheSame(
            oldItem: FilterItem,
            newItem: FilterItem,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: FilterItem,
            newItem: FilterItem,
        ): Boolean {
            val same =
                when {
                    oldItem is FilterRadioItem && newItem is FilterRadioItem ->
                        oldItem.id == newItem.id &&
                            oldItem.isChecked == newItem.isChecked
                    oldItem is FilterCheckItem && newItem is FilterCheckItem ->
                        oldItem.id == newItem.id &&
                            oldItem.isChecked == newItem.isChecked
                    else -> oldItem.id == newItem.id
                }

            return same
        }
    }

    enum class FilterType(
        val id: Int,
    ) {
        HEADER(0),
        RADIO(1),
        CHECK(2),
        UNKNOWN(3),
    }

    open class FilterViewHolder<T : FilterItem>(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        open fun bind(item: T) {
        }

        open fun unbind() {
        }
    }
}
