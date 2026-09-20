package com.washingtonpost.android.config.domain.models.config

data class ElectionConfig(
    val baseUrl: String,
    val landingPage: String,
    val links: List<LinksItems>,
)

data class LinksItems(
    val id: String,
    val name: String,
    val type: String,
    val path: String,
)