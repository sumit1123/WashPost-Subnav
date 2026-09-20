package com.washingtonpost.android.paywall.metering

import android.content.Context
import android.content.SharedPreferences
import com.washingtonpost.android.paywall.features.tetro.TetroManager
import com.washingtonpost.android.paywall.helper.PaywallCounterHelper
import com.washingtonpost.android.paywall.helper.PaywallDbHelper
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import com.washingtonpost.android.paywall.util.PaywallBaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class TetroLogicTests : PaywallBaseTest() {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharePrefs: SharedPreferences

    @Mock
    private lateinit var applicationContext: Context

    @Mock
    private lateinit var tetroManager: TetroManager

    private val meteringPrefsMock = mockStatic(MeteringPrefs::class.java)
    private val paywallCounterMock = mockStatic(PaywallCounterHelper::class.java)

    private lateinit var meteringService: MeteringService
    private lateinit var paywallDbHelper: PaywallDbHelper

    private var count: Float = 0f
    private var weight: Float = 0f

    private val tetroDbMap = mutableMapOf<String, ArticleStub?>()

    private val weightMap = hashMapOf<String, Float>(
        "article_url_1" to 1f,
        "article_url_2" to 2f,
        "article_url_3" to 3f,
        "article_url_4" to 4f,
        "article_url_5" to 5f,
    )

    private val stubList = listOf<ArticleStub>(
        ArticleStub("article1", "article_url_0"),
        ArticleStub("article1", "article_url_1"),
        ArticleStub("article1", "article_url_X"),
        ArticleStub("article1", "article_url_2"),
        ArticleStub("article1", "article_url_4"),
    )

    @Before
    override fun setUp() {
        super.setUp()
        meteringService = MeteringService()
        meteringService.skipTTLcheck = true
        paywallDbHelper = PaywallDbHelper(context)

        `when`(tetroManager.getWeightedArticles(true)).then { weightMap }
        `when`(paywallService.tetroManager).then { tetroManager }
    }

    @Test
    fun normal_article_calculation_test() {
        println("\n-----Normal Article Limit Tests---------")
        initMeterPrefs(2)
        testProcessWeights(stubList[0], false)
        testProcessWeights(stubList[1], false)
        testProcessWeights(stubList[2], true)
    }

    @Test
    fun weighted_article_calculation_test() {
        println("\n-----Weighted Article Limit Tests---------")
        initMeterPrefs(3)
        testProcessWeights(stubList[0], false)
        testProcessWeights(stubList[4], true)
        testProcessWeights(stubList[3], false)
        testProcessWeights(stubList[1], true)
    }

    @Test
    fun tetro_limit_logic_test() {
        println("\n-----Complete Tetro Metering Logic Test---------")
        tetroDbMap.clear()
        initMeterPrefs(2)
        testIsAtTetroLimit(stubList[0], shouldPaywall = false, storedInDb = false)
        testIsAtTetroLimit(stubList[0], shouldPaywall = false, storedInDb = true)
        testIsAtTetroLimit(stubList[3], shouldPaywall = true, storedInDb = false)
        testIsAtTetroLimit(stubList[4], shouldPaywall = true, storedInDb = false)
        testIsAtTetroLimit(stubList[2], shouldPaywall = false, storedInDb = false)
        testIsAtTetroLimit(stubList[2], shouldPaywall = false, storedInDb = true)
        testIsAtTetroLimit(stubList[1], shouldPaywall = true, storedInDb = false)
    }

    private fun testProcessWeights(articleStub: ArticleStub, isOverLimit: Boolean) {
        setWeight(articleStub)
        assert(meteringService.processWeightedLimit(articleStub) == isOverLimit)
        println("Article : ${articleStub.url} - count: $count | weight: $weight")
    }

    private fun testIsAtTetroLimit(stub: ArticleStub, shouldPaywall: Boolean, storedInDb: Boolean) {
        setWeight(stub)
        stub.url?.apply {
            mockDbGetArticleByUrl(this)
        }

        assert(tetroDbMap.contains(stub.url) == storedInDb)
        assert(meteringService.isAtTetroLimit(stub) == shouldPaywall)
        println("Article : ${stub.url} - count: $count | weight: $weight | shouldPaywall: $shouldPaywall | storedInDb: $storedInDb")
    }

    private fun initMeterPrefs(limit: Int) {
        println("Limit: $limit")
        count = 0f
        meteringPrefsMock.`when`<Any> { MeteringPrefs.getCurrentArticleCount() }.then { count }
        meteringPrefsMock.`when`<Any> { MeteringPrefs.getMaxArticleLimit() }.then { limit }
        meteringPrefsMock.`when`<Any> { MeteringPrefs.setCurrentArticleCount(
            anyString(),
            anyFloat()
        ) }.then { updateCount() }
    }

    private fun mockDbGetArticleByUrl(url: String) {
        paywallCounterMock.`when`<Any> {PaywallCounterHelper.getArticleByUrl(any(),any())}
            .then { tetroDbMap.getOrDefault(url, null) }
        paywallCounterMock.`when`<Any> {PaywallCounterHelper.insertArticle(any(),any())}
            .then {
                tetroDbMap[url] = ArticleStub("", url)
                Any()
            }
    }


    private fun updateCount() {
        count += weight
    }

    private fun setWeight(articleStub: ArticleStub) {
        weight = if (weightMap.containsKey(articleStub.url)) weightMap[articleStub.url]!! else 1f
    }

}

