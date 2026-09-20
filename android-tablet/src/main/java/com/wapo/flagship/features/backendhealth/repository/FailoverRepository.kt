package com.wapo.flagship.features.backendhealth.repository

import com.squareup.moshi.Moshi
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.features.backendhealth.models.FailoverPageResponse
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.config.domain.manager.ConfigManager
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.text.ifEmpty
import kotlin.text.isEmpty

class FailoverRepository @Inject constructor(
    val dispatcherProvider: DispatcherProvider,
) {
    private val failoverUrl get() = ConfigManager.getInstance().config.backendHealthConfig.fallbackStaticURL
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(FailoverPageResponse::class.java)
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(DefaultHeadersInterceptor())
            .callTimeout(15, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchArticles(url: String): APIResult<FailoverPageResponse> {
        if (BuildConfig.DEBUG && ENABLE_TEST_MODE) {
            return try {
                APIResult.Success(adapter.fromJson(articlesMockResponse), Headers.headersOf())
            } catch (e: Exception) {
                APIResult.Failure(0, "Error fetching failover articles")
            }
        }

        return withContext(dispatcherProvider.io) {
            val url = url.ifEmpty { failoverUrl }
            if (url.isEmpty()) return@withContext APIResult.Failure(0, "Feature not enabled")
            try {
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val json = response.use {
                    if (!it.isSuccessful) throw okio.IOException("HTTP ${it.code}")
                    it.body?.string()
                }
                val data = json?.let { adapter.fromJson(json) }
                if (data != null) {
                    APIResult.Success(data, Headers.Companion.headersOf())
                } else {
                    APIResult.Failure(0, "Failed to parse response")
                }
            } catch (e: Exception) {
                APIResult.Failure(0, e.message)
            }
        }
    }

    companion object {
        private const val ENABLE_TEST_MODE = false
        private val articlesMockResponse = """
            {
                "articles": [
                    {
                        "blurb": "",
                        "byline": "",
                        "contenttype": "",
                        "contenturl": "https://www.washingtonpost.com/donald-trump/",
                        "headline": "Donald Trump - The Washington Post",
                        "smallthumburl": "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT0M3kIjTa1I3-GZa3jRLwhy3ZZVUaPN2QYh_LuWqPKR5rr4rF8ZL8XzZ3F&s",
                        "systemid": "https://www.washingtonpost.com/donald-trump/"
                    },
                    {
                        "blurb": "The North Korean leader has grown closer to Moscow since he and Trump met in 2019 and did not respond to the U.S. president’s public push for a meeting.",
                        "byline": "",
                        "contenttype": "Article",
                        "contenturl": "https://www.washingtonpost.com/politics/2025/10/30/trump-kim-north-korea-gifts/",
                        "displaydatetime": 1761838097000,
                        "headline": "Analysis | Trump wanted one gift in Asia he didn’t get: A visit with Kim Jong Un",
                        "smallthumburl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/ZXB3TB2KQJMLMRAAG4C2SOSBSU_size-normalized.jpg",
                        "systemid": "https://www.washingtonpost.com/politics/2025/10/30/trump-kim-north-korea-gifts/"
                    },
                    {
                        "blurb": "The move comes as President Donald Trump pursues efforts to build a White House ballroom and a triumphal arch in Washington.",
                        "byline": "",
                        "contenttype": "Article",
                        "contenturl": "https://www.washingtonpost.com/politics/2025/10/28/trump-arts-commission-firings-ballroom-arch/",
                        "displaydatetime": 1761689406000,
                        "headline": "White House fires arts commission expected to review Trump construction projects",
                        "smallthumburl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/ICQ65D536QYTTBIIUCNKL6QKUE.JPG",
                        "systemid": "https://www.washingtonpost.com/politics/2025/10/28/trump-arts-commission-firings-ballroom-arch/"
                    },
                    {
                        "blurb": "The president said he wanted testing to occur “on an equal basis” with Russia and China. The Kremlin condemned the move, and there was no indication of when tests might take place.",
                        "byline": "",
                        "contenttype": "Article",
                        "contenturl": "https://www.washingtonpost.com/politics/2025/10/29/trump-nuclear-test-plans/",
                        "displaydatetime": 1761788612000,
                        "headline": "Trump directs Pentagon to test nuclear weapons for first time since 1992",
                        "smallthumburl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/ZSNKSAYKGATUKZLPUKVIRYNROQ_size-normalized.jpg",
                        "systemid": "https://www.washingtonpost.com/politics/2025/10/29/trump-nuclear-test-plans/"
                    },
                    {
                        "blurb": "The leaders share an affinity for great-power-style foreign policy. That’s working — for now.",
                        "byline": "",
                        "contenttype": "Article",
                        "contenturl": "https://www.washingtonpost.com/opinions/2025/10/30/summit-trump-xi-china-us-tariffs/",
                        "displaydatetime": 1761832689000,
                        "headline": "Trump and Xi flexed. Who won?",
                        "label": {
                            "basic": {
                                "style": "opinions",
                                "text": "Opinion",
                                "url": "https://www.washingtonpost.com/opinions/2025/10/30/summit-trump-xi-china-us-tariffs/"
                            },
                            "transparency": {
                                "text": "",
                                "url": ""
                            }
                        },
                        "smallthumburl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/EK5JNVWGSEZTBR646V3OVAZP3M_size-normalized.jpg",
                        "systemid": "https://www.washingtonpost.com/opinions/2025/10/30/summit-trump-xi-china-us-tariffs/"
                    },
                    {
                        "blurb": "The bipartisan move is symbolic, as the House has passed a rule against legislation to block tariffs this year.",
                        "byline": "",
                        "contenttype": "Article",
                        "contenturl": "https://www.washingtonpost.com/business/2025/10/30/trump-tariffs-senate-vote/",
                        "displaydatetime": 1761844363000,
                        "headline": "Senate votes to quash Trump’s ‘Liberation Day’ global tariffs",
                        "smallthumburl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/267RZKAF7TYNK4Y7NS3GZ5IW5I_size-normalized.jpg",
                        "systemid": "https://www.washingtonpost.com/business/2025/10/30/trump-tariffs-senate-vote/"
                    },
                    {
                        "blurb": "Independents hold Trump and Republicans responsible for the shutdown by a 2-to-1 margin, according to a poll conducted by The Washington Post, ABC News and Ipsos.",
                        "byline": "",
                        "contenttype": "Article",
                        "contenturl": "https://www.washingtonpost.com/politics/2025/10/30/trump-shutdown-blame-poll/",
                        "displaydatetime": 1761847381000,
                        "headline": "Americans blame Trump and GOP more than Democrats for shutdown, poll finds",
                        "smallthumburl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/P4QDZQ63KXEXRKKSJ7NHGR3CTM_size-normalized.jpg",
                        "systemid": "https://www.washingtonpost.com/politics/2025/10/30/trump-shutdown-blame-poll/"
                    },
                    {
                        "blurb": "",
                        "byline": "",
                        "contenttype": "",
                        "contenturl": "https://www.washingtonpost.com/politics/",
                        "headline": "Politics - The Washington Post",
                        "smallthumburl": "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT2nlti1DQrCCuE9eufOK-6NrosDcWcq-NlLTYQtYjUEqN6nMsTtMHNXju8&s",
                        "systemid": "https://www.washingtonpost.com/politics/"
                    },
                    {
                        "blurb": "The Washington Post’s essential guide to power and influence in D.C. Trump isn’t on the ballot in New Jersey. But he’s a key figure in this year’s elections there.",
                        "byline": "",
                        "contenttype": "Article",
                        "contenturl": "https://www.washingtonpost.com/politics/2025/10/30/trump-is-casting-long-shadow-new-jerseys-elections/",
                        "displaydatetime": 1761818467000,
                        "headline": "Analysis | Trump is casting a long shadow on New Jersey’s elections",
                        "smallthumburl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/BNBIPNMST5LQ6RHMP2UNFVDWFM_size-normalized.jpg",
                        "systemid": "https://www.washingtonpost.com/politics/2025/10/30/trump-is-casting-long-shadow-new-jerseys-elections/"
                    }
                ]
            }
        """.trimIndent()
    }
}