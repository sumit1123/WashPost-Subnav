package com.washingtonpost.android.save.database.model

class ArticleAndMetadata(var id: Long, var contentURL: String) {
    var headline: String? = null
    var byline: String? = null
    var blurb: String? = null
    var imageURL: String? = null
    var publishedTime: Long? = null
    var lastUpdated: Long? = null
    var canonicalURL: String? = null
    var secondaryText: String? = null
    var displayLabel: String? = null
    var displayTransparency: String? = null
    var trackingString: String? = null
    var headlinePrefix: String? = null
    var lmt: Long? = null
    var isListened: Boolean = false
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArticleAndMetadata

        if (contentURL != other.contentURL) return false
        if (headline != other.headline) return false
        if (byline != other.byline) return false
        if (blurb != other.blurb) return false
        if (imageURL != other.imageURL) return false
        if (secondaryText != other.secondaryText) return false
        if (displayLabel != other.displayLabel) return false
        if (displayTransparency != other.displayTransparency) return false
        if (trackingString != other.trackingString) return false
        if (headlinePrefix != other.headlinePrefix) return false
        if (isListened != other.isListened) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentURL.hashCode() ?: 0
        result = 31 * result + (headline?.hashCode() ?: 0)
        result = 31 * result + (byline?.hashCode() ?: 0)
        result = 31 * result + (blurb?.hashCode() ?: 0)
        result = 31 * result + (imageURL?.hashCode() ?: 0)
        result = 31 * result + (secondaryText?.hashCode() ?: 0)
        result = 31 * result + (displayLabel?.hashCode() ?: 0)
        result = 31 * result + (displayTransparency?.hashCode() ?: 0)
        result = 31 * result + (trackingString?.hashCode() ?: 0)
        result = 31 * result + (headlinePrefix?.hashCode() ?: 0)
        result = 31 * result + (isListened.hashCode())
        return result
    }
}