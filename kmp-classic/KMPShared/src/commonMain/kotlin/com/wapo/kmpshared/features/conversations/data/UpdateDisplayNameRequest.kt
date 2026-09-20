package com.wapo.kmpshared.features.conversations.data

import kotlinx.serialization.Serializable

@Serializable
data class UpdateDisplayNameRequest(
    val displayName: String,
)
