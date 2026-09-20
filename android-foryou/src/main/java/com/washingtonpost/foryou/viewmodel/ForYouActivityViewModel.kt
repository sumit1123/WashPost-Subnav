package com.washingtonpost.foryou.viewmodel

import androidx.lifecycle.LiveData
import com.wapo.android.commons.util.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "ForYouActivityViewModel"

/**
 * ViewModel to be used to get data from MyPost->ForYou and Article's recirc view.
 * TODO: It is scoped to host activity's life cycle. Probably, should be scoped globally to reduce re-init count.
 */
@HiltViewModel
class ForYouActivityViewModel @Inject constructor(
    private val forYouFeedRepository: ForYouFeedRepository
) : ViewModel() {

    private val _feedData = MutableStateFlow<ForYouResponse?>(null)
    val feedData: StateFlow<ForYouResponse?> = _feedData

    private val _feedDataReloaded: LiveEvent<Unit> = LiveEvent()
    val feedDataReloaded: LiveData<Unit> = _feedDataReloaded

    private val _mapData = MutableStateFlow<Pair<RecommendationsItem?, Boolean>>(Pair(null, false))
    val mapData: StateFlow<Pair<RecommendationsItem?, Boolean>> = _mapData

    init {
        Logger.d(TAG, "getForYouFeed: View model initialized")
    }

    /**
     * @param surface the FY Flex surface to send in the request
     * @param fetchFromCache whether or not to fetch the recommendation(s) from cache.
     *        IMPORTANT: Set to true when calling from a push originated scenario.
     * @param limit the maximum number of recommendations to fetch
     * @param currentUrl the current article's url
     */
    fun fetchData(
        surface: String,
        fetchFromCache: Boolean,
        limit: Int? = null,
        currentUrl: String? = null,
        getMore: Boolean = false
    ) {
        if (fetchFromCache) {
            fetchDataFromCache(surface)
        } else {
            fetchDataFromRemote(surface, limit, currentUrl, getMore)
        }
    }

    private fun fetchDataFromRemote(
        surface: String,
        limit: Int? = null,
        currentUrl: String? = null,
        getMore: Boolean = false
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
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

                if (recs is APIResult.Success) {
                    if (surface == ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL) {
                        _mapData.value = Pair(recs.data?.recommendations?.lastOrNull(), getMore)
                    } else {
                        _feedData.value = recs.data
                    }
                    _feedDataReloaded.postValue(Unit)
                }
            }
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

    private fun fetchDataFromCache(surface: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val recs = forYouFeedRepository.getCache(surface)
                if (recs is APIResult.Success) {
                    if (surface == ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL) {
                        _mapData.value = Pair(recs.data?.recommendations?.first(), false)
                    } else {
                        _feedData.value = recs.data
                    }
                    _feedDataReloaded.postValue(Unit)
                }
            }
        }
    }
}