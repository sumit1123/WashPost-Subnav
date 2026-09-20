package com.wapo.flagship.features.articles2.adinjector

import android.util.SparseArray
import com.wapo.flagship.features.articles.AdViewInfo
import com.wapo.flagship.features.articles2.models.Article2

interface AdInjector2 {
    fun getAdPositions(articleModel: Article2?): SparseArray<AdViewInfo>

    fun getAdPositions(
        articleModel: Article2?,
        articleItems: List<Any?>?,
    ): SparseArray<AdViewInfo>

    fun shouldSuppressAds(): Boolean
}
