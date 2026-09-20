// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.mypost.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.wapo.flagship.features.mypost.models.TopicFollowButtonActionItem
import com.wapo.flagship.features.mypost.models.TopicItemActionItem
import com.wapo.flagship.features.mypost.viewholders.TopicItemViewHolder
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.models.MyPostTopicItem

class TopicsAdapter(
    private val onInterestItemClick: (TopicItemActionItem) -> Unit,
    private val onFollowButtonClick: (TopicFollowButtonActionItem) -> Unit,
) : ListAdapter<MyPostTopicItem, TopicItemViewHolder>(DiffUtils()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TopicItemViewHolder =
        LayoutInflater
            .from(parent.context)
            .inflate(R.layout.topic_item, parent, false)
            .run { TopicItemViewHolder(this) }

    override fun onBindViewHolder(
        holder: TopicItemViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position), onInterestItemClick, onFollowButtonClick)
    }

    class DiffUtils : DiffUtil.ItemCallback<MyPostTopicItem>() {
        override fun areItemsTheSame(
            oldItem: MyPostTopicItem,
            newItem: MyPostTopicItem,
        ): Boolean = oldItem.topicId == newItem.topicId

        override fun areContentsTheSame(
            oldItem: MyPostTopicItem,
            newItem: MyPostTopicItem,
        ): Boolean = oldItem == newItem
    }
}
