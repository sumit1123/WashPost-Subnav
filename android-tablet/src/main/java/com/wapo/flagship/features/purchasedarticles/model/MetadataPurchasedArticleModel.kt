package com.wapo.flagship.features.purchasedarticles.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.washingtonpost.android.save.network.MetadataLabel

@Entity(tableName = "metadataPurchasedArticles")
data class MetadataPurchasedArticleModel(
    @PrimaryKey()
    val url: String,
    val canonicalUrl: String,
    val headline: String,
    val byLine: String,
    val displayDate: String,
    val socialImageUrl: String,
    val description: String,
    val label: MetadataLabel,
    val lastUpdated: String,
    val contentRestrictionCode: String,
    val shouldOpenWebView: Boolean,
    val purchasedDate: Long
)