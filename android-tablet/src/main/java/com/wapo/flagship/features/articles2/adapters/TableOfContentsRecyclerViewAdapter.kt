package com.wapo.flagship.features.articles2.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.wapo.flagship.features.articles2.diffutils.TableOfContentsDiffUtils
import com.wapo.flagship.features.articles2.models.LiveEntry
import com.wapo.flagship.features.articles2.viewholders.ItemViewHolder
import com.wapo.flagship.features.articles2.viewholders.TableOfContentsItemSelectedViewHolder
import com.wapo.flagship.features.articles2.viewholders.TableOfContentsItemViewHolder
import com.wapo.view.RippleHelper
import com.washingtonpost.android.databinding.ItemSelectedTableOfContentsBinding
import com.washingtonpost.android.databinding.ItemTableOfContentsBinding

class TableOfContentsRecyclerViewAdapter(
    private val currentAnchorPos: () -> Int,
    private val onClick: (String?) -> Unit,
) : ListAdapter<LiveEntry, ItemViewHolder>(TableOfContentsDiffUtils()) {
    private enum class ViewType {
        NORMAL,
        SELECTED,
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.SELECTED.ordinal -> {
                val viewBinding =
                    ItemSelectedTableOfContentsBinding.inflate(inflater, parent, false)
                RippleHelper.addRippleEffectToView(viewBinding.root)
                TableOfContentsItemSelectedViewHolder(viewBinding, onClick)
            }
            else -> {
                val viewBinding = ItemTableOfContentsBinding.inflate(inflater, parent, false)
                RippleHelper.addRippleEffectToView(viewBinding.root)
                TableOfContentsItemViewHolder(viewBinding, onClick)
            }
        }
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int =
        if (position == currentAnchorPos()) {
            ViewType.SELECTED.ordinal
        } else {
            ViewType.NORMAL.ordinal
        }

    override fun getItemId(position: Int): Long = position.toLong()
}
