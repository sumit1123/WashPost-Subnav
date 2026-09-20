/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.base.Cacheable
import com.wapo.flagship.features.articles2.typeconverters.*

@JsonClass(generateAdapter = true)
@Entity(tableName = "articles")
data class Article2(
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

    @Json(name = "title")
    @ColumnInfo(name = "title")
    val title: String? = null,

    @Json(name = "type")
    @ColumnInfo(name = "type")
    val type: String? = null,

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

    ) : Cacheable {

    override fun getTimeToLive(): Long {
        return ttl ?: 0L
    }

    override fun lastUpdated(): Long {
        return updatedAt ?: 0L
    }
}