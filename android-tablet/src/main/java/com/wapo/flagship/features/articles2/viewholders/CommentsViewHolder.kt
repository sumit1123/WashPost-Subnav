package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.Comments
import com.washingtonpost.android.databinding.ItemCommentsBinding

class CommentsViewHolder(
    private val binding: ItemCommentsBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Comments>(binding.root) {
    override fun bind(
        item: Comments,
        position: Int,
    ) {
        binding.btnViewComments.setOnClickListener {
            articlesInteractionHelper.onEventFired(
                ArticleInteractionEvent.ViewCommentsClickEvent,
            )
        }
    }
}
