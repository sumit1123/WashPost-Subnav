package com.wapo.flagship.features.signin

import android.app.TaskStackBuilder
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.activities.Articles2Activity
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.model.ArticleMeta
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import java.lang.reflect.Type

object LoginUtil {
    /**
     * If there's a value saved for [PaywallPrefHelper.PREF_ARTICLES_META_LIST]:
     * - Convert it back to an [ArrayList] of [ArticleMeta]'s
     * - Create a new [Articles2Activity] intent
     * - Add the [PaywallPrefHelper.PREF_ARTICLES_META_LIST] to the intent's extras
     * - Add the [PaywallPrefHelper.PREF_CURRENT_ARTICLE_URL]'s index in the list to the intent's extras
     * - Start a new activity with the intent
     * If [PaywallPrefHelper.PREF_ARTICLES_META_LIST] is null:
     * - Do nothing, as there's nothing to restore
     */
    private fun restoreArticleActivity(onComplete: (Intent) -> Unit) {
        val paywallPrefHelper = PaywallService.getPaywallPrefHelper()
        val articlesMetaListString = paywallPrefHelper.articlesMetaList
        if (articlesMetaListString != null) {
            val listType: Type = object : TypeToken<ArrayList<ArticleMeta?>?>() {}.type
            val articlesMetaList: ArrayList<ArticleMeta> =
                Gson().fromJson(articlesMetaListString, listType)
            val currentArticle =
                articlesMetaList.firstOrNull { it.id == paywallPrefHelper.currentArticleUrl }
            val currentArticleIndex = currentArticle?.let { articlesMetaList.indexOf(it) } ?: 0
            val context = FlagshipApplication.getInstance().applicationContext
            val intent =
                ArticlesParcel
                    .builder()
                    .setArticleMetas(articlesMetaList, currentArticleIndex)
                    .buildIntent(context)
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP)
            onComplete(intent)
            paywallPrefHelper.articlesMetaList = null
            paywallPrefHelper.currentArticleUrl = null
        }
    }

    fun restorePreviousActivity(onComplete: (Intent) -> Unit) {
        val paywallPrefHelper = PaywallService.getPaywallPrefHelper()
        val context = FlagshipApplication.getInstance().applicationContext
        val previousScreen = paywallPrefHelper.previousScreen

        // restore previous activity (and its parent stack if defined)
        if (!previousScreen.isNullOrEmpty() && previousScreen != Articles2Activity::class.java.name) {
            val prevClass = Class.forName(previousScreen)
            val intent = Intent(context, prevClass)

            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addNextIntentWithParentStack(intent)
            stackBuilder.startActivities()

            paywallPrefHelper.previousScreen = null
        } else if (previousScreen == Articles2Activity::class.java.name && paywallPrefHelper.articlesMetaList != null) {
            restoreArticleActivity(onComplete)
            paywallPrefHelper.previousScreen = null
        }
    }
}
