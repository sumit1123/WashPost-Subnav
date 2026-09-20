package com.washingtonpost.android.config.domain.models.config

data class PrintConfigStub(
    val newsstandBaseURL: String,
    val awsBaseURL: String,
    val bundleURLTemplate: String,
    val retinaBundleURLTemplate: String,
    val metadataURLTemplateLmt: String,
    val frontPageImageURLTemplate: String,
    val previewEnabled: Boolean,
    val dailyDownloadEnabled: Boolean,
    val dailyDownloadHourUTC: Int,
    val dailyDownloadMinuteUTC: Int,
    val dailyDownloadVarianceMinutes: Int,
    val articleDateTimeoutHours: Int,
)