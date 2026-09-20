/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.homepage.diffutils.HomepageItemDiffUtil
import com.wapo.flagship.features.homepage.models.HeaderItem
import com.wapo.flagship.features.homepage.models.HomepageItem
import com.wapo.flagship.features.homepage.models.MenuItem
import com.wapo.flagship.features.homepage.viewholders.DefaultViewHolder
import com.wapo.flagship.features.homepage.viewholders.HeaderItemViewHolder
import com.wapo.flagship.features.homepage.viewholders.MenuItemViewHolder
import com.washingtonpost.android.databinding.ItemDefaultBinding
import com.washingtonpost.android.databinding.ItemHeaderBinding
import com.washingtonpost.android.databinding.ItemMenuBinding

class HomepageItemAdapter(
    private val onMenuItemClick: (MenuItem) -> Unit
) : ListAdapter<HomepageItem, HomepageItemAdapter.HomepageItemViewHolder>(
    HomepageItemDiffUtil()
) {

    abstract class HomepageItemViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        abstract fun bind(homepageItem: HomepageItem)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HomepageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            MENU_ITEM -> {
                val binding = ItemMenuBinding.inflate(inflater, parent, false)
                MenuItemViewHolder(binding, onMenuItemClick)
            }
            HEADER_ITEM -> {
                val binding = ItemHeaderBinding.inflate(inflater, parent, false)
                HeaderItemViewHolder(binding)
            }
            else -> {
                val binding = ItemDefaultBinding.inflate(inflater, parent, false)
                DefaultViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: HomepageItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MenuItem -> MENU_ITEM
            is HeaderItem -> HEADER_ITEM
            else -> super.getItemViewType(position)
        }
    }

    companion object {
        const val MENU_ITEM = 1
        const val HEADER_ITEM = 3
    }

}