/*
 * Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.flagship.features.articles.recirculation

import com.wapo.android.commons.util.Logger
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.features.articles.models.ArticlesRecirculationArticleModelItem
import com.wapo.flagship.features.articles.recirculation.model.MostReadElement
import com.wapo.flagship.features.articles.recirculation.model.MostReadFeed
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.models.deserialized.RecirculationType
import com.wapo.flagship.features.articles2.utils.KickerStyleHelper.getStyle
import com.wapo.flagship.features.audio.service2.media.extensions.containsCaseInsensitive
import com.wapo.flagship.model.Status
import com.wapo.flagship.querypolicies.Query
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import rx.Observable
import rx.android.schedulers.AndroidSchedulers

class RecirculationStorage(
    private val contentManager: ContentManager,
) {
    private val cache = mutableMapOf<String, List<CarouselViewItem>>()
    private val wapoHomepage = "https://www.washingtonpost.com"
    private val TAG = "RecircStorage"

    fun getCarouselItems(
        sectionName: String,
        type: ArticlesRecirculationArticleModelItem.Type,
    ): Observable<List<CarouselViewItem>> {
        if (cache[sectionName] != null) {
            return Observable.just(cache[sectionName])
        }

        return when (type) {
            ArticlesRecirculationArticleModelItem.Type.MOST_READ ->
                mostReadCarouselItems(
                    sectionName,
                )
        }
    }

    // new articles function
    fun getCarouselItems(
        sectionName: String,
        type: RecirculationType,
    ): Observable<List<CarouselViewItem>> {
        if (cache[sectionName] != null) {
            return Observable.just(cache[sectionName])
        }

        return when (type) {
            RecirculationType.MOST_READ -> mostReadCarouselItems(sectionName)
        }
    }

    private fun mostReadCarouselItems(sectionName: String): Observable<List<CarouselViewItem>> {
        val hasValue = contentManager.mostReadFeedSubj.hasValue()
        return contentManager
            .mostReadFeedSubj
            .doOnSubscribe {
                if (!hasValue) {
                    contentManager
                        .mostReadFeed
                        .subscribe(
                            {
                                Logger.d(TAG, "contentManager.mostReadFeed prefetched")
                            },
                            {
                                Logger.d(
                                    TAG,
                                    "contentManager.mostReadFeed error prefetching",
                                    it,
                                )
                            },
                        )
                }
            }.flatMap {
                val list = getCarouselViewItemList(it, sectionName)
                if (list.isEmpty()) {
                    Observable.error(
                        RuntimeException("Recirculation: error getting : $sectionName"),
                    )
                } else {
                    cache[sectionName] = list
                    prefetchArticles(list, sectionName)
                    Observable.just(list.toList())
                }
            }.observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                cache.remove(sectionName)
            }
    }

    private fun getCarouselViewItemList(
        mostReadFeed: MostReadFeed?,
        sectionName: String,
    ): List<CarouselViewItem> =
        if (mostReadFeed?.elements == null || mostReadFeed.elements.isEmpty()) {
            emptyList()
        } else {
            val list = ArrayList<CarouselViewItem>(mostReadFeed.elements.size)
            mostReadFeed.elements.forEach { mostReadElement ->
                val carouselViewItem = mostReadElement.toCarouselItem(sectionName)
                if (carouselViewItem != null) {
                    list.add(carouselViewItem)
                }
            }
            list
        }

    private fun MostReadElement.toCarouselItem(sectionName: String): CarouselViewItem? {
        val title: String = headlines?.basic ?: return null
        val contentURL =
            if ("washpost" == website &&
                canonicalURL != null &&
                !canonicalURL.containsCaseInsensitive(
                    "www.washingtonpost.com",
                )
            ) {
                wapoHomepage + if (canonicalURL.startsWith("/")) canonicalURL else ("/$canonicalURL")
            } else {
                wapoHomepage
            }
        val kicker = label?.transparency?.text
        val kickerStyle = labelDisplay?.basic?.style
        val storyType = label?.basic?.text
        val imageURL =
            if ((promoItems?.basic?.width ?: 0) > ((promoItems?.basic?.height ?: 0))) {
                promoItems?.basic?.url ?: promoItems?.basic?.addProps?.resizeURL
            } else {
                null
            }

        val byline =
            if (credits?.by != null) {
                val sb = StringBuilder()
                val size = credits.by.size
                if (size > 0) {
                    sb.append("By ")
                    credits.by.forEachIndexed { index, mostReadSubElement ->
                        sb.append(mostReadSubElement.name)
                        if (index == (size - 2)) {
                            sb.append(" and ")
                        } else if (index < (size - 2)) {
                            sb.append(", ")
                        }
                    }
                }
                sb.toString()
            } else {
                ""
            }

        return CarouselViewItem(
            hashCode(),
            contentURL,
            kicker,
            storyType,
            "",
            title,
            byline,
            imageURL,
            sectionName,
            true,
            null,
            null,
            null,
            null,
            style = kickerStyle?.let { getStyle(it) },
        )
    }

    private fun prefetchArticles(
        items: List<CarouselViewItem>,
        sectionName: String,
    ) {
        GlobalScope.launch {
            val updatedCarouselItems = mutableListOf<CarouselViewItem>()
            for (carouselViewItem in items) {
                val articleStatus =
                    FlagshipApplication.getInstance().repo.fetchArticleStatus(
                        Query(carouselViewItem.contentUrl),
                    )
                when (articleStatus) {
                    is Status.Cache ->
                        mapAndAddItem(
                            updatedCarouselItems,
                            articleStatus.data,
                            carouselViewItem,
                        )
                    is Status.Network ->
                        mapAndAddItem(
                            updatedCarouselItems,
                            articleStatus.data,
                            carouselViewItem,
                        )
                    is Status.Error -> { /*skip*/ }
                    is Status.Error415 -> { /*skip*/ }
                }
            }
            cache[sectionName] = updatedCarouselItems
        }
    }

    private fun mapAndAddItem(
        targetList: MutableList<CarouselViewItem>,
        article: Article2,
        carouselViewItem: CarouselViewItem,
    ) {
        val mappedItem =
            CarouselViewItem(
                carouselViewItem.id,
                carouselViewItem.contentUrl,
                carouselViewItem.kicker,
                carouselViewItem.storyType,
                "",
                carouselViewItem.title,
                carouselViewItem.byline,
                findProperImageUrl(article, carouselViewItem.imageUrl),
                carouselViewItem.sectionName,
                true,
                carouselViewItem.becauseYouRead,
                carouselViewItem.displayDate,
                carouselViewItem.trackingString,
                carouselViewItem.headlinePrefix,
                style = carouselViewItem.style,
            )
        targetList.add(mappedItem)
    }

    private fun findProperImageUrl(
        nativeContent: Article2,
        currentImageURL: String?,
    ): String? {
        if (currentImageURL == null) {
            if (!nativeContent.socialImage.isNullOrBlank()) return nativeContent.socialImage
            return nativeContent.items
                ?.filterIsInstance(Image::class.java)
                ?.firstOrNull()
                ?.takeIf {
                    it.imageWidth != null &&
                        it.imageHeight != null &&
                        it.imageWidth > it.imageHeight &&
                        !it.imageURL.isNullOrBlank()
                }?.imageURL
        } else {
            return currentImageURL
        }
    }
}
