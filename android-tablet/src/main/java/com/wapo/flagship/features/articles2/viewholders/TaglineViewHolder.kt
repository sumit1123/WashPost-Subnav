// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles2.viewholders

import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.Tagline
import com.washingtonpost.android.databinding.ItemTaglineBinding

class TaglineViewHolder(
    val binding: ItemTaglineBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Tagline>(
        binding.root,
    )
