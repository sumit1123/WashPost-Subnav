package com.wapo.flagship.data.repository

import com.wapo.flagship.data.model.RecommendationsRepoResult
import com.wapo.flagship.domain.repository.RecommendationsRepo
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import javax.inject.Inject

class RecommendationsRepoImpl @Inject constructor(
    private val forYouFeedRepository: ForYouFeedRepository
): RecommendationsRepo {

    override suspend fun fetchData(
        surface: String,
        fetchFromCache: Boolean,
        limit: Int?,
        currentUrl: String?,
        getMore: Boolean
    ): RecommendationsRepoResult {
        return if (fetchFromCache) {
            fetchDataFromCache(surface)
        } else {
            fetchDataFromRemote(surface, limit, currentUrl, getMore)
        }
    }

    private suspend fun fetchDataFromCache(surface: String): RecommendationsRepoResult {
        val recs = forYouFeedRepository.getCache(surface)
        return if (recs is APIResult.Success) {
            if (surface == ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL) {
                RecommendationsRepoResult.MapData(
                    item = recs.data?.recommendations?.first(),
                    getMore = false
                )
            } else {
                RecommendationsRepoResult.FeedData(
                    response = recs.data
                )
            }
        } else {
            RecommendationsRepoResult.Error("forYouFeedRepository not Success => $recs")
        }
    }

    private suspend fun fetchDataFromRemote(
        surface: String,
        limit: Int? = null,
        currentUrl: String? = null,
        getMore: Boolean = false
    ): RecommendationsRepoResult {
        val recs = if (getMore) {
            forYouFeedRepository.getMoreRecommendations(
                surface = surface,
                limit = limit,
                currentUrl = currentUrl
            )
        } else {
            forYouFeedRepository.getForYouRecommendations(
                skipReadingFromCache = surface == ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL,
                surface = surface,
                excludeList = getExcludeList(surface),
                limit = limit,
                currentUrl = currentUrl
            )
        }

        return if (recs is APIResult.Success) {
            if (surface == ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL) {
                RecommendationsRepoResult.MapData(
                    item = recs.data?.recommendations?.lastOrNull(),
                    getMore = getMore
                )
            } else {
                RecommendationsRepoResult.FeedData(
                    response = recs.data
                )
            }
        } else {
            RecommendationsRepoResult.Error("forYouFeedRepository not Success => $recs")
        }
    }

    private suspend fun getExcludeList(surface: String): List<String> {
        return if (surface == ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL) {
            val result = forYouFeedRepository.getCache(surface)
            if (result is APIResult.Success) {
                result.data?.recommendations?.mapNotNull { it.url } ?: emptyList()
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
