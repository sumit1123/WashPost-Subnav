/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.model

data class ExternalTts(
    val ttsList: List<String> = listOf(),
    val title: String? = null
)
