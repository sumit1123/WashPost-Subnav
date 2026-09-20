package com.wapo.flagship.features.articles2.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles2.paywall.WallUiEvent
import com.wapo.flagship.util.MatcherUtils
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.events.WallState
import com.washingtonpost.android.paywall.features.tetro.TetroManager
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import com.washingtonpost.android.paywall.newdata.model.WpUser
import com.washingtonpost.android.save.SavedArticleManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.stubbing.Answer

@ExperimentalCoroutinesApi
class ArticleWallHelperViewModelTest : ViewModelTest() {
    /*
    @Mock
    lateinit var savedArticleManager: SavedArticleManager

    @Mock
    lateinit var paywallService: PaywallService

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    @Mock
    lateinit var wpUser: WpUser

    @Mock
    lateinit var uri: Uri

    @Mock
    lateinit var builder: Uri.Builder

    @Mock
    lateinit var tetroManager: TetroManager

    private lateinit var viewmodel: ArticleWallHelperViewModel

    private val paywallConf =
        "{\n" +
                "        \"metering\": {\n" +
                "            \"sync\": \"https://www.washingtonpost.com/tetro/metering/v2/engine/metering/apps/evaluate\",\n" +
                "            \"queue\": 3,\n" +
                "            \"age\": 86400,\n" +
                "            \"mapping\": {\n" +
                "                \"w_905\": \"reddit\"\n" +
                "            },\n" +
                "            \"mapping2\": [\n" +
                "                { \"action\": 3, \"code\": \"w_3oatp\", \"blocker\": \"oatp\" }," +
                "                { \"action\": 9, \"code\": \"w_905\", \"blocker\": \"reddit\" }" +
                "            ]\n" +
                "         }\n" +
                "}"

    @Before
    override fun setUp() {
        super.setUp()
        /*
            For mocking static classes.
         */

        //PaywallConfigurator.loadPaywallConfigurator(paywallConf)
        Mockito.mockStatic(PaywallService::class.java).`when`<Any> { PaywallService.getInstance() }
            .thenAnswer { paywallService }
        Mockito.mockStatic(PreferenceManager::class.java)
            .`when`<Any> { PreferenceManager.getDefaultSharedPreferences(context) }
            .thenAnswer { sharedPreferences }
        Mockito.`when`(sharedPreferences.getStringSet(MatcherUtils.any(), MatcherUtils.any()))
            .thenReturn(null)
        Mockito.mockStatic(Uri::class.java).`when`<Any> { Uri.parse(anyString()) }.thenReturn(uri)
        Mockito.mockStatic(Log::class.java).`when`<Any> { Logger.d(anyString(), anyString()) }
            .thenReturn(1)
        Mockito.`when`(uri.buildUpon()).thenReturn(builder)
        Mockito.`when`(builder.appendQueryParameter(anyString(), anyString())).then { builder }
        Mockito.`when`(builder.build()).then { uri }
        Mockito.`when`(uri.toString()).then { "Something" }
        Mockito.`when`(paywallService.loggedInUser).thenReturn(wpUser)
        Mockito.`when`(paywallService.tetroManager).thenReturn(tetroManager)
        viewmodel = ArticleWallHelperViewModel(context, testCoroutineDispatcherProvider, savedArticleManager)
    }

    /**
     * This case is used to test scenario when user wants to sign in as someone else.
     * They first need to log out.
     * This tests the logic that we only start the sign in flow after current user is logged out.
     */
    @Test
    fun testLogOutUser() = runTest {
        var loggedOut = false

        Mockito.`when`(paywallService.logOutCurrentUser()).thenAnswer(
            Answer<Any> {
                loggedOut = true
            },
        )
        viewmodel.logOutUser()
        Assert.assertFalse(loggedOut)
        advanceUntilIdle()
        Assert.assertTrue(loggedOut)
    }

    @Test
    fun testShowPaywall() =
        runTest(coroutinesTestRule.testDispatcher) {
            val articleStub =
                ArticleStub(
                    title = "title",
                    url = "https://www.washingtonpost.com/politics/2022/06/16/how-americans-feel-about-jan-6-hearings-so-far",
                    section = "politics",
                )

            Mockito.`when`(tetroManager.getWallResponse(MatcherUtils.any(), MatcherUtils.any(), MatcherUtils.any())).thenReturn(
                WallState.Paywall(listOf()),
            )
            viewmodel.getDelayedTetroMetering(articleStub)
            viewmodel.paywallEvent.observeForever {
                Assert.assertTrue(it is WallUiEvent.ShowPaywall)
                println("$it shown")
            }
        }

    @Test
    fun testShowNoRegwallMatch() =
        runTest(coroutinesTestRule.testDispatcher) {
            val articleStub =
                ArticleStub(
                    title = "title",
                    url = "https://www.washingtonpost.com/politics/2022/06/16/how-americans-feel-about-jan-6-hearings-so-far",
                    section = "politics",
                )

            Mockito.`when`(tetroManager.getWallResponse(MatcherUtils.any(), MatcherUtils.any(), MatcherUtils.any())).then {
                WallState.Regwall(
                    listOf("w_100"),
                )
            }
            viewmodel.getDelayedTetroMetering(articleStub)
            viewmodel.paywallEvent.observeForever {
                Assert.assertTrue(it is WallUiEvent.ShowPaywall)
                println("$it shown")
            }
        }

    @Test
    fun testShowRegwallMatch() =
        runTest(coroutinesTestRule.testDispatcher) {
            val articleStub =
                ArticleStub(
                    title = "title",
                    url = "https://www.washingtonpost.com/politics/2022/06/16/how-americans-feel-about-jan-6-hearings-so-far",
                    section = "politics",
                )

            Mockito.`when`(tetroManager.getWallResponse(MatcherUtils.any(), MatcherUtils.any(), MatcherUtils.any())).then {
                WallState.Regwall(
                    listOf("w_905"),
                )
            }
            viewmodel.getDelayedTetroMetering(articleStub)
            viewmodel.paywallEvent.observeForever {
                Assert.assertTrue(it is WallUiEvent.ShowRegwall)
                println("$it shown")
            }
        }

    @Test
    fun testShowGiftwall() =
        runTest(coroutinesTestRule.testDispatcher) {
            val articleStub =
                ArticleStub(
                    title = "title",
                    url = "https://www.washingtonpost.com/politics/2022/06/16/how-americans-feel-about-jan-6-hearings-so-far",
                    section = "politics",
                )

            Mockito.`when`(tetroManager.getWallResponse(MatcherUtils.any(), MatcherUtils.any(), MatcherUtils.any())).then {
                WallState.GiftWall(
                    GiftState.Expired,
                )
            }
            viewmodel.getDelayedTetroMetering(articleStub, giftToken = "Token")
            viewmodel.paywallEvent.observeForever {
                Assert.assertTrue(it is WallUiEvent.ShowGiftWall)
                println("$it shown")
            }
        }

     */
}
