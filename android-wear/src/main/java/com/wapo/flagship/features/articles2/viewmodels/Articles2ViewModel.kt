/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.states.ArticleContentState
import com.wapo.flagship.features.articles2.use_cases.Articles2UseCases
import com.wapo.flagship.features.section.models.ArticleMeta
import com.wapo.flagship.models.Status
import com.wapo.flagship.querypolicies.BypassCacheQueryPolicy
import com.wapo.flagship.querypolicies.LMTQueryPolicy
import com.wapo.flagship.querypolicies.Query
import com.wapo.flagship.utils.coroutines.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * View model for Articles2 feature.
 *
 */
@HiltViewModel
class Articles2ViewModel @Inject constructor(
    private val articles2UseCases: Articles2UseCases,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {
    private val _article2State = MutableStateFlow(
        Article2State(
            articleContentState = ArticleContentState.UiTimedOut
        )
    )
    val article2State = _article2State.asStateFlow()

    /**
     * A job that will run in the background to track time spent on individual article network request.
     */
    var uiTimeOutTimer: Job? = null

    private var getArticle2Job: Job? = null

    fun getArticle2(articleMeta: ArticleMeta) {
        uiTimeOutTimer?.cancel()
        getArticle2Job?.cancel()

        _article2State.update {
            it.copy(articleContentState = ArticleContentState.Loading)
        }
        val policy =
            if (articleMeta.bypassCache) BypassCacheQueryPolicy()
            else LMTQueryPolicy(articleMeta.lastModified)
        startUiTimeoutTimer()

        getArticle2Job = articles2UseCases
            .getArticles2(Query(articleMeta.contentUrl, policy))
            .onEach { article2Status ->
                when (article2Status) {
                    is Status.Network -> {
                        _article2State.update {
                            it.copy(
                                articleContentState = processData(
                                    article2Status.data,
                                    ArticleContentState.Source.NETWORK
                                )
                            )
                        }
                    }
                    is Status.Cache -> {
                        _article2State.update {
                            it.copy(
                                articleContentState = processData(
                                    article2Status.data,
                                    ArticleContentState.Source.CACHE
                                )
                            )
                        }
                    }
                    is Status.Error -> {
                        _article2State.update {
                            it.copy(articleContentState = ArticleContentState.Failure)
                        }
                    }
                    is Status.Error415 -> {
                        _article2State.update {
                            it.copy(
                                articleContentState = ArticleContentState.Unsupported(
                                    article415 = article2Status.article415
                                )
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startUiTimeoutTimer() {
        uiTimeOutTimer = viewModelScope.launch(dispatcherProvider.io) {
            // Wait for UI timeout before we can show the WebView article to the user.
            delay(WearAppContext.config().articleContentUpdateRulesConfig.timeout)

            if (_article2State.value.articleContentState is ArticleContentState.Loading) {
                _article2State.update {
                    it.copy(articleContentState = ArticleContentState.UiTimedOut)
                }
            }
        }
    }

    private fun processData(
        article: Article2,
        source: ArticleContentState.Source
    ): ArticleContentState {
        return ArticleContentState.Success(article, source = source)
    }

}