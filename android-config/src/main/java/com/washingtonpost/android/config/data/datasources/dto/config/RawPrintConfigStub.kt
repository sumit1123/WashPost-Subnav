package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.PrintConfigStub
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawPrintConfigStub(
    @Json(name = "newsstandBaseURL") val newsstandBaseURL: String? = null,
    @Json(name = "newsstandBaseURLAmazon") val newsstandBaseURLAmazon: String? = null,
    @Json(name = "awsBaseURL") val awsBaseURL: String? = null,
    @Json(name = "bundleURLTemplate") val bundleURLTemplate: String? = null,
    @Json(name = "retinaBundleURLTemplate") val retinaBundleURLTemplate: String? = null,
    @Json(name = "metadataURLTemplateLmt") val metadataURLTemplateLmt: String? = null,
    @Json(name = "frontPageImageURLTemplate") val frontPageImageURLTemplate: String? = null,
    @Json(name = "previewEnabled") val previewEnabled: Boolean? = null,
    @Json(name = "dailyDownloadEnabled") val dailyDownloadEnabled: Boolean? = null,
    @Json(name = "dailyDownloadHourUTC") val dailyDownloadHourUTC: Int? = null,
    @Json(name = "dailyDownloadMinuteUTC") val dailyDownloadMinuteUTC: Int? = null,
    @Json(name = "dailyDownloadVarianceMinutes") val dailyDownloadVarianceMinutes: Int? = null,
    @Json(name = "articleDateTimeoutHours") val articleDateTimeoutHours: Int? = null,
) {
    fun mapToDomain(params: MapConfigParams): PrintConfigStub {
        val resolvedNewsstandBaseUrl = when (params.configProvider.storeType) {
            StoreType.AMAZON -> newsstandBaseURLAmazon
            else -> this.newsstandBaseURL
        } ?: "https://rainbowdatanet-a.wpdigital.net/native/"

        return PrintConfigStub(
            newsstandBaseURL = resolvedNewsstandBaseUrl,
            awsBaseURL = awsBaseURL ?: "https://s3.amazonaws.com/wp-stat/pagestore/",
            bundleURLTemplate = (bundleURLTemplate ?: "ipad/%s/%s.zip")
                .replace("\$newsstandBaseURL", resolvedNewsstandBaseUrl),
            retinaBundleURLTemplate = (retinaBundleURLTemplate ?: "ipad-retina/%s/%s.zip")
                .replace("\$newsstandBaseURL", resolvedNewsstandBaseUrl),
            metadataURLTemplateLmt = (metadataURLTemplateLmt ?: "ipad-retina/%s/tablet.json")
                .replace("\$newsstandBaseURL", resolvedNewsstandBaseUrl),
            frontPageImageURLTemplate = (frontPageImageURLTemplate ?: "%s/%s")
                .replace("\$newsstandBaseURL", resolvedNewsstandBaseUrl),
            previewEnabled = previewEnabled ?: true,
            dailyDownloadEnabled = dailyDownloadEnabled ?: true,
            dailyDownloadHourUTC = dailyDownloadHourUTC ?: 9,
            dailyDownloadMinuteUTC = dailyDownloadMinuteUTC ?: 30,
            dailyDownloadVarianceMinutes = dailyDownloadVarianceMinutes ?: 10,
            articleDateTimeoutHours = articleDateTimeoutHours ?: 6,
        )
    }
}