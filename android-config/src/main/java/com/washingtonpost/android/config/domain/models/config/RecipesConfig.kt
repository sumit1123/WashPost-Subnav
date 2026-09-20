package com.washingtonpost.android.config.domain.models.config

data class RecipesConfig(
    val baseUrl: String,
    val landingPage: String,
    val filters: List<FiltersItem>,
)

data class FiltersItem(
    val queryName: String,
    val items: List<ItemsItem>,
    val group: String,
    val isMultiSelect: Boolean,
    val isQuickFilter: Boolean,
)

data class ItemsItem(
    val label: String,
    val queryId: String?,
)
