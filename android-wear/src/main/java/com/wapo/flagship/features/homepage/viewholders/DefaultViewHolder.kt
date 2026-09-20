/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.viewholders

import com.wapo.flagship.features.homepage.adapters.HomepageItemAdapter
import com.wapo.flagship.features.homepage.models.HomepageItem
import com.washingtonpost.android.databinding.ItemDefaultBinding

class DefaultViewHolder(private val binding: ItemDefaultBinding) :
    HomepageItemAdapter.HomepageItemViewHolder(binding.root) {

    override fun bind(homepageItem: HomepageItem) {
    }

}