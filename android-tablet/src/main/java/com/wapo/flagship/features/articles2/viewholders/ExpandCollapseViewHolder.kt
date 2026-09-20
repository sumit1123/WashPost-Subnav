package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.ExpandCollapseCard
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemExpandCollapseBinding

class ExpandCollapseViewHolder(
    private val binding: ItemExpandCollapseBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<ExpandCollapseCard>(binding.root) {
    override fun bind(
        item: ExpandCollapseCard,
        position: Int,
    ) {
        val itemState = item.state
        if (itemState != null) {
            val truncated = itemState.isTruncated()
            val labelText = if (truncated) item.truncatedLabel else item.expandedLabel
            // Fallback to the default labels
            val label =
                if (labelText.isNullOrEmpty()) {
                    if (truncated) item.truncatedLabel else item.expandedLabel
                } else {
                    labelText
                }
            binding.expandCollapseButton.text = label
            val iconId = if (truncated) R.drawable.see_more_arrow else R.drawable.see_less_arrow
            binding.expandCollapseButton.setIconResource(iconId)
            binding.expandCollapseButton.setOnClickListener {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.ArticleCardExpandCollapse(item),
                )
            }
        }
    }
}
