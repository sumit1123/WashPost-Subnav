package com.wapo.flagship.wapomain

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.domain.repository.RecommendationsRepo
import com.wapo.flagship.features.articles2.interfaces.ArticlesSaveRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val recommendationsRepo: RecommendationsRepo,
    private val articlesSaveRepo: ArticlesSaveRepo
): ViewModel() {

    private val _mainEvent: MutableSharedFlow<MainActivityEvent?> = MutableSharedFlow(replay = 0)
    val mainEvent: SharedFlow<MainActivityEvent?> = _mainEvent

    fun fetchRecommendationsData(
        surface: String,
        fetchFromCache: Boolean,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                recommendationsRepo.fetchData(
                    surface,
                    fetchFromCache
                )
            }
        }
    }

    fun getArticleUrlsById(
        widgetId: String?,
        bundle: Bundle?,
        widgetType: String?,
        articleContentUrl: String?,
        sectionDisplayName: String,
    ) {
        viewModelScope.launch {
            val result = widgetId?.let {
                articlesSaveRepo.getArticleUrlsById(widgetId)
            } ?: listOf()
            _mainEvent.emit(
                MainActivityEvent.OnGetArticles(
                    articlesUrls = result,
                    bundle = bundle,
                    widgetType = widgetType,
                    articleContentUrl = articleContentUrl,
                    sectionDisplayName = sectionDisplayName
                )
            )
        }
    }
}
