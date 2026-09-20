package com.wapo.flagship.features.articles2.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.services.Articles2Service
import com.wapo.flagship.features.articles2.typeconverters.MoshiAdapters
import com.wapo.flagship.features.articles2.typeconverters.parseArticle
import com.wapo.flagship.model.Status
import com.wapo.flagship.querypolicies.Query
import com.wapo.flagship.roomdb.AppDatabase
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Note: [com.wapo.flagship.features.deeplinks.DeepLinksProcessor] requires deep links to be a valid urls.
 * So added a url prefix to make the resource file a valid url.
 */
const val DEBUG_ARTICLES_PREFIX = "https://www.washingtonpost.com/debug"
val DEBUG_ARTICLES_RESOURCES =
    arrayOf(
        "$DEBUG_ARTICLES_PREFIX/autoplay_video_article",
        "$DEBUG_ARTICLES_PREFIX/human_read_author_narrated_article",
        "$DEBUG_ARTICLES_PREFIX/ad_targeting_api_article",
        "$DEBUG_ARTICLES_PREFIX/human_read_article",
        "$DEBUG_ARTICLES_PREFIX/article_the_7",
        "$DEBUG_ARTICLES_PREFIX/article_inline_ads",
        "$DEBUG_ARTICLES_PREFIX/article_style_font",
        "$DEBUG_ARTICLES_PREFIX/article_inline_gallery",
        "$DEBUG_ARTICLES_PREFIX/article_live_image",
        "$DEBUG_ARTICLES_PREFIX/reporter_insight_luf",
        "$DEBUG_ARTICLES_PREFIX/inline_pod_article",
        "$DEBUG_ARTICLES_PREFIX/seven_briefs_exclusive",
        "$DEBUG_ARTICLES_PREFIX/kitchen_sink",
        "$DEBUG_ARTICLES_PREFIX/article_list",
        "$DEBUG_ARTICLES_PREFIX/article_faq",
        "$DEBUG_ARTICLES_PREFIX/article_cardified",
        "$DEBUG_ARTICLES_PREFIX/article_cardified_dev",
        "$DEBUG_ARTICLES_PREFIX/article_cardified_luf_1",
        "$DEBUG_ARTICLES_PREFIX/article_cardified_luf_2",
        "$DEBUG_ARTICLES_PREFIX/article_cardified_luf_3",
        "$DEBUG_ARTICLES_PREFIX/article_audio_new_voices",
        "$DEBUG_ARTICLES_PREFIX/article_context_box",
        "$DEBUG_ARTICLES_PREFIX/article_live_image",
        "$DEBUG_ARTICLES_PREFIX/in_story_carousel",
        "$DEBUG_ARTICLES_PREFIX/in_story_autorecirc_carousel",
        "$DEBUG_ARTICLES_PREFIX/article_tall_image",
        "$DEBUG_ARTICLES_PREFIX/article_block_ads",
        "$DEBUG_ARTICLES_PREFIX/article_summaries",
        "$DEBUG_ARTICLES_PREFIX/static_test_articlepage",
        "$DEBUG_ARTICLES_PREFIX/atp_article",
        "$DEBUG_ARTICLES_PREFIX/fts_article",
        "$DEBUG_ARTICLES_PREFIX/oembed_testing",
        "$DEBUG_ARTICLES_PREFIX/oembed_testing2",
        "$DEBUG_ARTICLES_PREFIX/oembed_testing3",
        "$DEBUG_ARTICLES_PREFIX/vast_audio_article",
        "$DEBUG_ARTICLES_PREFIX/vast_audio_article_od_spy",
        "$DEBUG_ARTICLES_PREFIX/vast_article_with_podcast",
        "$DEBUG_ARTICLES_PREFIX/seven_live_article",
        "$DEBUG_ARTICLES_PREFIX/ripple_article",
        "$DEBUG_ARTICLES_PREFIX/block_quote_test",
        "$DEBUG_ARTICLES_PREFIX/partial_feeds",
        "$DEBUG_ARTICLES_PREFIX/track_webview_event_feeds",
    )

/**
 * A utility repository that can load and parse article data directly from a resource file.
 * Example of usage: adb shell am start -n com.washingtonpost.android/com.wapo.flagship.wapomain.MainActivity -d "https://www.washingtonpost.com/debug/kitchen_sink"
 */
class DebugArticleRepository(
    articles2Service: Articles2Service,
    appDatabase: AppDatabase,
    coroutineScopeProvider: CoroutineScopeProvider,
) : Articles2Repository(articles2Service, appDatabase, coroutineScopeProvider) {
    override fun fetchData(
        query: Query<Article2>,
        viewModelScope: CoroutineScope?,
        coroutineContext: CoroutineContext?,
    ): LiveData<Status<out Article2>> {
        val url = query.url
        if (DEBUG_ARTICLES_RESOURCES.contains(url)) {
            val resourceName = URLParser(url).lastPathSegment()
            val article = parseArticleFromResources(resourceName)
            if (article != null) {
                val liveData = MutableLiveData<Status<out Article2>>()
                liveData.postValue(Status.Cache(article))
                return liveData
            }
        }
        return super.fetchData(query, viewModelScope, coroutineContext)
    }

    private fun parseArticleFromResources(resourceName: String?): Article2? =
        try {
            val context = FlagshipApplication.getInstance()
            val json =
                context.resources
                    .openRawResource(
                        context.resources.getIdentifier(
                            resourceName,
                            "raw",
                            context.packageName,
                        ),
                    ).reader()
                    .readText()
            parseArticle(json, MoshiAdapters.INSTANCE.moshi)
        } catch (t: Throwable) {
            null
        }
}
