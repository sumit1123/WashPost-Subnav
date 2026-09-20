package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.OlympicsMedals
import com.washingtonpost.android.databinding.ItemAnchorBinding

class AnchorViewHolder(
    private val binding: ItemAnchorBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<OlympicsMedals>(binding.root)
