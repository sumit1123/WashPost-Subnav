package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.SignatureDateFormat

data class Signature(
        val byLine: String?,
        val alignment: Alignment?,
        val section: String?,
        val timestamp: String?,
        val recencyThreshold: Long,
        val ratingCharacter: String?,
        val rating: Int?,
        val dateFormat: SignatureDateFormat?
)