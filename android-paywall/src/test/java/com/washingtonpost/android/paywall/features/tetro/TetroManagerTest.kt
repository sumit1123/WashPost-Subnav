package com.washingtonpost.android.paywall.features.tetro

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.washingtonpost.android.paywall.events.WallState
import com.washingtonpost.android.paywall.util.PaywallBaseTest
import com.washingtonpost.android.paywall.features.tetro.local.TetroLocalService
import com.washingtonpost.android.paywall.features.tetro.remote.ArticleObj
import com.washingtonpost.android.paywall.features.tetro.remote.TetroApiService
import com.washingtonpost.android.paywall.helper.PaywallDbHelper
import com.washingtonpost.android.paywall.metering.MeteringService
import com.washingtonpost.android.paywall.network.retrofit.APIResult
import com.washingtonpost.android.paywall.newdata.model.WpUser
import com.washingtonpost.android.paywall.util.anyNonNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import java.lang.Exception
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
class TetroManagerTest : PaywallBaseTest() {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharePrefs: SharedPreferences

    @Mock
    private lateinit var applicationContext: Context

    private lateinit var tetroManager: TetroManager

    @Mock
    lateinit var tetroApi: TetroApiService.TetroApi

    @Mock
    private lateinit var tetroLocalService: TetroLocalService

    private lateinit var meteringService: MeteringService
    private lateinit var paywallDbHelper: PaywallDbHelper
    private val gson = Gson()

    private val responsePaywall = "{\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"action\": 3,\n" +
            "    \"data\": {\n" +
            "        \"meterCount\": 2,\n" +
            "        \"actionCodes\": [\n" +
            "        ],\n" +
            "        \"articleWeight\": 1,\n" +
            "        \"meterCycleDays\": 45\n" +
            "    },\n" +
            "    \"granted\": false\n" +
            "}"

    private val responseRegWall = "{\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"action\": 9,\n" +
            "    \"data\": {\n" +
            "        \"meterCount\": 2,\n" +
            "        \"actionCodes\": [\n" +
            "        ],\n" +
            "        \"articleWeight\": 1,\n" +
            "        \"meterCycleDays\": 45\n" +
            "    },\n" +
            "    \"granted\": false\n" +
            "}"

    private val responseNoWall = "{\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"action\": 0,\n" +
            "    \"data\": {\n" +
            "        \"meterCount\": 2,\n" +
            "        \"actionCodes\": [\n" +
            "        ],\n" +
            "        \"articleWeight\": 1,\n" +
            "        \"meterCycleDays\": 45\n" +
            "    },\n" +
            "    \"granted\": false\n" +
            "}"

    private val responseSoftWall = "{\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"action\": 6,\n" +
            "    \"data\": {\n" +
            "        \"meterCount\": 2,\n" +
            "        \"actionCodes\": [\n" +
            "        ],\n" +
            "        \"articleWeight\": 1,\n" +
            "        \"meterCycleDays\": 45\n" +
            "    },\n" +
            "    \"granted\": false\n" +
            "}"

    private val responseValidNotExpiredGift = "{\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"action\": 6,\n" +
            "    \"data\": {\n" +
            "        \"meterCount\": 2,\n" +
            "        \"actionCodes\": [\n" +
            "            \"1600\",\n" +
            "            \"1601\",\n" +
            "            \"w_1610\"\n" +
            "        ],\n" +
            "        \"articleWeight\": 1,\n" +
            "        \"meterCycleDays\": 45\n" +
            "    },\n" +
            "    \"granted\": false\n" +
            "}"

    private val responseExpiredGift = "{\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"action\": 6,\n" +
            "    \"data\": {\n" +
            "        \"meterCount\": 2,\n" +
            "        \"actionCodes\": [\n" +
            "            \"1600\",\n" +
            "            \"1699\",\n" +
            "            \"w_1698\"\n" +
            "        ],\n" +
            "        \"articleWeight\": 1,\n" +
            "        \"meterCycleDays\": 45\n" +
            "    },\n" +
            "    \"granted\": false\n" +
            "}"

    private val responseInvalidGift = "{\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"action\": 6,\n" +
            "    \"data\": {\n" +
            "        \"meterCount\": 2,\n" +
            "        \"actionCodes\": [\n" +
            "        ],\n" +
            "        \"articleWeight\": 1,\n" +
            "        \"meterCycleDays\": 45\n" +
            "    },\n" +
            "    \"granted\": false\n" +
            "}"

    private val responseMapWall = "{\n" +
            "    \"action\": 0,\n" +
            "    \"data\": {\n" +
            "        \"actionCodes\": [],\n" +
            "        \"meterCount\": 2,\n" +
            "      \n" +
            "        \"prompts\": [{\n" +
            "            \"id\": \"softwall-recirc-1217f1926a62\", \n" +
            "            \"itid\": \"\",     \t\n" +
            "            \"promo\": {\t\t\n" +
            "               \"logo\": \"for-you\", \t\n" +
            "               \"labels\": [\"<b>FOR YOU</b>\"]\t\t\n" +
            "            },\n" +
            "            \"trigger\": {\t\t\n" +
            "              \"type\": \"scroll\", \t\n" +
            "              \"depth\": 50\t\t\n" +
            "            },\n" +
            "            \"appearance\": {\n" +
            "                \"dismissExpirationSeconds\": 86400, \t\n" +
            "                \"maxSnooze\": 5\t\t\n" +
            "            }\n" +
            "        }]\n" +
            "    }\n" +
            "}"

    private val article = ArticleObj(
        "https://www.washingtonpost.com/business/2022/06/15/7-tips-on-how-to-survive-a-recession",
        null
    )

    @Before
    override fun setUp() {
        super.setUp()

        Mockito.`when`(tetroLocalService.updateMeterData(Mockito.any(TetroResponse::class.java)))
            .then { }
//        val headers = Mockito.any(Headers::class.java) ?: Headers.headersOf()
        Mockito.`when`(tetroLocalService.storeCookies(anyNonNull())).then {  }

        tetroManager = TetroManager(tetroApi, tetroLocalService)
    }

    private fun mockLoggedInCookies() {
        Mockito.`when`(paywallService.isWpUserLoggedIn).then { true }
        Mockito.`when`(paywallService.loggedInUser).then {
            val wpUser = WpUser()
            wpUser.uuid = "UUID"
            wpUser.secureLoginID = "SecureLoginId"
            wpUser
        }
        Mockito.`when`(connector.subAcctMgmt).then { "subAcctMgmt" }
    }

    private fun mockNotLoggedInCookies() {
        Mockito.`when`(paywallService.isWpUserLoggedIn).then { false }
    }


    @Test
    fun getPaywallTest() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responsePaywall,TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article, null, null)
        println(wallState)
        Assert.assertTrue(wallState is WallState.Paywall)
    }

    @Test
    fun getRegwallTest() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responseRegWall,TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article, null, null)
        println(wallState)
        Assert.assertTrue(wallState is WallState.Regwall)
    }

    @Test
    fun getNoWallTest() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responseNoWall,TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article, null, null)
        println(wallState)
        Assert.assertTrue(wallState is WallState.NoWall)
    }

    @Test
    fun getSoftWallTest() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responseSoftWall,TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article, null, null)
        println(wallState)
        Assert.assertTrue(wallState is WallState.Softwall)
    }

    @Test
    fun getNetworkErrorTest() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.NetworkError(Exception())
        Mockito.`when`(tetroApi.getMeterData(anyNonNull(), anyNonNull())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article, "token")
        println(wallState)
        Assert.assertTrue(wallState is WallState.Failed)
    }

    @Test
    fun getFailureTest() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Failure(403,null)
        Mockito.`when`(tetroApi.getMeterData(anyNonNull(), anyNonNull())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article, "token")
        println(wallState)
        Assert.assertTrue(wallState is WallState.Failed)
    }

    @Test
    fun getExceptionTest() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        Mockito.`when`(tetroApi.getMeterData(anyNonNull(), anyNonNull())).then {
            throw Exception()
        }
        val wallState = tetroManager.getWallResponse(article, "token")
        println(wallState)
        Assert.assertTrue(wallState is WallState.Failed)
    }

    @Test
    fun getGiftValidNotExpired() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responseValidNotExpiredGift,TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterData(anyNonNull(), anyNonNull())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article,null, "token")
        println(wallState)
        Assert.assertTrue(wallState is WallState.GiftWall)
    }

    @Test
    fun getGiftNotValid() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responseInvalidGift,TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterData(anyNonNull(), anyNonNull())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article,null, "token")
        println(wallState)
        Assert.assertTrue(wallState is WallState.GiftWall)
    }

    @Test
    fun getGiftExpired() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responseExpiredGift,TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterData(anyNonNull(), anyNonNull())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article,null, "token")
        println(wallState)
        Assert.assertTrue(wallState is WallState.GiftWall)
    }

    @Test
    fun getMapWall() = runTest(coroutinesTestRule.testDispatcher) {
        mockRequestHeaders()
        val response = APIResult.Success(gson.fromJson(responseMapWall, TetroResponse::class.java), Headers.headersOf())
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any())).thenReturn(response)
        val wallState = tetroManager.getWallResponse(article, null, null)
        println(wallState)
        Assert.assertTrue(wallState is WallState.MapWall)
    }
}