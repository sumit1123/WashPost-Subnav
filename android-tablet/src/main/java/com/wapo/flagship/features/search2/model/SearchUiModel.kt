package com.wapo.flagship.features.search2.model

import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdSlotType
import com.wapo.flagship.features.search2.state.PostAnswersUIState
import kotlin.reflect.KClass

open class SearchItem(
    var id: String = "",
    var position: ItemPosition? = null,
) : java.io.Serializable

data class HeaderItem(
    val label: String = "",
    var results: Boolean = true,
    val groupType: KClass<out SearchItem>? = null,
) : SearchItem(label)

data class SectionItem(
    val label: String = "",
    val url: String,
    val type: String? = "",
    val path: String? = "",
) : SearchItem(label)

data class SpacerItem(
    var height: Int = 10,
) : SearchItem("spacer")

data class LoaderItem(
    var height: Int = 10,
) : SearchItem("spacer")

data class NoResult(
    val label: String,
) : SearchItem(label)

data class ArticleItem(
    val headline: String = "",
    val byline: String = "",
    val imageUrl: String? = null,
    val contentUrl: String,
    val publishTime: Long?,
) : SearchItem(headline)

data class AdItem(
    val commercialNode: String,
    val adPosition: String,
    val adSlotType: AdSlotType = AdSlotType.SHORT,
    val adDimension: AdDimension = AdDimension.Medium
) : SearchItem(commercialNode)

data class RecipeItem(
    val headline: String,
    val imageUrl: String?,
    val contentUrl: String,
    val duration: Int?,
    val course: String?,
    val rating: Double?,
    val reviews: Int?,
) : SearchItem(headline)

data class ElectionItem(
    val name: String,
    val type: String?,
    val path: String?,
) : SearchItem(name)

data class ExpandableItem(
    val label: String,
    var expanded: Boolean,
) : SearchItem(label)

data class SearchQueryItem(
    val query: String = "",
) : SearchItem(query)

data class PostAnswerContainerItem(
    var state: PostAnswersUIState,
) : SearchItem()

data class AskQuestionsItem(
    val label: String,
) : SearchItem()

enum class ItemPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    ALL,
}
