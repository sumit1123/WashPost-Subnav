/* Copyright (c) 2019 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.views

import androidx.annotation.NonNull
import androidx.lifecycle.*
import androidx.paging.PagedList
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import javax.inject.Inject

class ArticleListViewModel
@Inject
constructor
    (
    private val savedArticleManager: SavedArticleManager,
    ) : ViewModel() {

    private val currentPageUrl = MutableLiveData<String>()

    val liveArticleByUrl = currentPageUrl.switchMap { url ->
        getLiveArticleByUrl(url)
    }

    fun setCurrentPageUrl(@NonNull url: String) {
        currentPageUrl.value = url
    }

    fun getCurrentPageUrl(): String? {
        return currentPageUrl.value
    }

    fun getPagedArticles(): LiveData<PagedList<ArticleAndMetadata>> {
        return savedArticleManager.getPagedArticles()
    }

    fun getLiveArticleByUrl(url: String): LiveData<ArticleAndMetadata?> {
        return savedArticleManager.getLiveArticleByUrl(replaceHttp(url))
    }

    fun saveArticle(articleModel: SavedArticleModel, metadataModel: MetadataModel) {
        articleModel.contentURL = replaceHttp(articleModel.contentURL)
        metadataModel.contentURL = replaceHttp(metadataModel.contentURL)
        savedArticleManager.addArticle(articleModel, metadataModel)
    }

    fun removeArticles(articles: List<ArticleAndMetadata>) {
        articles.map { it.contentURL = replaceHttp(it.contentURL) }
        savedArticleManager.removeArticles(articles)
    }

    class Factory(
        private val articleManager: SavedArticleManager,
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ArticleListViewModel(articleManager) as T
        }
    }

    /**
     * This supporting method was added because at this time, print edition URLs are not https.
     * The collections API automatically converts URLs from http to https. This change prevents
     * UI bugs such as the save icon flashing or not staying highlighted.
     */
    private fun replaceHttp(url: String): String {
        return url.replace("http://", "https://")
    }
}