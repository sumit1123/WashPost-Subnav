/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.wapo.flagship.features.homepage.models.HomepageItem

class HomepageItemDiffUtil : DiffUtil.ItemCallback<HomepageItem>() {

    override fun areItemsTheSame(old: HomepageItem, aNew: HomepageItem): Boolean {
        return old == aNew
    }

    override fun areContentsTheSame(old: HomepageItem, aNew: HomepageItem): Boolean {
        return old == aNew
    }

}