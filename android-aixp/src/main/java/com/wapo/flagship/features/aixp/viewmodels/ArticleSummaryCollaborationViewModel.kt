/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.aixp.models.ArticleMeta
import com.wapo.flagship.features.aixp.models.ArticleRealtimeSummary
import com.wapo.flagship.features.aixp.models.Status
import com.wapo.flagship.features.aixp.models.ArticleSummary
import com.wapo.flagship.features.aixp.models.toSummary
import com.wapo.flagship.features.aixp.querypolicies.Query
import com.wapo.flagship.features.aixp.querypolicies.SummaryQueryPolicy
import com.wapo.flagship.features.aixp.repo.ArticleSummaryRepository
import com.wapo.flagship.features.aixp.states.ArticleSummaryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class ArticleSummaryCollaborationViewModel @Inject constructor(
    private val repository: ArticleSummaryRepository,
) : ViewModel() {

    private val _articleSummaryState: MediatorLiveData<ArticleSummaryState?> = MediatorLiveData()
    val articleSummaryState: LiveData<ArticleSummaryState?> = _articleSummaryState

    private val _forYouSummary: MutableLiveData<ArticleSummary?> = MutableLiveData()
    val forYouSummary: LiveData<ArticleSummary?> = _forYouSummary

    private var articleMeta: ArticleMeta? = null

    private val _realtimeFeedbackEnabled: MutableLiveData<Boolean> = MutableLiveData(true)
    private val realtimeFeedbackEnabled: LiveData<Boolean> = _realtimeFeedbackEnabled

    fun setArticleMeta(articleMeta: ArticleMeta?) {
        this.articleMeta = articleMeta
        resetArticleSummaryState()
    }

    fun setForYouSummary(summary: ArticleSummary?) {
        _forYouSummary.value = summary
    }

    fun enableFeedback(realtimeFeedbackEnabled: Boolean) {
        _realtimeFeedbackEnabled.value = realtimeFeedbackEnabled
    }

    fun startLoadingSummary() {
        val meta = articleMeta ?: return
        val url = articleMeta?.contentUrl ?: return
        _articleSummaryState.postValue(ArticleSummaryState.Loading)
        val policy = SummaryQueryPolicy<ArticleRealtimeSummary>(0, false)
        val articleSummary =
            repository.fetchData(meta, Query(url, policy), viewModelScope, Dispatchers.IO)
        _articleSummaryState.addSource(articleSummary) {
            when (it) {
                is Status.Network -> {
                    _articleSummaryState.postValue(ArticleSummaryState.Success(it.data.toSummary()))
                }
                is Status.Error -> {
                    _articleSummaryState.postValue(ArticleSummaryState.Failure(it.message))
                }
                else -> {}
            }
        }
    }

    private fun resetArticleSummaryState() {
        _articleSummaryState.value = null
    }

    /**
     * ForYouSummary is considered realtimeSummary so feedback is send to realtime/feedback endpoint
     */
    fun isRealtimeSummary(): Boolean {
        return articleSummaryState.value is ArticleSummaryState.Success || isForYouSummary()
    }

    fun isForYouSummary(): Boolean {
        return forYouSummary.value is ArticleSummary
    }

    fun shouldEnableFeedbackLink(): Boolean {
        return realtimeFeedbackEnabled.value == true && (isRealtimeSummary() || isForYouSummary())
    }

    private var arcId: String? = null

    fun getArticleId(): String? {
        return articleMeta?.contentId ?: this.arcId
    }

    fun setArticleId(articleId: String?) {
        this.arcId = articleId
    }

    fun getCurrentSummary(): ArticleSummary? {
        return forYouSummary.value
            ?: (articleSummaryState.value as? ArticleSummaryState.Success)?.run { this.summary }
    }

    override fun onCleared() {
        articleMeta = null
        super.onCleared()
    }

    companion object {
        const val MISCELLANY_SUFFIX_REALTIME = "auto"
    }
}