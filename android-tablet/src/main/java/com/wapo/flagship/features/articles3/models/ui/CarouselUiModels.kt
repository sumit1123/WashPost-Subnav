package com.wapo.flagship.features.articles3.models.ui

import com.wapo.flagship.features.articles2.models.deserialized.Kicker
import com.wapo.flagship.features.comments.model.Author
import com.wapo.flagship.features.comments.model.Video
import com.washingtonpost.foryou.data.AuthorsItem
import com.washingtonpost.foryou.data.Credits

sealed interface CarouselItemUiModel

data class ForYouCarouselItemUiModel(
    val credits: Credits?,
    val headline: String?,
    val imageUrl: String?,
    val sourceType: String?,
    val label: com.washingtonpost.foryou.data.Label?,
    val secondaryLabel: String? = null,
    val isOpinions: Boolean = false,
    val recReason: String?,
    val url: String?,
    val authors: List<AuthorsItem>?,
    val articleId: String? = null,
    val contentType: String? = null,
) : CarouselItemUiModel

data class RecircCarouselItemUiModel(
    val headline: String?,
    val imageUrl: String?,
    val sourceType: String?,
    val label: com.wapo.flagship.features.articles2.models.recirculation.Label?,
    val secondaryLabel: String? = null,
    val isOpinions: Boolean = false,
    val url: String?,
    val articleId: String? = null,
    val contentType: String? = null,
    val timestamp: String? = null,
) : CarouselItemUiModel

data class InlineCarouselItemUiModel(
    val headline: String?,
    val imageUrl: String?,
    val sourceType: String?,
    val kicker: Kicker?,
    val url: String?
) : CarouselItemUiModel

//data class ArticleCarouselItemUiModel(
//    val credits: Credits?,
//    val headline: String?,
//    val imageUrl: String?,
//    val sourceType: String?,
//    val label: Label?,
//    val recReason: String?,
//    val url: String?,
//    val authors: List<AuthorsItem>?,
//    val byline: String? = null,
//    val articleId: String? = null,
//    val contentType: String? = null,
//) : CarouselItemUiModel
data class SourceCommentUiModel(
    val url: String?,
    val author: Author?,
    val body: String?,
    val video: Video?,
) : CarouselItemUiModel
