package com.wapo.flagship.features.articles.recirculation

import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles.models.ArticlesRecirculationArticleModelItem

class RecirculationHook {
    companion object {
        @JvmStatic
        fun prefetchItems(carouselCache: RecirculationStorage) {
            val mostReadSectionName =
                ArticlesRecirculationArticleModelItem.getSectionName(
                    ArticlesRecirculationArticleModelItem.Type.MOST_READ,
                )
            carouselCache
                .getCarouselItems(
                    mostReadSectionName,
                    ArticlesRecirculationArticleModelItem.Type.MOST_READ,
                ).subscribe(
                    {
                        Logger.d("RecirculationHook", "$mostReadSectionName prefetched")
                    },
                    {
                        Logger.d(
                            "RecirculationHook",
                            "error prefetching $mostReadSectionName",
                            it,
                        )
                    },
                )
        }
    }
}
