package com.wapo.flagship.features.grid.model

data class Ad(
        val commercialNode: String?,
        val adType: String?,
        val primarySectionId: String?,
        val contentType: String?,
) : Item()