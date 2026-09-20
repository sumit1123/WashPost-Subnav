package com.washingtonpost.android.save.database.model

import androidx.room.Entity

/**
 * This is how Room handles partial updates in order to perform an upsert (update or insert)
 * without erasing the fields which are marked null or blank. This is necessary because
 * FOR_YOU has fields not contained in DEFAULT and we don't want to erase them. In addition,
 * we don't want to update metadata from the for you feed if the url already exists. This
 * approach could be extended to other types if needed.
 */
@Entity
class DefaultMetadataUpdate(
        var contentURL: String,
        var syncLmt: Long,
        var headline: String?,
        var byline: String?,
        var blurb: String?,
        var imageURL: String?,
        var canonicalURL: String?,
        var lastUpdated: Long?,
        var publishedTime: Long?
)

fun MetadataModel.toDefault(): DefaultMetadataUpdate {
    return DefaultMetadataUpdate(contentURL, syncLmt, headline, byline, blurb,
            imageURL, canonicalURL, lastUpdated, publishedTime)
}

enum class MetadataUpdateType {
    DEFAULT
}