/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.viewholders

import com.wapo.flagship.features.homepage.adapters.HomepageItemAdapter
import com.wapo.flagship.features.homepage.models.HomepageItem
import com.wapo.flagship.features.homepage.models.MenuItem
import com.washingtonpost.android.databinding.ItemMenuBinding

class MenuItemViewHolder(
    private val binding: ItemMenuBinding,
    private val onMenuItemClick: (MenuItem) -> Unit
) : HomepageItemAdapter.HomepageItemViewHolder(binding.root) {

    override fun bind(homepageItem: HomepageItem) {
        with(binding) {
            if (homepageItem is MenuItem) {
                btnMenu.text = homepageItem.sectionName
                btnMenu.setIconResource(homepageItem.iconResId)
                btnMenu.setOnClickListener {
                    onMenuItemClick(homepageItem)
                }
            }
        }
    }

}