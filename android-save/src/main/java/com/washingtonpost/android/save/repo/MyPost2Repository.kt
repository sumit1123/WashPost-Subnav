package com.washingtonpost.android.save.repo

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class MyPost2Repository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val savedArticleManager: SavedArticleManager,
    private val coroutineScope: CoroutineScope
) {

    //TODO redo with dagger
    private val followManager = FollowManager.getInstance(applicationContext as Application)

    fun getPagedArticles(): LiveData<PagedList<ArticleAndMetadata>> {
        return savedArticleManager.getPagedArticles()
    }

    fun getFollowedAuthorsLiveData(): LiveData<List<AuthorEntity>?> {
        return followManager.followDb.followDao().followingAllByLmtLiveData()
    }

    suspend fun getArticlesByFollow(id: String): AuthorItem? {
        return try {
            followManager.fetchAuthor(id, 10, 0)
        } catch (t: Throwable) {
            return null
        }
    }

    fun getAuthorImageUrl(authorImageStub: String?) : String? {
        return followManager.followProvider.getAuthorImageRequestUrl(authorImageStub)
    }

    fun getLiveArticleByUrl(
        url: String,
    ): LiveData<ArticleAndMetadata?> {
            return savedArticleManager.getLiveArticleByUrl(
                replaceHttp(url)
            )
    }

    fun removeArticleFromList(savedArticleMeta: ArticleAndMetadata) {
        savedArticleManager.removeArticles(listOf(savedArticleMeta))
    }

    fun saveArticle(articleModel: SavedArticleModel, metadataModel: MetadataModel) {
        articleModel.contentURL = replaceHttp(articleModel.contentURL)
        metadataModel.contentURL = replaceHttp(metadataModel.contentURL)
        savedArticleManager.addArticle(articleModel, metadataModel)
    }

    fun synchronize() {
        savedArticleManager.synchronize()
        followManager.syncAuthorsFromRemote()
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