package com.wapo.flagship.features.articles2.viewholders

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.LinkButton
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemLinkButtonBinding

class LinkButtonViewHolder(
    private val binding: ItemLinkButtonBinding,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<LinkButton>(binding.root) {
    private val buttonIconDrawable =
        ContextCompat.getDrawable(itemView.context, R.drawable.ic_right_chevron)

    override fun bind(
        item: LinkButton,
        position: Int,
    ) {
        binding.buttonLink.icon = if (item.showArrow) buttonIconDrawable else null
        binding.buttonLink.text = item.label
        binding.buttonLink.setOnClickListener {
            if (item.subtype == LinkButton.SubType.BUTTON_OUTCOME.value) {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.LufOutcomePostClickEvent(item.url),
                )
            } else {
                articlesInteractionHelper.onEventFired(
                    ArticleInteractionEvent.LinkClickEvent(item.url),
                )
            }
        }
        if (item.subtype == LinkButton.SubType.BUTTON_OUTCOME.value) {
            binding.buttonLink.apply {
                setBackgroundResource(com.washingtonpost.android.articles.R.drawable.rounded_button_style_1)
                setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.live_outcome_textcolor),
                )
                backgroundTintList = null
                iconTint =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(binding.root.context, R.color.live_outcome_textcolor),
                    )
            }
        } else {
            binding.buttonLink.apply {
                setBackgroundResource(com.washingtonpost.android.articles.R.drawable.rounded_button_inverted_style)
                setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        com.washingtonpost.android.articles.R.color.rounded_corner_button_inverted_text,
                    ),
                )
                backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            binding.root.context,
                            com.washingtonpost.android.articles.R.color.rounded_corner_button_inverted_background,
                        ),
                    )
            }
        }
    }
}
