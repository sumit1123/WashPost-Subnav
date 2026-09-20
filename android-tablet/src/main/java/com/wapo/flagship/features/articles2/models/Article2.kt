package com.wapo.flagship.features.articles2.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.base.Cacheable
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.features.articles2.typeconverters.*

@JsonClass(generateAdapter = true)
@Entity(tableName = "articles")
data class Article2(
    @Json(name = "version")
    @ColumnInfo(name = "version")
    val version: Int? = null,
    @Json(name = "comments")
    @ColumnInfo(name = "comments")
    val comments: String? = null,
    @Json(name = "arcId")
    @ColumnInfo(name = "article_arcId")
    val arcId: String? = null,
    @Json(name = "blogname")
    @ColumnInfo(name = "blogname")
    val blogname: String? = null,
    @Json(name = "blurb")
    @ColumnInfo(name = "blurb")
    val blurb: String? = null,
    @Json(name = "commercialnode")
    @ColumnInfo(name = "commercialnode")
    val commercialnode: String? = null,
    @Json(name = "content_restriction_code")
    @ColumnInfo(name = "content_restriction_code")
    val contentRestrictionCode: String? = null,
    @Json(name = "contenturl")
    @ColumnInfo(name = "contenturl")
    @PrimaryKey(autoGenerate = false)
    val contenturl: String,
    @Json(name = "dataServiceAdaptor")
    @ColumnInfo(name = "dataServiceAdaptor")
    val dataServiceAdaptor: String? = null,
    @Json(name = "editorpicks")
    @TypeConverters(EditorPickListTypeConverter::class)
    @ColumnInfo(name = "editorpicks")
    val editorpicks: List<Editorpick>? = null,
    @Json(name = "first_published")
    @ColumnInfo(name = "first_published")
    val firstPublished: Long? = null,
    @Json(name = "id")
    @ColumnInfo(name = "id")
    val id: String? = null,
    @Json(name = "items")
    @TypeConverters(ItemListTypeConverter::class)
    @ColumnInfo(name = "items")
    val items: List<Item>? = null,
    @Json(name = "lmt")
    @ColumnInfo(name = "lmt")
    val lmt: Long? = null,
    @Json(name = "omniture")
    @TypeConverters(OmnitureXTypeConverter::class)
    @ColumnInfo(name = "omniture")
    val omniture: OmnitureX? = null,
    @Json(name = "published")
    @ColumnInfo(name = "published")
    val published: Long? = null,
    @Json(name = "display_date")
    @ColumnInfo(name = "display_date")
    val displayDate: Long? = null,
    @Json(name = "section")
    @ColumnInfo(name = "section")
    val section: String? = null,
    @Json(name = "shareurl")
    @ColumnInfo(name = "shareurl")
    val shareurl: String? = null,
    @Json(name = "socialImage")
    @ColumnInfo(name = "socialImage")
    val socialImage: String? = null,
    @Json(name = "source")
    @ColumnInfo(name = "source")
    val source: String? = null,
    @Json(name = "sourcecategory")
    @ColumnInfo(name = "sourcecategory")
    val sourcecategory: String? = null,
    @Json(name = "sourcesection")
    @ColumnInfo(name = "sourcesection")
    val sourcesection: String? = null,
    @Json(name = "sourceslug")
    @ColumnInfo(name = "sourceslug")
    val sourceslug: String? = null,
    @Json(name = "sourcesubsection")
    @ColumnInfo(name = "sourcesubsection")
    val sourcesubsection: String? = null,
    @Json(name = "tags")
    @ColumnInfo(name = "tags")
    val tags: String? = null,
    @Json(name = "taxonomy")
    @TypeConverters(TaxonomyTypeConverter::class)
    @ColumnInfo(name = "taxonomy")
    val taxonomy: Taxonomy? = null,
    @Json(name = "targeting")
    @TypeConverters(TargetingTypeConverter::class)
    @ColumnInfo(name = "targeting")
    val targeting: Targeting? = null,
    @Json(name = "title")
    @ColumnInfo(name = "title")
    val title: String? = null,
    @Json(name = "type")
    @ColumnInfo(name = "type")
    val type: String? = null,
    @Json(name = "content_type")
    @ColumnInfo(name = "content_type")
    val contentType: String? = null,
    @Json(name = "subtype")
    @ColumnInfo(name = "subtype")
    val subtype: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null,
    @ColumnInfo(name = "ttl")
    val ttl: Long? = null,
    @Json(name = "adKey")
    @ColumnInfo(name = "adKey1")
    val adKey: String? = null,
    @Json(name = "adkey")
    @ColumnInfo(name = "adkey")
    val adkey: String? = null,
    @Json(name = "renderer")
    @TypeConverters(RendererTypeConverter::class)
    @ColumnInfo(name = "renderer")
    val renderer: Renderer? = null,
    @Json(name = "tableOfContents")
    @TypeConverters(TableOfContentsTypeConverter::class)
    @ColumnInfo(name = "tableOfContents")
    val tableOfContents: TableOfContents? = null,
    @Json(name = "audio")
    @TypeConverters(AudioTypeConverter::class)
    @ColumnInfo(name = "audio")
    val audio: Audio? = null,
    @Json(name = "isLive")
    @ColumnInfo(name = "isLive")
    val isLive: Boolean? = false,
    @Json(name = "poll_frequency")
    @ColumnInfo(name = "poll_frequency")
    val pollFrequency: Int? = 60,
    @Json(name = "summary")
    @TypeConverters(SummaryTypeConverter::class)
    @ColumnInfo(name = "summary")
    val summary: Summary? = null,
    @Json(name = "show_summary")
    @ColumnInfo(name = "show_summary")
    val showSummary: Boolean? = null,
    @TypeConverters(ATPQuestionTypeConverter::class)
    @Json(name = "atp_questions")
    @ColumnInfo(name = "atp_questions")
    val atpQuestions: ATPQuestions? = null,
    @TypeConverters(FtsCarouselTypeConverter::class)
    @Json(name = "fts_carousel")
    @ColumnInfo(name = "fts_carousel")
    val ftsCarousel: FtsCarousel? = null,
    @Json(name = "showCarousel")
    @ColumnInfo(name = "showCarousel")
    val showCarousel: Boolean? = true,
    @TypeConverters(DisclaimerInfoTypeConverter::class)
    @Json(name = "disclaimer_info")
    @ColumnInfo(name = "disclaimer_info")
    val disclaimerInfo: DisclaimerInfo? = null,
) : Cacheable {
    override fun getTimeToLive(): Long = ttl ?: 0L

    override fun lastUpdated(): Long = updatedAt ?: 0L

    fun isCardified(): Boolean = items?.any { it.group != null } ?: false
}
