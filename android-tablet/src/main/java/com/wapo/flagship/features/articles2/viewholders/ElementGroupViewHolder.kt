package com.wapo.flagship.features.articles2.viewholders

import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.ElementGroup
import com.wapo.flagship.features.articles2.models.deserialized.ElementGroupLinkBox
import com.washingtonpost.android.articles.R
import com.washingtonpost.android.databinding.ItemElementGroupBinding

class ElementGroupViewHolder(
    private val binding: ItemElementGroupBinding,
    private val adapter: Articles2ItemsRecyclerViewAdapter,
    private val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<ElementGroup>(binding.root) {
    private val linkBoxViewContainer =
        binding.root.findViewById<ViewGroup>(R.id.element_group_link_box_container)
    private val elementGroupContainer =
        binding.root.findViewById<CardView>(R.id.element_group_container)

    override fun bind(
        item: ElementGroup,
        position: Int,
    ) {
        elementGroupContainer.setCardBackgroundColor(
            ContextCompat.getColor(
                itemView.context,
                R.color.element_group_bg,
            ),
        )

        linkBoxViewContainer.visibility = View.GONE

        when (item) {
            is ElementGroupLinkBox -> {
                ElementLinkBoxBinder(
                    linkBoxViewContainer,
                    adapter.elementGroupStyleHelper,
                    adapter.onGroupToggleClicked,
                    adapter.expandedItemLookUp,
                    articlesInteractionHelper,
                ).apply { this.bind(item, position) }
                linkBoxViewContainer.visibility = View.VISIBLE
            }

            else -> {
                /* no-op */
            }
        }
    }
}
