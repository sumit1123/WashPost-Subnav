package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.model.SpacerItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchSpacerBinding

class SpacerViewHolder(
    val binding: SearchSpacerBinding,
) : Search2Adapter.SearchViewHolder<SpacerItem>(binding.root)
