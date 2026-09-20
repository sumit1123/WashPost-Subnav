package com.wapo.flagship.features.grid.model

data class Link(
        val type: LinkType,
        val url: String,
        val accessLevel: String? = null,
        val lastModified: String? = null,
        val displayDate: String? = null,
        val subtype: String? = null,
        val itId: String? = null
)

enum class LinkType {
    ARTICLE,
    GALLERY,
    VIDEO,
    WEB,
    NONE,
}