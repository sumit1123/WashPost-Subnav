package com.wapo.flagship.features.grid.model

data class RelatedLinks(
        val items: List<RelatedLinkItem>? = null,
        val info: RelatedLinksInfo? = null,
        val compoundLabel: CompoundLabel? = null
)

data class RelatedLinkItem(
        val text: String? = null,
        val link: String? = null,
        val type: LinkType = LinkType.ARTICLE
)

class RelatedLinksInfo(
        val size: Size?,
        val position: Position?,
        val arrangement: Arrangement?

)

enum class Position {
    BOTTOM,
    BELOW_SIGLINE,
}

enum class Arrangement {
    NORMAL,
    SIDE_BY_SIDE,
    SIDE_BY_SIDE_PIPES,
}