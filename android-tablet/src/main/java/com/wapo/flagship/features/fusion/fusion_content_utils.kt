@file:JvmName("FusionContentUtils")

package com.wapo.flagship.features.fusion

import android.os.Build
import com.wapo.android.commons.util.toDateLong
import com.wapo.flagship.features.articles.ArticleLinkType
import com.wapo.flagship.features.grid.*
import com.wapo.flagship.features.grid.model.*
import com.wapo.flagship.model.ArticleMeta
import com.washingtonpost.android.config.domain.models.config.WebArticlesConfig

fun Grid.getArticles(
    isArticleFallbackMode: Boolean,
    webArticlesConfig: WebArticlesConfig,
): List<ArticleMeta> {
    val articleUrls = ArrayList<ArticleMeta>()
    this.regions
        .flatMap { it.items }
        .flatMap { it.items }
        .flatMap { it.items }
        .filter { it is HomepageStory || it is Carousel }
        .forEach {
            when (it) {
                is HomepageStory -> {
                    val link = it.getLink(isArticleFallbackMode)
                    if (link != null &&
                        isValidArticlesListType(
                            link.type)
                    ) {
                        val articleMeta =
                            ArticleMeta(
                                link.url,
                                false,
                                mapLinkToArticleLinkType(link.type),
                                toDateLong(link.lastModified),
                            )
                        if (articleMeta.id?.isNotEmpty() == true) {
                            articleUrls.add(articleMeta)
                        }
                    }
                }
                is Carousel -> {
                    it.items.forEach { bright ->
                        articleUrls.add(
                            ArticleMeta(
                                bright.link.url,
                                false,
                                mapLinkToArticleLinkType(bright.link.type),
                                toDateLong(bright.link.lastModified),
                            ),
                        )
                    }
                }
            }
        }
    return articleUrls
}

fun GridEntity.getArticleUrls(): List<ArticleMeta> {
    val articleUrls = ArrayList<ArticleMeta>()
    this.regions
        .flatMap { it.items }
        .filter { it is ChainEntity || it is CarouselBaseItemEntity }
        .forEach { entity ->
            when (entity) {
                is ChainEntity -> {
                    entity.items
                        .filterNotNull()
                        .flatMap { it.items }
                        .filter { it is HomepageStoryEntity || it is CarouselItemEntity }
                        .forEach { feature ->
                            when (feature) {
                                is HomepageStoryEntity -> {
                                    val linkModel = HomepageStoryMapper.getLink(feature.link)
                                    val linkModelOffline =
                                        HomepageStoryMapper.getLink(
                                            feature.offlineLink,
                                        )
                                    if (linkModelOffline != null &&
                                        isLinkDownloadable(
                                            linkModelOffline,
                                        )
                                    ) {
                                        articleUrls.add(getArticleModel(linkModelOffline))
                                    } else if (linkModel != null && isLinkDownloadable(linkModel)) {
                                        articleUrls.add(getArticleModel(linkModel))
                                    }
                                }
                                is CarouselItemEntity -> {
                                    if (feature.itemType == ItemType.CAROUSEL.toString()) {
                                        feature.items?.forEach { brightItem ->
                                            val linkModel =
                                                HomepageStoryMapper.getLink(
                                                    brightItem?.link,
                                                )
                                            if (linkModel != null && isLinkDownloadable(linkModel)) {
                                                articleUrls.add(getArticleModel(linkModel))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
                is CarouselBaseItemEntity -> {
                    entity.items?.forEach { brightItem ->
                        val linkModel = HomepageStoryMapper.getLink(brightItem?.link)
                        if (linkModel != null && isLinkDownloadable(linkModel)) {
                            articleUrls.add(getArticleModel(linkModel))
                        }
                    }
                }
            }
        }

    return articleUrls.distinct()
}

private fun getArticleModel(link: Link): ArticleMeta =
    ArticleMeta(
        link.url,
        false,
        mapLinkToArticleLinkType(link.type),
        toDateLong(link.lastModified),
    )

fun isLinkDownloadable(link: Link?): Boolean =
    link?.url != null && (link.type == LinkType.ARTICLE)

fun HomepageStory.getLink(isArticleFallbackMode: Boolean): Link? =
    if (isArticleFallbackMode && offlineLink != null && !offlineLink?.url.isNullOrEmpty()) offlineLink else link

private fun mapLinkToArticleLinkType(linkType: LinkType): ArticleLinkType =
    try {
        ArticleLinkType.valueOf(linkType.name)
    } catch (e: Exception) {
        ArticleLinkType.NONE
    }

private fun isValidArticlesListType(
    type: LinkType?,
): Boolean =
    when (type) {
        LinkType.ARTICLE -> true
        LinkType.GALLERY -> false
        LinkType.VIDEO -> false
        LinkType.WEB -> false
        LinkType.NONE -> false
        null -> false
    }
