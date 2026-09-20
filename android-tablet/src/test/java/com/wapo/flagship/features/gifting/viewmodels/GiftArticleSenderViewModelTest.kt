package com.wapo.flagship.features.gifting.viewmodels

import android.net.Uri
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.articles2.viewmodels.ViewModelTest
import com.wapo.flagship.features.gifting.models.RemainingCountApiStatus
import com.wapo.flagship.features.gifting.models.RequestUrlApiStatus
import com.wapo.flagship.features.gifting.repo.GiftArticleSenderRepo
import com.wapo.flagship.features.gifting.states.GiftSendUiState
import com.wapo.flagship.util.MatcherUtils
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.newdata.model.WpUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class GiftArticleSenderViewModelTest : ViewModelTest() {
    @Mock
    lateinit var paywallService: PaywallService

    @Mock
    lateinit var giftSenderRepo: GiftArticleSenderRepo

    private lateinit var senderViewModel: GiftArticleSenderViewModel

    private val requestUrlApiStatus = LiveEvent<RequestUrlApiStatus>()

    private val remainingCountApiStatus = LiveEvent<RemainingCountApiStatus>()

    private val giftArticleUrl = "www.washingtonpost.com/somearticle"

    private val bitlyUrl = "https://www.wapo.st.com/somecode"

    @Mock
    lateinit var wpUser: WpUser

    @Mock
    lateinit var uri: Uri

    @Mock
    lateinit var builder: Uri.Builder

    @Before
    override fun setUp() {
        super.setUp()
        /*
            For mocking static classes.
         */

        Mockito.mockStatic(PaywallService::class.java)
            .`when`<Any> { PaywallService.getInstance() }.thenAnswer { paywallService }
        Mockito.`when`(paywallService.loggedInUser).thenReturn(wpUser)
        Mockito.`when`(giftSenderRepo.remainingCountStatus).thenReturn(remainingCountApiStatus)
        Mockito.`when`(giftSenderRepo.requestUrlApiStatus).thenReturn(requestUrlApiStatus)
        Mockito.mockStatic(Uri::class.java).`when`<Any> { Uri.parse(anyString()) }.thenReturn(uri)
        Mockito.`when`(uri.buildUpon()).thenReturn(builder)
        Mockito.`when`(builder.appendQueryParameter(anyString(), anyString())).then { builder }
        Mockito.`when`(builder.clearQuery()).then { builder }
        Mockito.`when`(builder.fragment(MatcherUtils.any())).then { builder }
        Mockito.`when`(builder.build()).then { uri }
        Mockito.`when`(uri.toString()).then { "Something" }
        senderViewModel =
            GiftArticleSenderViewModel(
                giftSenderRepo,
                testCoroutineDispatcherProvider,
            )
        senderViewModel.giftUiState.observeForever {
            println(senderViewModel.giftUiState.value)
        }
    }

    /**
     * Checks if Startup state is set when Viewmodel is created
     */
    @Test
    fun testStartupGiftFlow() {
        senderViewModel.giftUiState.observeForever {
            Assert.assertTrue(it is GiftSendUiState.Startup)
        }
    }

    /**
     * Checks if NoSub state is called when user is:
     * - Not Subscribed
     * - Signed In
     */
    @Test
    fun testStartGiftFlowNoSubSignedIn() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
        senderViewModel.startGiftingFlow()
        senderViewModel.giftUiState.observeForever {
            Assert.assertTrue(it is GiftSendUiState.NoSub)
        }
    }

    /**
     * Checks if NoSub state is called when user is:
     * - Not Subscribed
     * - Not Signed In
     */
    @Test
    fun testStartGiftFlowNoSubNotSignedIn() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(false)
        senderViewModel.startGiftingFlow()
        senderViewModel.giftUiState.observeForever {
            Assert.assertTrue(it is GiftSendUiState.NoSub)
        }
    }

    /**
     * Checks if NotSignedIn state is called when user is:
     * - Subscribed
     * - Not Signed In
     */
    @Test
    fun testStartGiftFlowHasSubNotSignedIn() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
        Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(false)
        senderViewModel.startGiftingFlow()
        senderViewModel.giftUiState.observeForever {
            Assert.assertTrue(it is GiftSendUiState.NotSignedIn)
        }
    }

    /**
     * Checks if SignedInSub state is called when user is:
     * - Subscribed
     * - Signed In
     */
    @Test
    fun testStartGiftFlowCanGift() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
        Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
        senderViewModel.startGiftingFlow()
        senderViewModel.giftUiState.observeForever {
            Assert.assertTrue(it is GiftSendUiState.SignedInSub)
        }
    }

    /**
     * Checks if UI State is Gift when getRemainingGiftCount returns a Success
     */
    @Test
    fun testGiftNewUiState() =
        runTest(coroutinesTestRule.testDispatcher) {
            var resumedDispatcher = false
            Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
            Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
            Mockito.`when`(giftSenderRepo.getRemainingGiftCount(giftArticleUrl)).then {
                remainingCountApiStatus.postValue(RemainingCountApiStatus.Success(5, false))
            }
            senderViewModel.signedInSubscriberTappedGiftIcon(giftArticleUrl)
            senderViewModel.giftUiState.observeForever {
                if (resumedDispatcher) {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Gift)
                } else {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Loading)
                }
            }
            resumedDispatcher = true
//        coroutinesTestRule.testDispatcher.resumeDispatcher()
        }

    /**
     * Checks if UI State is GiftAgain when getRemainingGiftCount returns a Success with hasAlreadyShared = true
     */
    @Test
    fun testGiftAgainUiState() =
        runTest(coroutinesTestRule.testDispatcher) {
            var resumedDispatcher = false
            Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
            Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
            Mockito.`when`(giftSenderRepo.getRemainingGiftCount(giftArticleUrl)).then {
                remainingCountApiStatus.postValue(RemainingCountApiStatus.Success(5, true))
            }
            senderViewModel.signedInSubscriberTappedGiftIcon(giftArticleUrl)
            senderViewModel.giftUiState.observeForever {
                if (resumedDispatcher) {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.GiftAgain)
                } else {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Loading)
                }
            }
            resumedDispatcher = true
//        coroutinesTestRule.testDispatcher.resumeDispatcher()
        }

    /**
     * Checks if UI State is NoGifts when getRemainingGiftCount returns a NoRemainingArticles
     */
    @Test
    fun testNoGiftsUiState() =
        runTest(coroutinesTestRule.testDispatcher) {
            var resumedDispatcher = false
            Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
            Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
            Mockito.`when`(giftSenderRepo.getRemainingGiftCount(giftArticleUrl)).then {
                remainingCountApiStatus.postValue(RemainingCountApiStatus.NoRemainingArticles)
            }
            senderViewModel.signedInSubscriberTappedGiftIcon(giftArticleUrl)
            senderViewModel.giftUiState.observeForever {
                if (resumedDispatcher) {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.NoGifts)
                } else {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Loading)
                }
            }
            resumedDispatcher = true
//        coroutinesTestRule.testDispatcher.resumeDispatcher()
        }

    /**
     * Checks if UI State is Failure when getRemainingGiftCount returns a Failure
     */
    @Test
    fun testNetworkFailureUiState() =
        runTest(coroutinesTestRule.testDispatcher) {
            var resumedDispatcher = false
            Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
            Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
            Mockito.`when`(giftSenderRepo.getRemainingGiftCount(giftArticleUrl)).then {
                remainingCountApiStatus.postValue(RemainingCountApiStatus.Failure)
            }
            senderViewModel.signedInSubscriberTappedGiftIcon(giftArticleUrl)
            senderViewModel.giftUiState.observeForever {
                if (resumedDispatcher) {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Failure)
                } else {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Loading)
                }
            }
            resumedDispatcher = true
//        coroutinesTestRule.testDispatcher.resumeDispatcher()
        }

    /**
     * Checks if UI State is GiftTokenUrl when getGiftArticleTokenWithUrl returns a Success with url
     */
    @Test
    fun testShareGiftSuccess() =
        runTest(coroutinesTestRule.testDispatcher) {
            var resumedDispatcher = false
            Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
            Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
            Mockito.`when`(giftSenderRepo.getGiftArticleTokenWithUrl(giftArticleUrl)).then {
                requestUrlApiStatus.postValue(RequestUrlApiStatus.Success(bitlyUrl))
            }
            senderViewModel.shareButtonClickedOnGiftBottomSheet(giftArticleUrl)
            senderViewModel.giftUiState.observeForever {
                if (resumedDispatcher) {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.GiftTokenUrl)
                } else {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Loading)
                }
            }
            resumedDispatcher = true
//        coroutinesTestRule.testDispatcher.resumeDispatcher()
        }

    /**
     * Checks if UI State is Failure when getGiftArticleTokenWithUrl returns a Failure
     */
    @Test
    fun testShareGiftFailure() =
        runTest(coroutinesTestRule.testDispatcher) {
            var resumedDispatcher = false
            Mockito.`when`(paywallService.isPremiumUser).thenReturn(true)
            Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(true)
            Mockito.`when`(giftSenderRepo.getGiftArticleTokenWithUrl(giftArticleUrl)).then {
                requestUrlApiStatus.postValue(RequestUrlApiStatus.Failure)
            }
            senderViewModel.shareButtonClickedOnGiftBottomSheet(giftArticleUrl)
            senderViewModel.giftUiState.observeForever {
                if (resumedDispatcher) {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Failure)
                } else {
                    Assert.assertTrue(senderViewModel.giftUiState.value is GiftSendUiState.Loading)
                }
            }
            resumedDispatcher = true
//        coroutinesTestRule.testDispatcher.resumeDispatcher()
        }
}
