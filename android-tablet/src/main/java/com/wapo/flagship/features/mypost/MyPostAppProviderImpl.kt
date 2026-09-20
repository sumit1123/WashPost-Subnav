// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.features.mypost

import android.content.Context
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.utils.appendTrackingParams
import com.wapo.flagship.model.ArticleMeta
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.save.models.MyPostArticleItem
import com.washingtonpost.android.save.types.MyPostSection
import java.text.SimpleDateFormat
import java.util.Locale

internal fun openArticles(
    context: Context?,
    tabName: String,
    myPostArticleItems: List<MyPostArticleItem>?,
    url: String,
    currentSection: String,
    sectionDisplayName: String,
    carouselOriginated: Boolean,
    isPreviewHeroArticle: Boolean,
    positionInCarousel: Int? = null,
    shouldNotSuppressPageView: Boolean = false,
    shouldPlayAudioArticle: Boolean = false
) {
    context ?: return
    val urls = myPostArticleItems?.map(MyPostArticleItem::contentUrl) ?: emptyList()
    val urlIndex = urls.indexOf(url)
    if (urlIndex < 0) {
        return
    }

    val articleMetas = mutableListOf<ArticleMeta>()
    myPostArticleItems?.forEach {
        val meta = ArticleMeta()
        meta.deepestScrollId = it.deepestScrollId
        val itId = when {
            carouselOriginated -> {
                String.format(
                    Measurement.PATH_TO_VIEW_MY_POST_CAROUSEL,
                    sectionDisplayName.lowercase(Locale.getDefault()),
                    "_${urls.indexOf(it.contentUrl).plus(1)}"
                )
            }
            isPreviewHeroArticle -> {
                String.format(
                    Measurement.PATH_TO_VIEW_MY_POST_ARTICLE_TOP,
                    sectionDisplayName.lowercase(Locale.getDefault()),
                )
            }
            else -> {
                String.format(
                    Measurement.PATH_TO_VIEW_MY_POST_ARTICLE_DETAIL,
                    sectionDisplayName.lowercase(Locale.getDefault()),
                    "_${urls.indexOf(it.contentUrl).plus(1)}",
                )
            }
        }
        meta.id = appendTrackingParams(it.contentUrl, itId,null)

        it.listenDepthSec?.let { listenDepth -> meta.listenDepthSec = listenDepth }
        articleMetas.add(meta)
    }

    val intent =
        ArticlesParcel
            .builder()
            .setArticleMetas(articleMetas.toList(), urlIndex)
            .setTabName(tabName)
            .setSectionDisplayName(sectionDisplayName)
            .setCarouselOriginated(carouselOriginated)
            .setShouldNotSuppressPageView(shouldNotSuppressPageView)
            .setPlayAudioArticle(shouldPlayAudioArticle)
    when {
        carouselOriginated -> {
            val trackingString =
                if (sectionDisplayName != "FOR_YOU" && sectionDisplayName != "PURCHASE") {
                    ""
                } else if (myPostArticleItems?.get(urlIndex)?.trackingString == null) {
                    ""
                } else {
                    "_${myPostArticleItems[urlIndex].trackingString}"
                }
            val navigationBehavior = String.format(
                Measurement.PATH_TO_VIEW_MY_POST_CAROUSEL,
                sectionDisplayName.lowercase(Locale.getDefault()),
                trackingString,
            )
            intent.setNavigationBehavior(
                navigationBehavior,
            )
            intent.setPositionInMyPostCarousel(positionInCarousel)
        }

        isPreviewHeroArticle -> {
            val navigationBehavior = String.format(
                Measurement.PATH_TO_VIEW_MY_POST_ARTICLE_TOP,
                sectionDisplayName.lowercase(Locale.getDefault()),
            )
            intent.setNavigationBehavior(
                navigationBehavior,
            )
            intent.setPositionInMyPostCarousel(positionInCarousel)
        }

        else -> {
            val trackingString =
                if (sectionDisplayName == MyPostSection.PURCHASE.name) {
                    "_${urlIndex.plus(1)}"
                } else if (sectionDisplayName != "FOR_YOU") {
                    ""
                } else if (myPostArticleItems?.get(urlIndex)?.trackingString == null) {
                    ""
                } else {
                    "${sectionDisplayName}_${urlIndex.plus(1)}"
                }
            val navigationBehavior = String.format(
                Measurement.PATH_TO_VIEW_MY_POST_ARTICLE_DETAIL,
                sectionDisplayName.lowercase(Locale.getDefault()),
                trackingString,
            )
            intent.setNavigationBehavior(
                navigationBehavior
            )
        }
    }
    intent.setArticleUrls(
        urls,
        urlIndex
    )
    context.startActivity(intent.buildIntent(context))
}

internal fun getDate(
    millis: Long?,
    format: String,
): String? =
    try {
        SimpleDateFormat(format, Locale.US).run {
            format(millis)
                .replace("AM", "a.m")
                .replace("PM", "p.m")
        }
    } catch (e: Exception) {
        null
    }
