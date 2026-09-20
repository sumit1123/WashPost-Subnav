/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.viewholders

import com.wapo.flagship.features.homepage.adapters.HomepageItemAdapter
import com.wapo.flagship.features.homepage.models.HeaderItem
import com.wapo.flagship.features.homepage.models.HomepageItem
import com.washingtonpost.android.databinding.ItemHeaderBinding

class HeaderItemViewHolder(private val binding: ItemHeaderBinding) :
    HomepageItemAdapter.HomepageItemViewHolder(binding.root) {

    override fun bind(homepageItem: HomepageItem) {
        if (homepageItem is HeaderItem) {
            binding.tvHeader.text = homepageItem.text
        }
    }

}