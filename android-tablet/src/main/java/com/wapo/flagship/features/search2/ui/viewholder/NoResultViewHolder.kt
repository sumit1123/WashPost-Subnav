package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.model.NoResult
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchNoResultBinding

class NoResultViewHolder(
    val binding: SearchNoResultBinding,
) : Search2Adapter.SearchViewHolder<NoResult>(binding.root)
