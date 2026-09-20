package com.washingtonpost.android.follow.repository

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.follow.model.ArticleItem
import java.util.concurrent.Executor

class FollowDataSourceFactory(
        private val followManager: FollowManager,
        private val followId: String,
        private val retryExecutor: Executor) : DataSource.Factory<Int, ArticleItem>() {
    val sourceLiveData = MutableLiveData<AuthorArticleDataSource>()
    override fun create(): DataSource<Int, ArticleItem> {
        val source = AuthorArticleDataSource(followManager, followId, retryExecutor)
        sourceLiveData.postValue(source)
        return source
    }
}