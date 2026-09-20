package com.wapo.flagship.features.articles2.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.wapo.flagship.features.articles2.models.Item

class Articles2ItemDiffUtils : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(
        old: Item,
        aNew: Item,
    ): Boolean = old == aNew

    override fun areContentsTheSame(
        old: Item,
        aNew: Item,
    ): Boolean = old == aNew
}
