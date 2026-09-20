package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.comments.model.SourceAnnotation
import com.wapo.flagship.features.articles2.models.ElementGroupItem
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class SanitizedHtml(
    @Json(name = "content")
    val content: String?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "arcId")
    override val arcId: String?,
    @Json(name = "subhead_level")
    val subheadLevel: Int?,
    @Json(name = "style")
    val style: String?,
    @Json(name = "oembed")
    val oembed: String?,
    @Json(name = "truncate")
    val truncate: Truncate?,

    var sourceAnnotations: List<SourceAnnotation>? = null
) : Item(type = type),
    ElementGroupItem {
    enum class SubType(
        val value: String,
    ) {
        EXPANDED_BYLINE("expanded-byline"),
    }
}
