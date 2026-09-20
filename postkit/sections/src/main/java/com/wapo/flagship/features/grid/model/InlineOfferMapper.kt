package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.InlineOfferEntity

object InlineOfferMapper {
    fun getInlineOffer(inlineOfferEntity: InlineOfferEntity): InlineOffer {
        return InlineOffer(inlineOfferEntity.id).apply {
            layoutAttributes = PageModelMapper.createDefaultLayoutAttributes()
        }
    }
}