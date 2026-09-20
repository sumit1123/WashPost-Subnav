package com.wapo.flagship.features.search2.ui.viewholder

import com.wapo.adsinf.databinding.AdLayoutBinding
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdRequestTargets
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.flagship.features.search2.model.AdItem
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter

class AdViewHolder(
    val binding: AdLayoutBinding,
    val shouldSuppressAds: Boolean
) : Search2Adapter.SearchViewHolder<AdItem>(binding.root) {

    override fun bind(item: AdItem) {
        super.bind(item)
        if (shouldSuppressAds) {
            AdsUtil.setAdLayoutVisibility(adLayout = binding.root, isVisible = false)
            return
        }
        val adConfig = AdConfig(
            adUnitId = AdsUtil.getAdUnitId(
                context = binding.root.context,
                contentType = item.commercialNode,
                adType = item.adPosition,
                adKey = item.commercialNode,
            ),
            adRequestTargets = AdRequestTargets.getDefault().apply {
                addSlotSizeParameters(item.adSlotType)
                addSection("search")
                addAdPosition(item.adPosition)
            },
            adDimensions = listOf(item.adDimension),
            adSlotType = item.adSlotType,
            section = "search"
        )
        binding.adView.loadAd(adConfig)
    }

    override fun unbind() {
        binding.adView.removeAllViews()
        super.unbind()
    }
}