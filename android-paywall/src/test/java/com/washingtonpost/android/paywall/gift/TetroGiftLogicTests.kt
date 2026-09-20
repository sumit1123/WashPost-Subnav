package com.washingtonpost.android.paywall.gift

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.wapo.android.commons.logs.EventLog
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.util.PaywallBaseTest
import com.washingtonpost.android.paywall.features.tetro.TetroManager
import com.washingtonpost.android.paywall.features.tetro.TetroResponse
import com.washingtonpost.android.paywall.features.tetro.local.TetroLocalService
import com.washingtonpost.android.paywall.features.tetro.remote.TetroApiService
import com.washingtonpost.android.paywall.network.retrofit.APIResult
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
import java.util.concurrent.TimeoutException


@ExperimentalCoroutinesApi
class TetroGiftLogicTests : PaywallBaseTest() {

    private val responseValidNotExpired = "{\n" +
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

    private val responseExpired = "{\n" +
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

    private val responseInvalid = "{\n" +
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

    private val fakeToken = "dfadsasdf"
    private val fakeUrl = "www.washpost.com/somearticle"

    lateinit var tetroManager: TetroManager

    @Mock
    lateinit var tetroApi: TetroApiService.TetroApi

    @Mock
    lateinit var tetroLocalService: TetroLocalService

    val gson = Gson()


    @Before
    override fun setUp() {
        super.setUp()
        /*
            For mocking static classes.
         */

        Mockito.`when`(connector.isOnline).thenReturn(true)
        Mockito.`when`(connector.logE(EventLog.Builder())).then { }
        mockRequestHeaders()

        tetroManager = TetroManager(tetroApi, tetroLocalService)
    }

    /**
     * Gift token is valid and not expired
     */
    @Test
    fun testGiftValidNotExpired() = runTest(coroutinesTestRule.testDispatcher) {
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any()))
            .then {
                APIResult.Success(
                    gson.fromJson(responseValidNotExpired, TetroResponse::class.java),
                    Headers.headersOf()
                )
            }

        val state = tetroManager.processGiftToken(fakeToken, fakeUrl, null)
        Assert.assertNotNull(state)
        Assert.assertTrue(state is GiftState.ValidNotExpired)
    }

    /**
     * Gift token is expired
     */
    @Test
    fun testGiftExpired() = runTest(coroutinesTestRule.testDispatcher) {
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any()))
            .then {
                APIResult.Success(
                    gson.fromJson(responseExpired, TetroResponse::class.java),
                    Headers.headersOf()
                )
            }

        val state = tetroManager.processGiftToken(fakeToken, fakeUrl, null)
        Assert.assertNotNull(state)
        Assert.assertTrue(state is GiftState.Expired)
    }

    /**
     * Gift token is invalid
     */
    @Test
    fun testGiftNotValid() = runTest(coroutinesTestRule.testDispatcher) {
        Mockito.`when`(tetroApi.getMeterProxyData(anyNonNull(), anyNonNull(), any()))
            .then {
                APIResult.Success(
                    gson.fromJson(responseInvalid, TetroResponse::class.java),
                    Headers.headersOf()
                )
            }

        val state = tetroManager.processGiftToken(fakeToken, fakeUrl, null)
        Assert.assertNotNull(state)
        Assert.assertTrue(state is GiftState.NotGift)
    }

    /**
     * Network failure
     */
    @Test
    fun testNetworkFailure() = runTest(coroutinesTestRule.testDispatcher) {
        Mockito.`when`(tetroApi.getMeterData(MockitoHelper.anyObject(), MockitoHelper.anyObject()))
            .then {
                APIResult.NetworkError(throw TimeoutException())
            }

        val state = tetroManager.processGiftToken(fakeToken, fakeUrl, null)
        Assert.assertNotNull(state)
        Assert.assertTrue(state is GiftState.Failure)
    }

    /**
     * Network failure
     */
    @Test
    fun testJsonException() = runTest(coroutinesTestRule.testDispatcher) {
        Mockito.`when`(tetroApi.getMeterData(MockitoHelper.anyObject(), MockitoHelper.anyObject()))
            .then {
                throw JsonParseException("")
            }

        val state = tetroManager.processGiftToken(fakeToken, fakeUrl, null)
        Assert.assertNotNull(state)
        Assert.assertTrue(state is GiftState.Failure)
    }
}

object MockitoHelper {
    fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> uninitialized(): T = null as T
}

