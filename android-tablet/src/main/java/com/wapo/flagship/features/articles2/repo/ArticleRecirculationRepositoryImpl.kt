package com.wapo.flagship.features.articles2.repo

import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.features.articles2.interfaces.ArticleRecirculationRepository
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.AutoRecircCarousel
import com.wapo.flagship.features.articles2.models.recirculation.ArticleCollectionsRequestBody
import com.wapo.flagship.features.articles2.models.recirculation.AutoRecircResponse
import com.wapo.flagship.features.articles2.models.recirculation.ArticleRecircMeta
import com.wapo.flagship.features.articles2.models.recirculation.ConsumedArticles
import com.wapo.flagship.features.articles2.services.RecirculationService
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.JUcidTracker
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.save.SavedArticleManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRecirculationRepositoryImpl @Inject constructor(
    private val recirculationService: RecirculationService,
    private val savedArticleManager: SavedArticleManager,
    @CoroutineScopeCommonsModule.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ArticleRecirculationRepository {
    override suspend fun getAutoRecirculation(
        article: Article2,
    ): APIResult<AutoRecircResponse> = withContext(ioDispatcher) {
        val collectionLimit = article.items?.count { it is AutoRecircCarousel } ?: 0
        if (collectionLimit == 0) {
            return@withContext APIResult.Failure(-1, "collection_limit is 0")
        }
        val recircMeta = getRecircMeta()
        val tags = article.omniture?.trackingTags?.split(DELIMITER).orEmpty()
        val request = ArticleCollectionsRequestBody(
            currentUrl = getPath(article.contenturl),
            collectionLimit = collectionLimit,
            articleLimit = MIN_ARTICLE_COUNT,
            articleId = article.arcId.orEmpty(),
            section = article.commercialnode.orEmpty(),
            authors = article.omniture?.authorId?.let {
                when {
                    it.isNotEmpty() && it != NOT_SET_VALUE -> it.split(DELIMITER)
                    else -> null
                }
            },
            collections = tags.filter { it.startsWith("pinned-collections-") },
            excludeCategory = when {
                tags.contains("exclude-autocollection") -> listOf("auto_collect")
                else -> null
            },
            `interface` = INTERFACE,
            surface = SURFACE,
            wapoLoginId = recircMeta.wapoLoginId,
            jUcid = recircMeta.jUcid.orEmpty(),
            readlist = getReadList(recircMeta),
        )
        when (val response = recirculationService.getAutoRecirculationArticles(request)) {
            is APIResult.Success -> {
                val responseWithCollectionIds = response.data?.copy(
                    collections = response.data.collections?.mapIndexed { index, collection ->
                        if (collection.collectionId == null) {
                            collection.copy(collectionId = index)
                        } else {
                            collection
                        }
                    }
                )
                APIResult.Success(responseWithCollectionIds, response.headers)
            }

            else -> response
        }
    }

    private fun getRecircMeta(): ArticleRecircMeta {
        return ArticleRecircMeta(
            wapoLoginId = PaywallService.getInstance().loginId,
            jUcid = JUcidTracker.jUcid,
            privacyConsentGiven = OneTrustHelper.isFunctionalityEnabled(),
        )
    }

    private fun getReadList(recircMeta: ArticleRecircMeta): List<ConsumedArticles> {
        return if (recircMeta.privacyConsentGiven) {
            savedArticleManager.getArticles(limit = 100)
                .map {
                    ConsumedArticles(
                        articleUrl = getPath(it.contentURL),
                        timestamp = it.lmt ?: 0
                    )
                }
        } else {
            emptyList()
        }
    }

    private fun getPath(url: String): String {
        val urlParser = URLParser(url)
        return if (urlParser.getDomain().isEmpty()) url else urlParser.getPath()
    }

    companion object {
        private const val MIN_ARTICLE_COUNT = 5
        private const val DELIMITER = ";"
        private const val NOT_SET_VALUE = "(not set)"
        private const val INTERFACE = "android"
        private const val SURFACE = "article-recirc"
    }
}