package com.wapo.flagship.features.articles2.viewmodels.recirculation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.articles2.interfaces.ArticleRecirculationRepository
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.recirculation.AutoRecirculationMapper.toCarouselViewItem
import com.wapo.flagship.network.retrofit.network.APIResult
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleRecirculationViewModel @Inject constructor(
    private val recirculationRepository: ArticleRecirculationRepository,
    private val remoteLogRepo: RemoteLogRepo,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArticleRecirculationUiState())
    val uiState: StateFlow<ArticleRecirculationUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ArticleRecirculationEvent?>(replay = 0)
    val event: SharedFlow<ArticleRecirculationEvent?> = _event

    fun fetchAutoRecirculation(article: Article2) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = recirculationRepository.getAutoRecirculation(article)) {
                is APIResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        collections = buildMap {
                            val response = result.data ?: return@buildMap
                            val collections = response.collections.orEmpty()
                            collections.forEach { collection ->
                                put(
                                    collection.collectionId.toString(),
                                    CarouselViewGroup(
                                        id = collection.collectionId.toString(),
                                        requestId = response.requestId,
                                        category = collection.category,
                                        currentUrl = article.contenturl,
                                        items = collection.articles?.map { it.toCarouselViewItem(collection) }.orEmpty()
                                    )
                                )
                            }
                        },
                        error = null,
                    )
                }

                is APIResult.NetworkError -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.error.message ?: "Network error"
                    )
                }

                is APIResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.rawResponse ?: "Unknown error"
                        )
                    }
                    if (AppContextUtils.isConnectingOrConnected()) {
                        val eventLog = EventLog.Builder()
                            .setMessage("Get Article Auto Recirc failed")
                            .setModule(LogModules.ARTICLES)
                            .setErrorCode(result.statusCode)
                            .setErrorMessage(result.rawResponse)
                            .build()

                        remoteLogRepo.e(eventLog)
                    }
                }
            }
        }
    }

    fun onItemClicked(
        collectionId: String,
        positionInCarousel: Int,
    ) {
        val collection = _uiState.value.collections[collectionId] ?: return
        val collectionArticles = collection.items
        val article = collectionArticles.getOrNull(positionInCarousel) ?: return
        val urlsArray = collectionArticles.map { it.contentUrl }
        if (urlsArray.isEmpty() || positionInCarousel !in urlsArray.indices) return
        val articlesParcel = ArticlesParcel
            .builder()
            .setArticleUrls(urlsArray, positionInCarousel)
            .setCarouselCategoryId(collection.category)
            .setCategoryName(collection.category.orEmpty())
            .setSectionDisplayName(article.sectionName)
            .setCarouselTitle(article.sectionName)
            .setCarouselOriginated(true)
            .setRecircModuleOriginated(true)
        viewModelScope.launch {
            _event.emit(ArticleRecirculationEvent.OpenArticle(articlesParcel))
        }
    }
}
