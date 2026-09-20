/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.SectionTopperEntity
import com.wapo.flagship.features.grid.SectionTopperStyleTypeEntity

object SectionTopperMapper {

    fun getSectionTopper(entity: SectionTopperEntity, baseEntity: Boolean = false): SectionTopper {
        return SectionTopper(
            entity.id,
            getStyle(entity.style),
            entity.title,
            entity.tagline
        ).apply {
            layoutAttributes = if (entity.layoutAttributes != null && !baseEntity)
                PageModelMapper.getLayoutAttributes(entity.layoutAttributes)
            else
                PageModelMapper.createDefaultLayoutAttributes()
        }
    }

    private fun getStyle(styleEntity: SectionTopperStyleTypeEntity?): SectionTopperStyle? {
        return when (styleEntity) {
            SectionTopperStyleTypeEntity.COMMENTS -> SectionTopperStyle.COMMENTS
            else -> null
        }
    }
}