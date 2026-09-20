package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.flagship.features.search2.model.LoaderItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter
import com.washingtonpost.android.databinding.SearchLoaderBinding

class LoadingViewHolder(
    val binding: SearchLoaderBinding,
) : Search2Adapter.SearchViewHolder<LoaderItem>(binding.root)
