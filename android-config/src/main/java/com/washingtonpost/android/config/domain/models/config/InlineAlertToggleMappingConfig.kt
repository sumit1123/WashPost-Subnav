package com.washingtonpost.android.config.domain.models.config

data class InlineAlertToggleMappingConfig(
    var alertSegmentKey: String?,
    var includedSourceSections: List<String>,
    var excludedSourceSubsections: List<String>
)