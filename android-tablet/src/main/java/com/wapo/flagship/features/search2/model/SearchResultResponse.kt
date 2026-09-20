package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResultResponse(
    @Json(name = "criteria")
    val criteria: Criteria,
    @Json(name = "results")
    val results: Results,
    @Json(name = "filters")
    val filters: Filters? = null,
    @Json(name = "source")
    val source: String?,
)

@JsonClass(generateAdapter = true)
data class Criteria(
    @Json(name = "count")
    val count: Int,
    @Json(name = "offset")
    val offset: Int,
    @Json(name = "query")
    val query: String,
    @Json(name = "startingrow")
    val startingrow: Int,
)

@JsonClass(generateAdapter = true)
data class Results(
    @Json(name = "documents")
    val documents: List<Document>,
    @Json(name = "total")
    val total: Int,
)

@JsonClass(generateAdapter = true)
data class Document(
    @Json(name = "blurb")
    val blurb: String?,
    @Json(name = "byline")
    val byline: String?,
    @Json(name = "contenttype")
    val contenttype: String?,
    @Json(name = "contenturl")
    val contenturl: String?,
    @Json(name = "displaydatetime")
    val displaydatetime: Double? = null,
    @Json(name = "headline")
    val headline: String?,
    @Json(name = "smallthumburl")
    val smallthumburl: String?,
    @Json(name = "systemid")
    val systemid: String?,
    @Json(name = "recipeinfo")
    val recipeInfo: RecipeInfo? = null,
    @Json(name = "rating")
    val rating: Rating? = null,
)

@JsonClass(generateAdapter = true)
data class RecipeInfo(
    @Json(name = "courses")
    val courses: List<Course?>?,
    @Json(name = "totaltime")
    val totalTime: Int? = 0,
)

@JsonClass(generateAdapter = true)
data class Rating(
    @Json(name = "count")
    val count: Int?,
    @Json(name = "max")
    val max: Double?,
    @Json(name = "type")
    val type: String?,
    @Json(name = "value")
    val value: Double?,
)

@JsonClass(generateAdapter = true)
data class Course(
    @Json(name = "description")
    val description: String?,
    @Json(name = "id")
    val id: String?,
)

@JsonClass(generateAdapter = true)
data class Filters(
    @Json(name = "new_section")
    val sections: List<SectionFilter>? = null,
    @Json(name = "author")
    val authors: List<AuthorFilter>? = null,
)

@JsonClass(generateAdapter = true)
data class SectionFilter(
    @Json(name = "name")
    val name: String?,
    @Json(name = "count")
    val count: Int,
)

@JsonClass(generateAdapter = true)
data class AuthorFilter(
    @Json(name = "name")
    val name: String?,
    @Json(name = "count")
    val count: Int,
)

data class Section(
    val url: String,
    val name: String,
    val type: String? = "",
    val path: String,
)
