package com.wapo.flagship.features.search2.ui.viewholder

import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.setVisible
import com.wapo.android.commons.util.timeAgo
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.search2.model.ArticleItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.wapo.flagship.glide.GlideImageRequestFactory.Companion.NO_RESIZER
import com.washingtonpost.android.databinding.SearchArticleItemBinding

class ArticleItemViewHolder(
    val binding: SearchArticleItemBinding,
    val onItemClick: (UserEvent) -> Unit,
) : Search2Adapter.SearchViewHolder<ArticleItem>(binding.root) {
    override fun bind(item: ArticleItem) {
        super.bind(item)

        val bylineText =
            when {
                item.publishTime != null && item.byline.isNotEmpty() ->
                    "By ${item.byline} \u2022 ${
                        timeAgo(
                            item.publishTime,
                        )
                    }"
                item.byline.isNotEmpty() -> "By ${item.byline}"
                item.publishTime != null -> timeAgo(item.publishTime)
                else -> ""
            }

        binding.headline.text = item.headline
        binding.byline.text = bylineText

        val requestOption = RequestOptions().centerInside()

        binding.image.setVisible(!item.imageUrl.isNullOrEmpty())

        item.imageUrl?.let {
            if (it.isNotBlank()) {
                val glideUrl = GlideUrl(it)
                try {
                    Glide
                        .with(binding.root.context)
                        .load(glideUrl)
                        .apply(requestOption)
                        .error(
                            Glide
                                .with(binding.root.context)
                                .load(GlideUrl(it) { mapOf(NO_RESIZER to "true") })
                                .apply(requestOption),
                        ).into(binding.image)
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in Glide module. error_msg=${e.message}")
                }
            }
        }

        binding.root.setOnClickListener {
            onItemClick(UserEvent.ArticleItemClick(item, bindingAdapterPosition))
        }
    }

    override fun unbind() {
        super.unbind()
        binding.root.setOnClickListener(null)
    }

    companion object {
        val TAG = ArticleItemViewHolder::class.simpleName
    }
}
