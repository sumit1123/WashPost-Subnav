package com.washingtonpost.foryou.viewmodel

import android.app.Activity
import com.wapo.android.commons.util.Logger
import androidx.annotation.WorkerThread
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.sections.SectionActivity
import com.washingtonpost.foryou.data.ForYouContentType
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.ui.ForYouUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val forYouFeedRepository: ForYouFeedRepository
): ViewModel() {

    private val _uiState = MutableStateFlow<ForYouUiState>(ForYouUiState.Loading)
    val uiState: StateFlow<ForYouUiState> = _uiState

    private val _loadMoreRecs = MutableStateFlow(false)
    var loadMoreRecs: StateFlow<Boolean> = _loadMoreRecs
    var canLoadMore = true
        private set

    private var loadMoreRequested = false
    private var currentRecs = mutableListOf<RecommendationsItem>()

    private val _isTabActive = MutableStateFlow(false)
    val isTabActive: StateFlow<Boolean> get() = _isTabActive

    val playerManager = mutableStateOf<PostTvPlayer2Manager?>(null)

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val forYouResult = forYouFeedRepository.getForYouRecommendations(
                    false,
                    surface = ForYouFeedRepositoryImpl.SURFACE_FEED,
                    contentType = listOf(ForYouContentType.ARTICLE.type, ForYouContentType.VIDEO.type)
                )
                updateState(forYouResult)
                when (forYouResult) {
                    is APIResult.Success -> {
                        (forYouResult.data?.recommendations as? MutableList<RecommendationsItem>)?.let {
                            currentRecs = it
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    suspend fun getMoreRecommendations(
        contentType: List<String>? = null
    ){
        _loadMoreRecs.value = false
        if (loadMoreRequested || !canLoadMore) {
            // already processing
            return
        }
        loadMoreRequested = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val moreRecommendations = forYouFeedRepository.getMoreRecommendations(
                    contentType = contentType
                )
                when (moreRecommendations) {
                    is APIResult.Success -> {
                        // checking size since getMoreRecommendations() will always return a new list even when we hit max limit
                        val isResultNew = currentRecs.size != moreRecommendations.data?.recommendations?.size
                        _loadMoreRecs.value = isResultNew
                        (moreRecommendations.data?.recommendations as? MutableList<RecommendationsItem>)?.let {
                            currentRecs = it
                        }
                        withContext(Dispatchers.Main) {
                            updateState(moreRecommendations)
                            loadMoreRequested = false
                        }
                    }
                    else -> {}
                }

                withContext(Dispatchers.Main) {
                    canLoadMore = currentRecs.size < forYouFeedRepository.maxLimit()
                    updateState(moreRecommendations)
                    loadMoreRequested = false
                }
            }
        }
    }

    fun attachPlayer(context: Activity, sectionDisplayName: String) {
        playerManager.value = PostTvPlayer2Coordinator.getOrCreatePlayer(sectionDisplayName, context)
        playerManager.value?.apply {
            mute()
            val containerResId = context.findActivityOfType<SectionActivity>()?.player2ContainerResId
            if (containerResId != null) updatePlayerContainerView(containerResId)
            hideController()
            setRespectAudioFocus(false)
        }
    }

    fun releasePlayer() {
        playerManager.value?.releasePlayer()
        playerManager.value = null
    }

    @WorkerThread
    fun refresh() {
        canLoadMore = true
        _uiState.value = ForYouUiState.SwipeToRefresh
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val results = forYouFeedRepository.refresh()
                updateState(results)
            }
        }
    }

    private fun updateState(forYouResult: APIResult<ForYouResponse>) {
        when (forYouResult) {
            is APIResult.Success -> {
                _uiState.value = forYouResult.data?.recommendations?.let {
                    ForYouUiState.Feed(forYouResult.data.requestId, forYouResult.data.recipeId, forYouResult.data.testId, it)
                } ?: run {
                    Logger.e(TAG, "Failed to parse response: $forYouResult")
                    ForYouUiState.Error
                }
            }
            is APIResult.Failure -> _uiState.value = ForYouUiState.Error
            is APIResult.NetworkError -> _uiState.value = ForYouUiState.Error
        }
    }

    fun setIsTabActive(isActive: Boolean) {
        _isTabActive.value = isActive
    }

    companion object {
        private val TAG = ForYouViewModel::class.java.simpleName
    }
}