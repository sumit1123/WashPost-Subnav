/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.data

import com.wapo.flagship.features.section.models.ArticleMeta
import com.wapo.flagship.features.grid.ChainEntity
import com.wapo.flagship.features.grid.HomepageStoryEntity
import com.wapo.flagship.features.grid.RegionEntity
import com.wapo.flagship.features.grid.model.HomepageStoryMapper
import com.wapo.flagship.features.sections.model.LinkType as PageBuilderLinkType
import com.wapo.flagship.features.grid.model.LinkType as FusionLinkType
import com.wapo.flagship.features.sections.model.*
import com.wapo.flagship.utils.DateUtils
import kotlin.collections.ArrayList


object WearDataManager {
    private const val TAG = "WearDataManager"

    /**
     * Retrieves a list of [ArticleMeta] out of the specified list of
     * PageBuilder [Item]s.
     *
     * @param items a list of PageBuilder [Item]s
     * @return the resulting list of [ArticleMeta]
     */
    fun getPageBuilderArticleMetaList(items: List<Item>?): List<ArticleMeta> {
        val homepageStories = ArrayList<ArticleMeta>()
        getPageBuilderArticleMetaListHelper(items, homepageStories)
        return homepageStories
    }

    /**
     * Helps retrieves a list of [ArticleMeta] out of the specified
     * list of PageBuilder [Item]s by recursively going into nested
     * [Container].
     *
     * Note: This function is needed to make the signature of
     * [getPageBuilderArticleMetaList] consistent to
     * [getFusionArticleMetaList].
     *
     * @param items a list of PageBuilder [Item]s
     * @param homepageStories the resulting mutable list of [ArticleMeta]
     */
    private fun getPageBuilderArticleMetaListHelper(
        items: List<Item>?,
        homepageStories: MutableList<ArticleMeta>
    ) {
        if (items == null) return
        for (item in items) {
            if (item is Container) {
                getPageBuilderArticleMetaListHelper(item.items, homepageStories)
            } else if (item is HomepageStory) {
                val link = item.link
                val headline = item.headline
                val signature = item.signature
                val imageUrl = item.media?.url
                if (headline != null && link?.type == PageBuilderLinkType.ARTICLE) {
                    homepageStories.add(
                        ArticleMeta(
                            headline.text,
                            link.url,
                            item.blurb,
                            signature?.timestamp,
                            signature?.byLine,
                            imageUrl,
                            DateUtils.toDateLong(link.lastModified)
                        )
                    )
                }
            } else if (item is Feature) {
                val baseFeatureItems = item.items
                for (baseFeatureItem in baseFeatureItems) {
                    if (baseFeatureItem is FeatureItem) {
                        val headline = baseFeatureItem.headline
                        val link = baseFeatureItem.link
                        val signature = baseFeatureItem.signature
                        val imageUrl = baseFeatureItem.media?.url
                        if (headline != null && link?.type == PageBuilderLinkType.ARTICLE) {
                            homepageStories.add(
                                ArticleMeta(
                                    headline.text,
                                    link.url,
                                    baseFeatureItem.blurbText,
                                    signature?.timestamp,
                                    signature?.byLine,
                                    imageUrl,
                                    DateUtils.toDateLong(link.lastModified)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves a list of [ArticleMeta] out of the specified list of
     * Fusion [RegionEntity]s.
     *
     * @param regions a list of Fusion [RegionEntity]s
     * @return the resulting list of [ArticleMeta]
     */
    fun getFusionArticleMetaList(regions: List<RegionEntity>): List<ArticleMeta> {
        val fusionHomepageStories = ArrayList<ArticleMeta>()
        regions
            .flatMap { it.items }
            .filterIsInstance<ChainEntity>()
            .forEach { entity ->
                entity.items.filterNotNull()
                    .flatMap { it.items }
                    .filterIsInstance<HomepageStoryEntity>()
                    .forEach { feature ->
                        val homepageStory = HomepageStoryMapper
                            .getHomepageStoryModel(feature, null, null)
                        val link = HomepageStoryMapper.getLink(feature.link)
                        if (homepageStory.headline != null
                            && link?.type == FusionLinkType.ARTICLE
                            && link.subtype != "interactive"
                        ) {
                            val blurb =
                                if (homepageStory.blurbs?.items.isNullOrEmpty()) null
                                else homepageStory.blurbs!!.items!!.first().text
                            val imageUrl = homepageStory.media?.url
                            val articleMeta = ArticleMeta(
                                homepageStory.headline!!.text,
                                link.url,
                                blurb,
                                homepageStory.signature?.timestamp,
                                homepageStory.signature?.byLine,
                                imageUrl,
                                DateUtils.toDateLong(link.lastModified)
                            )
                            fusionHomepageStories.add(articleMeta)
                        }
                    }
            }

        return fusionHomepageStories.distinct()
    }
}