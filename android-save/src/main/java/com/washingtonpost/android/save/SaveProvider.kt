package com.washingtonpost.android.save

import android.content.Context
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader

interface SaveProvider {
    fun isLoggedInUser() : Boolean

    fun isLoggedInUserAndSubscriber() : Boolean

    fun getPreferencesRequestHeaders(isArchive: Boolean) : HashMap<String, String>

    fun getPreferenceBaseURL() : String

    fun getMetadataBaseUrl(): String

    fun getAnimatedImageLoader(): AnimatedImageLoader

    fun getSavedArticleManager(): SavedArticleManager

    fun updateArticlesIfNeeded(savedArticleList: List<ArticleAndMetadata>)

    fun getAppContext(): Context

    fun logException(throwable: Throwable)

    fun isConnected(): Boolean

    fun logPreferenceSyncException(t: Throwable)

    fun logMetadataSyncException(t: Throwable)

    fun logPreferenceErrorResponse(code: Int, errorResponse: String)

    fun logMetadataErrorResponse(code: Int, errorResponse: String)

    fun logExtras(message: String)

    fun openWebViewActivity(url: String, context: Context)

    fun openLoginActivity(context: Context)

    fun isNightModeOn(): Boolean

    fun openArticles(context: Context?, navigationBehavior: String, urls: Array<String>, url: String, sectionDisplayName: String)
}