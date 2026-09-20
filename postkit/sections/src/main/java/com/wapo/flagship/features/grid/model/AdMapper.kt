package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.AdBaseItemEntity
import com.wapo.flagship.features.grid.AdItemEntity
import com.wapo.flagship.features.grid.BleedEntity

object AdMapper {

    fun getAd(adEntity: AdItemEntity) : Ad {
        val ad = Ad(adEntity.advertisement?.commercialNode, adEntity.adType, adEntity.primarySectionId, adEntity.contentType)
        ad.layoutAttributes = PageModelMapper.getLayoutAttributes(adEntity.layoutAttributes)
        ad.bleed = getBleed(adEntity.bleed)

        return ad
    }

    fun getBaseAd(adBaseItemEntity: AdBaseItemEntity) : Ad {
        val ad = Ad(adBaseItemEntity.advertisement?.commercialNode, adBaseItemEntity.adType, adBaseItemEntity.primarySectionId, adBaseItemEntity.contentType)
        ad.layoutAttributes = PageModelMapper.getLayoutAttributes(adBaseItemEntity.layoutAttributes)
        ad.bleed = getBleed(adBaseItemEntity.bleed)

        return ad
    }

    private fun getBleed(bleedEntity: BleedEntity?): Bleed {
        return when (bleedEntity) {
            BleedEntity.FULL -> Bleed.FULL
            BleedEntity.CONTAINER -> Bleed.CONTAINER
            else -> Bleed.NONE
        }
    }
}