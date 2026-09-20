package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.InlinePlayerEntity

object InlinePlayerMapper {

    fun getInlinePlayer(inlinePlayerEntity: InlinePlayerEntity?): InlinePlayer? {
        inlinePlayerEntity ?: return null
        return InlinePlayer(
            listen = inlinePlayerEntity.listen
        )
    }
}