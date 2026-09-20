package com.washingtonpost.android.paywall.newdata.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ArticleStub(
    var title: String? = null,
    var url: String? = null,
    var arcId: String? = null,
    var section: String? = null,
    var contentRestrictionCode: String? = null,
    var timeStamp: Long? = 0,
    var contentSection: String? = null,
    var ctTags: String? = null,
    var referrer: String? = null,
    var tetroUtm: String? = null,
    var commercialNode: String? = null,
    var tetroAuthors: String? = null,
    var publishedDate: Long? = null,
    var displayDate: Long? = null,
    var contentType: String? = null,
    var tetroSubtype: String? = null
) :
    Parcelable {

    val isFreeContent: Boolean
        get() = CONTENT_RESTRICTION_FREE == contentRestrictionCode
    val isSubOnlyContent: Boolean
        get() = CONTENT_RESTRICTION_SUBSCRIBER == contentRestrictionCode

    companion object {
        const val CONTENT_RESTRICTION_FREE = "free"
        const val CONTENT_RESTRICTION_SUBSCRIBER = "subscriber-only"
    }
}
