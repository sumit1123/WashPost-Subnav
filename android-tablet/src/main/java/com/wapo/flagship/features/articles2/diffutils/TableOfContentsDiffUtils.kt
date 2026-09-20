package com.wapo.flagship.features.articles2.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.wapo.flagship.features.articles2.models.LiveEntry

class TableOfContentsDiffUtils : DiffUtil.ItemCallback<LiveEntry>() {
    override fun areItemsTheSame(
        oldItem: LiveEntry,
        newItem: LiveEntry,
    ): Boolean = oldItem == newItem

    override fun areContentsTheSame(
        oldItem: LiveEntry,
        newItem: LiveEntry,
    ): Boolean = oldItem == newItem
}
