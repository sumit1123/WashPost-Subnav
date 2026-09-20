package com.wapo.flagship.features.search2.navigation

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.wapo.android.commons.util.URLParser
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.search2.ui.Search2Activity

object SearchActivityNavigation {
    fun openSection(
        id: String,
        searchQuery: String,
        context: Context,
    ) {
        DeepLinksProcessor.processAsync(
            "washpost:///section?url=https://www.washingtonpost.com$id&search_query=$searchQuery",
            context,
            scope = context.findComponentActivity()?.lifecycleScope,
        )
    }

    /***
     * Opening a section from search results will close SearchActivity and open MainActivity.
     * Since MainActivity is always the root activity, we can not return to search results when clicking "BACK".
     * To imitate such behavior, we open SearchActivity with pre-filled section query when hitting "BACK" for the first time
     */
    fun imitateBackStack(
        intent: Intent?,
        activity: Activity,
    ): Boolean {
        intent ?: return false
        val backHandled = intent.hasExtra(EXTRA_SEARCH_BACK_HANDLED)
        if (backHandled) return false
        val urlParser = intent.getParcelableExtra<URLParser>(DeepLinksProcessor.ARG_URL_PARSER)
        val searchQuery = urlParser?.getQueryParameter("search_query")
        if (searchQuery != null) {
            intent.putExtra(EXTRA_SEARCH_BACK_HANDLED, true)
            val searchIntent = Intent(activity, Search2Activity::class.java)
            searchIntent.action = Intent.ACTION_SEARCH
            searchIntent.putExtra(SearchManager.QUERY, searchQuery)
            activity.startActivity(searchIntent)
            return true
        }
        return false
    }

    fun openArticle(
        url: String,
        context: Context,
        navBehavior: String?,
    ) {
        val intent =
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(url)
                .setNavigationBehavior(navBehavior)
                .setArticleOpenedFromSearch(true)
                .setItId(navBehavior)
                .buildIntent(context)
        context.startActivity(intent)
    }

    fun openRecipeArticle(
        url: String,
        context: Context,
        navBehavior: String?,
        position: Int,
    ) {
        val intent =
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(url)
                .setNavigationBehavior(navBehavior)
                .setArticleOpenedFromSearch(true)
                .setItId("sr_recipe-finder_$position")
                .buildIntent(context)
        context.startActivity(intent)
    }

    fun openElectionArticle(
        url: String,
        context: Context,
        navBehavior: String?,
        position: Int,
    ) {
        val intent =
            ArticlesParcel
                .builder()
                .setArticleSingleUrl(url)
                .setNavigationBehavior("sr_elections__$position")
                .setArticleOpenedFromSearch(true)
                .setItId("search_elections_menu")
                .buildIntent(context)
        context.startActivity(intent)
    }

    fun searchQuestion(
        id: String?,
        question: String?,
        activity: Activity,
    ) {
        question ?: return
        Intent(activity, Search2Activity::class.java).apply {
            action = Intent.ACTION_SEARCH
            putExtra(QUESTION_ID, id)
            putExtra(SearchManager.QUERY, question)
            activity.startActivity(this)
        }
    }

    private const val EXTRA_SEARCH_BACK_HANDLED = "EXTRA_SEARCH_BACK_HANDLED"
    const val QUESTION_ID = "QUESTION_ID"
}
