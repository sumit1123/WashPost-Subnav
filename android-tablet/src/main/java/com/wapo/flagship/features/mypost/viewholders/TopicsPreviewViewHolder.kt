// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.mypost.viewholders

import com.wapo.flagship.features.mypost.adapters.TopicsAdapter
import com.wapo.flagship.features.mypost.models.TopicFollowButtonActionItem
import com.wapo.flagship.features.mypost.models.TopicItemActionItem
import com.washingtonpost.android.R
import com.washingtonpost.android.save.databinding.MyPostTopicsPreviewBinding
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.models.PreviewItem.SectionPreviewItem
import com.washingtonpost.android.save.types.MyPostSection
import com.washingtonpost.android.save.viewholders.ItemViewHolder

class TopicsPreviewViewHolder(
    private val topicsPreviewBinding: MyPostTopicsPreviewBinding,
    private val onInterestItemClick: (TopicItemActionItem) -> Unit,
    private val onFollowButtonClick: (TopicFollowButtonActionItem) -> Unit,
    private val onViewMoreClick: (MyPostSection) -> Unit,
) : ItemViewHolder(topicsPreviewBinding.root) {
    override fun bind(previewItem: PreviewItem) {
        super.bind(previewItem)

        val maxItems = ((previewItem as SectionPreviewItem).topicsList?.size ?: 0).coerceAtMost(5)
        val topicItems = previewItem.topicsList?.subList(0, maxItems)
        val adapter =
            TopicsAdapter(onInterestItemClick, onFollowButtonClick).apply {
                this.submitList(topicItems)
            }
        topicsPreviewBinding.topicList.adapter = adapter

        topicsPreviewBinding.tvDescription.text =
            if (topicItems?.any { it.isFollowing } == true) {
                topicsPreviewBinding.root.context.resources.getString(
                    com.washingtonpost.android.save.R.string.my_post_topics_preview_desc_following,
                )
            } else {
                topicsPreviewBinding.root.context.resources.getString(
                    com.washingtonpost.android.save.R.string.my_post_topics_preview_desc_not_following,
                )
            }

        topicsPreviewBinding.buttonViewMore.setOnClickListener {
            onViewMoreClick(MyPostSection.TOPICS)
        }
    }
}
