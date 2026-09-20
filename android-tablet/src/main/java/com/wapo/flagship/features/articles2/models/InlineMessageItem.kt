package com.wapo.flagship.features.articles2.models

import com.wapo.flagship.features.grid.model.SectionInlineMessage

data class InlineMessageItem(
    override val type: String? = "InlineOfferType",
    val sectionInlineMessage: SectionInlineMessage?,
    val articleInlineMessage: ArticleInlineMessage?
) : Item(type)
