package com.wapo.flagship.features.gifting.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager;
import com.wapo.flagship.features.articles2.viewmodels.ViewModelTest
import com.wapo.flagship.util.MatcherUtils
import com.washingtonpost.android.paywall.PaywallConnector
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.features.tetro.TetroManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class GiftArticleRecipientViewModelTest : ViewModelTest() {
    private val fakeToken = "dfadsasdf"
    private val fakeUrl = "www.washpost.com/somearticle"

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    @Mock
    lateinit var paywallService: PaywallService

    @Mock
    lateinit var tetroManager: TetroManager

    @Mock
    lateinit var connector: PaywallConnector

    private lateinit var recipientViewModel: GiftArticleRecipientViewModel

    @Before
    override fun setUp() {
        super.setUp()
        /*
            For mocking static classes.
         */
        Mockito.mockStatic(PaywallService::class.java)
            .`when`<Any> { PaywallService.getInstance() }
            .thenAnswer { paywallService }
        Mockito.`when`(paywallService.tetroManager).thenReturn(tetroManager)
        Mockito.mockStatic(PreferenceManager::class.java)
            .`when`<Any> { PreferenceManager.getDefaultSharedPreferences(context) }
            .thenAnswer { sharedPreferences }
        Mockito.`when`(sharedPreferences.getStringSet(MatcherUtils.any(), MatcherUtils.any()))
            .thenReturn(null)
        Mockito.`when`(PaywallService.getConnector()).thenAnswer { connector }
        BDDMockito.given(connector.isOnline).willReturn(true)
        recipientViewModel = GiftArticleRecipientViewModel(context, testCoroutineDispatcherProvider)
    }

    /**
     * has gift token
     */
    @Test
    fun testGiftCodeFound() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(tetroManager.processGiftToken(fakeToken, fakeUrl, ""))
                .thenReturn(GiftState.ValidNotExpired)
            recipientViewModel.processGiftToken(fakeToken, fakeUrl)
            recipientViewModel.giftArticleState.observeForever {
                Assert.assertNotNull(it)
                Assert.assertTrue(it is GiftState.ValidNotExpired)
            }
        }

    /**
     * does not have gift token
     */
    @Test
    fun testGiftCodeNotFound() =
        runTest(coroutinesTestRule.testDispatcher) {
            Mockito
                .`when`(paywallService.tetroManager.processGiftToken(fakeToken, fakeUrl, null))
                .then { GiftState.ValidNotExpired }
            recipientViewModel.processGiftToken(null, fakeUrl)
            recipientViewModel.giftArticleState.observeForever {
                Assert.assertNull(it)
            }
        }
}
