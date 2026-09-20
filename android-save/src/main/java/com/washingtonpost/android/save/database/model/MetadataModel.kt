package com.washingtonpost.android.save.database.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    indices = [
        Index(value = ["syncLmt"]),
        Index(value = ["canonicalURL"]),
    ],
    primaryKeys = [
        "contentURL",
    ],
)
data class MetadataModel(
    var contentURL: String,
    var syncLmt: Long,
) {
    var headline: String? = null
    var byline: String? = null
    var blurb: String? = null
    var imageURL: String? = null
    var canonicalURL: String? = null
    var lastUpdated: Long? = null
    var publishedTime: Long? = null
    var secondaryText: String? = null
    var displayLabel: String? = null
    var displayTransparency: String? = null
    var trackingString: String? = null
    var headlinePrefix: String? = null
}
