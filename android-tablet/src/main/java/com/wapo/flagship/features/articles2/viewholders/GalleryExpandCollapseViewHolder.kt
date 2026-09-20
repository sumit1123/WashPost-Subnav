package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.GalleryExpandCollapse
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemExpandCollapseBinding

class GalleryExpandCollapseViewHolder(
    private val binding: ItemExpandCollapseBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<GalleryExpandCollapse>(binding.root) {
    override fun bind(
        item: GalleryExpandCollapse,
        position: Int,
    ) {
        val itemState = item.state
        val itemsCount = itemState?.truncate?.itemsCount ?: 0
        if (itemState != null && itemsCount > 0) {
            val truncated = itemState.isTruncated()
            binding.expandCollapseButton.text =
                if (truncated) "Show $itemsCount More Photos" else null
            val iconId = if (truncated) R.drawable.see_more_arrow else R.drawable.see_less_arrow
            binding.expandCollapseButton.setIconResource(iconId)
            binding.expandCollapseButton.setOnClickListener {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.ArticleGalleryExpandCollapse(item),
                )
            }
            binding.expandCollapseButton.visibility = View.VISIBLE
        } else {
            binding.expandCollapseButton.visibility = View.GONE
        }
    }
}
