package com.washingtonpost.android.paywall.features.promocodes.viemodels

import com.squareup.moshi.Moshi
import com.wapo.android.commons.util.LiveEvent
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.models.PromoCodeJsonAdapter
import com.washingtonpost.android.paywall.models.PromoCodeRequestState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.mockito.Mock
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class PromoCodeViewModelTest : ViewModelTest() {

    private val validSerializedPromoCode = "{\"promoCode\": \"abc\",\n" +
            "    \"promoName\": \"Test Promo Codes\",\n" +
            "    \"startDate\": \"2021-07-23 00:00:00\",\n" +
            "    \"endDate\": \"2100-08-22 00:00:00\",\n" +
            "    \"productId\": \"test.wp.classic.basic\",\n" +
            "    \"promoTermType\": \"FREE_TRIAL\",\n" +
            "    \"promoTerm\": \"DAY\",\n" +
            "    \"promoDuration\": 60}"

    private val expiredSerializedPromoCode = "{\"promoCode\": \"abc\",\n" +
            "    \"promoName\": \"Test Promo Codes\",\n" +
            "    \"startDate\": \"2021-07-23 00:00:00\",\n" +
            "    \"endDate\": \"2021-07-22 00:00:00\",\n" +
            "    \"productId\": \"test.wp.classic.basic\",\n" +
            "    \"promoTermType\": \"FREE_TRIAL\",\n" +
            "    \"promoTerm\": \"DAY\",\n" +
            "    \"promoDuration\": 60}"

    private lateinit var viewModel: PromoCodeViewModel

    private val dataSource = LiveEvent<PromoCodeRequestState>()

    @Before
    override fun setUp() {
        super.setUp()
        /*
            For mocking static classes.
         */

        Mockito.`when`(paywallService.promoCodeRequestLiveData).thenReturn(dataSource)
        viewModel = PromoCodeViewModel()
    }

    /**
     * Checks if the promo code retrieved from the shared prefs is valid
     * valid mock data is passed.
     */
    @Test
    fun testPromoCodeRetrievedFromSharedPref_Happy() {
        Mockito.mockStatic(PaywallPrefHelper::class.java).`when`<Any> { PaywallPrefHelper.getPromoCode() }
            .thenReturn(validSerializedPromoCode)

        viewModel.startPromoCodeRetrieval()
        Assert.assertNotNull(viewModel.promoCodeRequestState.value)
        Assert.assertTrue(viewModel.promoCodeRequestState.value is PromoCodeRequestState.Success)
        Assert.assertEquals((viewModel.promoCodeRequestState.value as? PromoCodeRequestState.Success)?.promocode?.promoCode, "abc")
    }

    /**
     * Checks if the promo code retrieved from the shared prefs is valid
     * invalid mock data is passed.
     */
    @Test
    fun testPromoCodeRetrievedFromSharedPref_Invalid_Expired() {
        Mockito.mockStatic(PaywallPrefHelper::class.java).`when`<Any> { PaywallPrefHelper.getPromoCode() }
            .thenReturn(expiredSerializedPromoCode)
        Mockito.`when`(paywallService.verifyDeviceSubscription(true, true, false))
            .then{
                val promoCode  = PromoCodeJsonAdapter(Moshi.Builder().build()).fromJson(validSerializedPromoCode)!!
                dataSource.postValue(PromoCodeRequestState.Success(promoCode))
            }
        viewModel.startPromoCodeRetrieval()
        viewModel.promoCodeRequestState.observeForever{
            Assert.assertNotNull(it)
            Assert.assertTrue(it is PromoCodeRequestState.Success)
            Assert.assertEquals((it as? PromoCodeRequestState.Success)?.promocode?.promoCode, "abc")
        }
    }

    /**
     * Checks if the promo code retrieved from the verify device subs call is valid
     * valid mock data is passed.
     */
    @Test
    fun testPromoCodeRetrievedFromNetwork_Happy() {
        Mockito.mockStatic(PaywallPrefHelper::class.java).`when`<Any> { PaywallPrefHelper.getPromoCode() }
            .thenReturn(null)
        Mockito.`when`(paywallService.verifyDeviceSubscription(true, true, false))
            .then{
                val promoCode  = PromoCodeJsonAdapter(Moshi.Builder().build()).fromJson(validSerializedPromoCode)!!
                dataSource.postValue(PromoCodeRequestState.Success(promoCode))
            }
        viewModel.startPromoCodeRetrieval()
        viewModel.promoCodeRequestState.observeForever{
            Assert.assertNotNull(it)
            Assert.assertTrue(it is PromoCodeRequestState.Success)
            Assert.assertEquals((it as? PromoCodeRequestState.Success)?.promocode?.promoCode, "abc")
        }
    }

    /**
     * Checks if the promo code retrieved from the verify device subs call is valid
     * invalid/failure mock data is passed.
     */
    @Test
    fun testPromoCodeRetrievedFromNetwork_Error() {
        Mockito.mockStatic(PaywallPrefHelper::class.java).`when`<Any> { PaywallPrefHelper.getPromoCode() }
            .thenReturn(null)
        Mockito.`when`(paywallService.verifyDeviceSubscription(true, true, false))
            .then{
                dataSource.postValue(PromoCodeRequestState.Failure)
            }
        viewModel.startPromoCodeRetrieval()
        viewModel.promoCodeRequestState.observeForever {
            Assert.assertNotNull(it)
            Assert.assertTrue(it is PromoCodeRequestState.Failure)
        }
    }

    @Test
    fun testPromoCodeRetrievedFromNetwork_Failed_And_Retry_Success() {
        Mockito.mockStatic(PaywallPrefHelper::class.java).`when`<Any> { PaywallPrefHelper.getPromoCode() }
            .thenReturn(null)
        var triedAgain = false
        Mockito.`when`(paywallService.verifyDeviceSubscription(true, true, false))
            .then{
                dataSource.postValue(PromoCodeRequestState.Failure)
            }
        viewModel.startPromoCodeRetrieval()
        viewModel.promoCodeRequestState.observeForever {
            if(!triedAgain) {
                Assert.assertNotNull(it)
                Assert.assertTrue(it is PromoCodeRequestState.Failure)
            }else{
                Assert.assertNotNull(it)
                Assert.assertTrue(it is PromoCodeRequestState.Success)
                Assert.assertEquals((it as? PromoCodeRequestState.Success)?.promocode?.promoCode, "abc")
            }
        }
        Mockito.`when`(paywallService.verifyDeviceSubscription(true, true, false))
            .then{
                val promoCode  = PromoCodeJsonAdapter(Moshi.Builder().build()).fromJson(validSerializedPromoCode)!!
                dataSource.postValue(PromoCodeRequestState.Success(promoCode))
            }
        triedAgain = true
        viewModel.tryAgainClicked()

    }

}