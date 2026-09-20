package com.wapo.flagship.features.gifting.repo

import com.wapo.android.commons.constants.COOKIE
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.articles2.viewmodels.ViewModelTest
import com.wapo.flagship.features.gifting.models.*
import com.wapo.flagship.features.gifting.repo.GiftArticleSenderRepo.Companion.FAILURE
import com.wapo.flagship.features.gifting.repo.GiftArticleSenderRepo.Companion.NO_ARTICLES_LEFT_STATE
import com.wapo.flagship.features.gifting.repo.GiftArticleSenderRepo.Companion.SUCCESS
import com.wapo.flagship.features.gifting.services.GiftArticleService
import com.wapo.flagship.network.retrofit.network.APIResult
import com.wapo.flagship.util.MatcherUtils
import com.washingtonpost.android.paywall.PaywallConnector
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.PaywallCounterHelper
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import com.washingtonpost.android.paywall.newdata.model.WpUser
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import java.io.IOException

@ExperimentalCoroutinesApi
class GiftArticleSenderRepoTest : ViewModelTest() {
    @Mock
    lateinit var giftArticleService: GiftArticleService

    @Mock
    lateinit var remoteLogRepo: RemoteLogRepo

    private lateinit var repo: GiftArticleSenderRepo

    private val map =
        HashMap<String, String>().apply {
            put(COOKIE, "wapo_login_id=wpUser; wapo_secure_login_id=wpUser")
        }

    private val giftArticleSenderRequestBody = GiftArticleSenderRequestBody("url")

    @Mock
    lateinit var wpUser: WpUser

    @Mock
    lateinit var paywallService: PaywallService

    @Mock
    lateinit var connector: PaywallConnector

    @Before
    override fun setUp() {
        super.setUp()
        Mockito.mockStatic(PaywallService::class.java).let { ps ->
            ps.`when`<Any> { PaywallService.getInstance() }.thenReturn(paywallService)
            ps.`when`<Any> { PaywallService.getConnector() }.thenReturn(connector)
        }
        Mockito.`when`(paywallService.loggedInUser).thenReturn(wpUser)
        Mockito.`when`(wpUser.uuid).thenReturn("wpUser")
        Mockito.`when`(wpUser.secureLoginID).thenReturn("wpUser")
        mockRequestHeaders()
        remoteLogRepo = mockk(relaxed = true)
        repo = GiftArticleSenderRepo(giftArticleService, remoteLogRepo)
    }

    /**
     * This tests the get remaining count api where the response is successful (200) and the this article wasn't previously shared by the user.
     */
    @Test
    fun getRemainingCountHappyNotAlreadyShared() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleRemainingCountResponseBody(2, SUCCESS, null),
                        Headers.headersOf(),
                    ),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.Success)
            Assert.assertEquals((value as RemainingCountApiStatus.Success).remainingCount, 2)
            Assert.assertEquals(value.hasAlreadyShared, false)
        }

    /**
     * This tests the get remaining count api where the response is successful (200) and the this article was previously shared by the user.
     */
    @Test
    fun getRemainingCountHappyAlreadyShared() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleRemainingCountResponseBody(5, SUCCESS, null, true),
                        Headers.headersOf(),
                    ),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.Success)
            Assert.assertEquals((value as RemainingCountApiStatus.Success).remainingCount, 5)
            Assert.assertEquals(value.hasAlreadyShared, true)
        }

    /**
     * This tests the get remaining count api where the response is successful (200) with remaining count == 0 AND the this article was previously shared by the user.
     */
    @Test
    fun getRemainingCountHappyAlreadySharedButNoMoreLeft() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleRemainingCountResponseBody(0, SUCCESS, null, true),
                        Headers.headersOf(),
                    ),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.Success)
            Assert.assertEquals((value as RemainingCountApiStatus.Success).remainingCount, 0)
            Assert.assertEquals(value.hasAlreadyShared, true)
        }

    /**
     * This tests the get remaining count api where the api response is successful but status of the api is failure with reason no articles left state = 2256
     */
    @Test
    fun getRemainingCountNoMoreArticlesLeft() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleRemainingCountResponseBody(
                            null,
                            FAILURE,
                            NO_ARTICLES_LEFT_STATE,
                        ),
                        Headers.headersOf(),
                    ),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.NoRemainingArticles)
        }

    /**
     * This tests the get remaining count api where the api response is successful but status of the api is success with remaining count == 0
     */
    @Test
    fun getRemainingCountNoMoreArticlesLeftCase2() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleRemainingCountResponseBody(0, SUCCESS, null),
                        Headers.headersOf(),
                    ),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.NoRemainingArticles)
        }

    /**
     * This tests the get remaining count api where the api response is failure.
     */
    @Test
    fun getRemainingCountApiFailure() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Failure(202, ""),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.Failure)
        }

    /**
     * This tests the get remaining count api where the api response is failure because of an unknown error.
     */
    @Test
    fun getRemainingCountUnknownErrorFailure() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleRemainingCountResponseBody(null, SUCCESS, 40),
                        Headers.headersOf(),
                    ),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.Failure)
        }

    /**
     * This tests the get remaining count api where the api response is failure because of a network error.
     */
    @Test
    fun getRemainingCountNetworkFailure() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleRemainingCount(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.NetworkError(IOException()),
                )
            repo.getRemainingGiftCount("url")
            val value = repo.remainingCountStatus.value
            Assert.assertTrue(value is RemainingCountApiStatus.Failure)
        }

    /**
     * This tests the get gift url api where the response is successful (200)
     */
    @Test
    fun getGiftArticleUrlHappy() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleTokenWithUrl(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleTokenResponseBody(
                            remainingCount = 3,
                            state = null,
                            status = SUCCESS,
                            token = "123",
                            url = "https://wapo.st/xxxx",
                        ),
                        Headers.headersOf(),
                    ),
                )
            repo.getGiftArticleTokenWithUrl("url")
            val value = repo.requestUrlApiStatus.value
            Assert.assertTrue(value is RequestUrlApiStatus.Success)
        }

    /**
     * This tests the get gift url api where the api response is successful but the status is failure.
     */
    @Test
    fun getGiftArticleUrlFailure() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleTokenWithUrl(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Success(
                        GiftArticleTokenResponseBody(
                            remainingCount = 0,
                            state = null,
                            status = FAILURE,
                            token = null,
                            url = null,
                        ),
                        Headers.headersOf(),
                    ),
                )
            repo.getGiftArticleTokenWithUrl("url")
            val value = repo.requestUrlApiStatus.value
            Assert.assertTrue(value is RequestUrlApiStatus.Failure)
        }

    /**
     * This tests the get gift url api where the api response is a failure.
     */
    @Test
    fun getGiftArticleUrlApiFailure() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleTokenWithUrl(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.Failure(203, ""),
                )
            repo.getGiftArticleTokenWithUrl("url")
            val value = repo.requestUrlApiStatus.value
            Assert.assertTrue(value is RequestUrlApiStatus.Failure)
        }

    /**
     * This tests the get gift url api where the api response is a failure because of a network failure.
     */
    @Test
    fun getGiftArticleUrlNetworkFailure() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(
                    giftArticleService.getGiftArticleTokenWithUrl(
                        MatcherUtils.any(),
                        MatcherUtils.any(),
                    ),
                ).thenReturn(
                    APIResult.NetworkError(IOException()),
                )
            repo.getGiftArticleTokenWithUrl("url")
            val value = repo.requestUrlApiStatus.value
            Assert.assertTrue(value is RequestUrlApiStatus.Failure)
        }

    protected fun mockRequestHeaders() {
        Mockito.mockStatic(PaywallPrefHelper::class.java).let { ps ->
            ps
                .`when`<Any> { PaywallPrefHelper.getPrefTetroStateCookie() }
                .thenReturn("H4sIAAAAAAAA/6uuBQBDv6ajAgAAAA==")
        }
        Mockito.mockStatic(PaywallCounterHelper::class.java).let { ps ->
            ps
                .`when`<Any> { PaywallCounterHelper.getArticleListNotSynced(MatcherUtils.any()) }
                .thenReturn(listOf<ArticleStub>())
        }
        Mockito.`when`(connector.clientId).thenReturn("client_id")
        Mockito.`when`(connector.ipAddress).thenReturn("ip_address")
        Mockito.`when`(connector.appName).thenReturn("classic")
        Mockito.`when`(connector.deviceId).thenReturn("device_id")
        Mockito.`when`(connector.userAgent).thenReturn("user_agent")
        Mockito.`when`(connector.appVersion).thenReturn("6.0")
    }
}
