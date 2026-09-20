package com.washingtonpost.android.save.models

import android.content.res.Resources
import com.wapo.android.commons.util.ContentType
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.recirculation.carousel.models.MyPostCarouselViewItem
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.TIME_STAMP_RECENCY_THRESHOLD
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import kotlin.math.ceil

data class MyPostArticleItem(
    var contentType: ContentType? = null,
    val contentUrl: String = "",
    val headline: String? = null,
    val headlinePrefix: String? = null,
    val blurb: String? = null,
    val kicker: String? = null,
    val transparency: String? = null,
    val imageUrl: String?= null,
    val byline: String? = null,
    val dateTime: Long? = null,
    val displayDate: String? = null,
    val authorId: String? = null,
    val trackingString: String? = null,
    val deepestScrollId: String? = null,
    var listenDepthSec: Long? = null,
    var percentConsumed: Float? = null,
    var mediaId: String? = null,
    var streamUrl: String? = null,
    var label: String? = null,
)

fun mapFromArticleAndMetadata(articleAndMetadataList: List<ArticleAndMetadata?>?): List<MyPostArticleItem> {
    if (articleAndMetadataList.isNullOrEmpty()) return emptyList()
    return articleAndMetadataList.mapNotNull {
        it?.let {
            if (it.headline != null && it.byline != null) {
                MyPostArticleItem(
                    contentUrl = it.contentURL,
                    headline = it.headline,
                    headlinePrefix = it.headlinePrefix,
                    blurb = it.blurb,
                    kicker = it.displayLabel,
                    transparency = it.displayTransparency,
                    imageUrl = it.imageURL,
                    byline = it.byline,
                    dateTime = it.lastUpdated ?: it.publishedTime,
                    displayDate = null,
                    authorId = null,
                    trackingString = it.trackingString,
                    contentType = ContentType.ARTICLE
                )
            } else null
        }
    }.toList()
}

fun MyPostArticleItem.roundUpPercentageConsumed(resources: Resources): String? {
    percentConsumed?.let {
        if (it == 0f) return null
        return resources.getString(R.string.pick_up_where_you_left_off_percentage_completed, (ceil(it * 100.0)).toInt())
    }
    return null
}

fun mapFromAuthorItem(authorItems: List<AuthorItem>, limit: Int): List<MyPostArticleItem> {
    if (authorItems.isNullOrEmpty()) return emptyList()
    val result = mutableListOf<MyPostArticleItem>()
    authorItems.forEach { author ->
        author.items
            ?.take(limit)
            ?.map {
                MyPostArticleItem(
                    contentUrl = it.url,
                    headline = it.headline,
                    headlinePrefix = null,
                    blurb = it.blurb,
                    kicker = null,
                    transparency = null,
                    imageUrl = it.image,
                    byline = it.byline,
                    dateTime = it.lmt,
                    displayDate = it.displayDate,
                    authorId = author.id,
                    trackingString = null,
                    contentType = ContentType.ARTICLE
                )
            }
            ?.also { result.addAll(it) }
    }

    return result
}

fun MyPostArticleItem.toMyPostCarouselViewItem(): MyPostCarouselViewItem {
    if (contentType == ContentType.PODCAST) {
        return MyPostCarouselViewItem(
            mediaId = mediaId,
            headline = headline,
            listenDepthSec = listenDepthSec,
            streamUrl = streamUrl,
            percentConsumed = percentConsumed,
            contentType = contentType,
            mByline = label
        )
    } else {
        return MyPostCarouselViewItem(
            contentUrl,
            headline,
            headlinePrefix,
            kicker,
            transparency,
            imageUrl,
            byline,
            dateTime,
            TIME_STAMP_RECENCY_THRESHOLD,
            listenDepthSec = listenDepthSec,
            percentConsumed = percentConsumed,
            contentType = contentType
        )
    }
}
