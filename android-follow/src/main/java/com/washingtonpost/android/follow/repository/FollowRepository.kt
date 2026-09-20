package com.washingtonpost.android.follow.repository

import androidx.annotation.MainThread
import androidx.lifecycle.switchMap
import androidx.paging.Config
import androidx.paging.toLiveData
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.follow.model.ArticleItem

class FollowRepository(private val followManager: FollowManager) {
    @MainThread
    fun getArticles(authorId: String, pageSize: Int): Listing<ArticleItem> {
        val sourceFactory = FollowDataSourceFactory(followManager, authorId, followManager.networkExecutor)

        val livePagedList = sourceFactory.toLiveData(
                config = Config(
                        pageSize = pageSize,
                        enablePlaceholders = false,
                        initialLoadSizeHint = pageSize),
                fetchExecutor = followManager.networkExecutor)

        val refreshState = sourceFactory.sourceLiveData.switchMap {
            it.initialLoad
        }
        return Listing(
                pagedList = livePagedList,
                networkState = sourceFactory.sourceLiveData.switchMap {
                    it.networkState
                },
                retry = {
                    sourceFactory.sourceLiveData.value?.retryAllFailed()
                },
                refresh = {
                    sourceFactory.sourceLiveData.value?.invalidate()
                },
                refreshState = refreshState,
                clearCoroutineJobs = {
                    sourceFactory.sourceLiveData.value?.clearCoroutineJobs()
                }
        )
    }
}