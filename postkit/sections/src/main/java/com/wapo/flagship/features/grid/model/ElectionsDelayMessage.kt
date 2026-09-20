package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.ElectionsDelayEntity

class ElectionsDelayMessage(
        val text: String,
        val linkText: String,
        val url: String
) : Item()

fun getElectionsDelayMessage(electionsDelayEntity: ElectionsDelayEntity): ElectionsDelayMessage {
    return ElectionsDelayMessage(
            text = electionsDelayEntity.text.orEmpty(),
            linkText = electionsDelayEntity.linkText.orEmpty(),
            url = electionsDelayEntity.url.orEmpty()
    ).apply {
        layoutAttributes = PageModelMapper.getLayoutAttributes(electionsDelayEntity.layoutAttributes)
    }
}