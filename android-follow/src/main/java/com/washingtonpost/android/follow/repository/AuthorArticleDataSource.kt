package com.washingtonpost.android.follow.repository

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.follow.model.ArticleItem
import com.washingtonpost.android.volley.VolleyError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class AuthorArticleDataSource(
        private val followManager: FollowManager,
        private val authorId: String,
        private val retryExecutor: Executor) : PageKeyedDataSource<Int, ArticleItem>() {

    private val completableJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + completableJob)
    private var retry: (() -> Any)? = null
    val networkState = MutableLiveData<NetworkState>()
    val initialLoad = MutableLiveData<NetworkState>()

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, ArticleItem>) {
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, ArticleItem>) {
        coroutineScope.launch {
            try {
                networkState.postValue(NetworkState.LOADING)
                val author = followManager.fetchAuthor(authorId, params.requestedLoadSize,
                        params.key * params.requestedLoadSize)
                val items = author.items ?: emptyList()
                retry = null
                callback.onResult(items, params.key + 1)
                networkState.postValue(NetworkState.LOADED)
            } catch (volleyError: VolleyError) {
                networkState.postValue(NetworkState.NOT_FOUND)
            }
        }
    }

    override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Int, ArticleItem>) {
        coroutineScope.launch {
            try {
                networkState.postValue(NetworkState.LOADED)
                initialLoad.postValue(NetworkState.LOADING)
                val author = followManager.fetchAuthor(authorId,params.requestedLoadSize)
                val items = author.items ?: emptyList()
                retry = null
                initialLoad.postValue(NetworkState.LOADED)
                callback.onResult(items, 1, 2)
            } catch (volleyError: VolleyError) {
                retry = {
                    loadInitial(params, callback)
                }
                initialLoad.postValue(NetworkState.ERROR)
            }
        }
    }

    fun clearCoroutineJobs() {
        completableJob.cancel()
    }

    companion object {
        val TAG: String = AuthorArticleDataSource::class.java.simpleName
    }
}