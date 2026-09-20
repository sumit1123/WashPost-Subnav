package com.washingtonpost.android.follow.viewmodel

import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.follow.repository.FollowRepository

const val FOLLOW_ID = "ArticleListViewModel.FOLLOW_ID"

class ArticleListViewModel @Keep constructor(val followManager: FollowManager,
                                             private val savedState: SavedStateHandle) : ViewModel() {

    private val repository = FollowRepository(followManager)

    private val repoResult = savedState.getLiveData<String>(FOLLOW_ID).map {
        repository.getArticles(it, FollowManager.PAGE_SIZE)
    }
    val articles = repoResult.switchMap { it.pagedList }
    val networkState = repoResult.switchMap { it.networkState }
    val refreshState = repoResult.switchMap { it.refreshState }

    @WorkerThread
    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    @WorkerThread
    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }

    @UiThread
    fun showArticles(followId: String?): Boolean {
        if (savedState.get<String?>(FOLLOW_ID) == followId) {
            return false
        }
        savedState.set(FOLLOW_ID, followId)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        repoResult.value?.clearCoroutineJobs?.invoke()
    }
}