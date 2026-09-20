package com.washingtonpost.android.paywall.bottomsheet.viewmodel

import com.squareup.moshi.Moshi
import com.washingtonpost.android.config.data.datasources.dto.config.paywallconf.RawBlocker
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker
import com.washingtonpost.android.config.domain.models.config.paywallconf.ComponentType
import com.washingtonpost.android.paywall.helper.componentTextToString
import com.washingtonpost.android.paywall.features.promocodes.viemodels.ViewModelTest
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems
import com.washingtonpost.android.paywall.util.PaywallConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class PaywallSheet2ViewModelTest : ViewModelTest() {

    private lateinit var paywallSheet2ViewModel: PaywallSheet2ViewModel

    private val moshi = Moshi.Builder().build()

    private lateinit var amazonBlocker: Blocker

    private val fallbackPriceM1R = "$3.99"

    private val iapSubItemsAmazon = IAPSubItems().apply {
        this.insertItem(
            IAPSubItem().apply {
                productId = "m1-r"
                title = "Basic Digital"
                basePrice = "$3.99"
                currencyCode = "USD"
                subscriptionPeriod = "P1M"
                offerPriceCycles = 0
            }
        )
    }


    @Before
    override fun setUp() {
        super.setUp()

        paywallSheet2ViewModel = PaywallSheet2ViewModel()

        moshi.adapter(RawBlocker::class.java).fromJson(amazonMainPaywallConfig)?.mapToDomain()?.let {
            amazonBlocker = it
        }
    }

    /**
     * -----------------------------------------
     * AMAZON TEST CASE SCENARIOS
     * -----------------------------------------
     */

    /**
     * Test No Sub
     */
    @Test
    fun testNoSubButtonText() {
        Mockito.`when`(connector.iapSubItems).thenReturn(iapSubItemsAmazon)
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isSubscriptionTerminated).thenReturn(false)
        Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(false)

        paywallSheet2ViewModel.update()
        val product = amazonBlocker.items!!.first()
        val text = paywallSheet2ViewModel.getProductButtonText(product)

        Assert.assertTrue(text == amazonM1RNoSubOfferText)
    }

    /**
     * Test Terminated Sub
     */
    @Test
    fun testTerminatedSubButtonText() {
        Mockito.`when`(connector.iapSubItems).thenReturn(iapSubItemsAmazon)
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isSubscriptionTerminated).thenReturn(true)
        Mockito.`when`(paywallService.isWpUserLoggedIn).thenReturn(false)

        paywallSheet2ViewModel.update()
        val product = amazonBlocker.items!!.first()
        val text = paywallSheet2ViewModel.getProductButtonText(product)

        Assert.assertTrue(text == amazonM1RTerminatedOfferText)
    }

    /**
     * Test gift paywall title for expired gift
     */
    @Test
    fun testGiftExpiredTitle() {
        paywallSheet2ViewModel.setPaywallType(PaywallConstants.WallType.GIFT_EXPIRED_PAYWALL)
        val component = amazonBlocker.components!!.find { it.type == ComponentType.TITLE }!!
        paywallSheet2ViewModel.getAppropriateText(component).observeForever {
            Assert.assertTrue(it == component.giftExpired)
        }
    }

    /**
     * Test save paywall title for no sub
     */
    @Test
    fun testSaveNoSubTitle() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isSubscriptionTerminated).thenReturn(false)
        paywallSheet2ViewModel.update()
        paywallSheet2ViewModel.setPaywallType(PaywallConstants.WallType.SAVE_REGWALL)
        val component = amazonBlocker.components!!.find { it.type == ComponentType.TITLE }!!
        paywallSheet2ViewModel.getAppropriateText(component).observeForever {
            Assert.assertTrue(it == component.text.componentTextToString())
        }
    }

    /**
     * Test save paywall title for terminated sub
     */
    @Test
    fun testSaveTerminatedSubTitle() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isSubscriptionTerminated).thenReturn(true)
        paywallSheet2ViewModel.update()
        paywallSheet2ViewModel.setPaywallType(PaywallConstants.WallType.SAVE_REGWALL)
        val component = amazonBlocker.components!!.find { it.type == ComponentType.TITLE }!!
        paywallSheet2ViewModel.getAppropriateText(component).observeForever {
            Assert.assertTrue(it == component.text.componentTextToString())
        }
    }

    /**
     * Test metered paywall title for no sub
     */
    @Test
    fun testDefaultNoSubTitle() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isSubscriptionTerminated).thenReturn(false)
        paywallSheet2ViewModel.update()
        paywallSheet2ViewModel.setPaywallType(PaywallConstants.WallType.METERED_PAYWALL)
        val component = amazonBlocker.components!!.find { it.type == ComponentType.TITLE }!!
        paywallSheet2ViewModel.getAppropriateText(component).observeForever {
            Assert.assertTrue(it == component.noneContent)
        }
    }

    /**
     * Test metered paywall title for terminated sub
     */
    @Test
    fun testDefaultTerminatedSubTitle() {
        Mockito.`when`(paywallService.isPremiumUser).thenReturn(false)
        Mockito.`when`(paywallService.isSubscriptionTerminated).thenReturn(true)
        paywallSheet2ViewModel.update()
        paywallSheet2ViewModel.setPaywallType(PaywallConstants.WallType.METERED_PAYWALL)
        val component = amazonBlocker.components!!.find { it.type == ComponentType.TITLE }!!
        paywallSheet2ViewModel.getAppropriateText(component).observeForever {
            Assert.assertTrue(it == component.noneContent)
        }
    }


    companion object{
        private const val AMAZON_DOLLER_ONE_OFFER_TEXT = "Get 6 months for $1"
        private const val AMAZON_M1R = "$3.99/month"

        private const val amazonDollerOneText = "<b>$AMAZON_DOLLER_ONE_OFFER_TEXT</b>\nthen $AMAZON_M1R"
        private const val amazonM1RNoSubOfferText = "<b>Subscribe</b>\nfor $AMAZON_M1R"
        private const val amazonM1RTerminatedOfferText = "<b>Resubscribe</b>\nfor $AMAZON_M1R"

        private const val amazonMainPaywallConfig = "{\n" +
                "                \"name\": \"amazon_main\",\n" +
                "                \"components\": [\n" +
                "                    {\n" +
                "                        \"type\": \"spacer\",\n" +
                "                        \"space\": 12\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"promo\",\n" +
                "                        \"text\": \"SPECIAL OFFER\",\n" +
                "                        \"none-content\": \"SPECIAL OFFER\",\n" +
                "                        \"ended-content\": \"SPECIAL OFFER\",\n" +
                "                        \"amazon-offer\": \"SPECIAL OFFER\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"spacer\",\n" +
                "                        \"space\": 8\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"title\",\n" +
                "                        \"none-content\": \"Subscribe to get the full experience.\",\n" +
                "                        \"ended-content\": \"Resubscribe to get the full experience.\",\n" +
                "                        \"gift-expired\": \"Sorry, this gift article link has expired.\",\n" +
                "                        \"gift-invalid\": \"Sorry, this gift article link is invalid.\",\n" +
                "                        \"amazon-offer\": \"Keep reading for only \$1\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"spacer\",\n" +
                "                        \"space\": 20\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"separator\",\n" +
                "                        \"text\": \"1-Click payment with Amazon\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"spacer\",\n" +
                "                        \"space\": 12\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"offer\",\n" +
                "                        \"components\": [\n" +
                "                            {\n" +
                "                                \"type\": \"button\"\n" +
                "                            }\n" +
                "                        ],\n" +
                "                        \"productSeparator\": [\n" +
                "                            {\n" +
                "                                \"type\": \"spacer\",\n" +
                "                                \"space\": 20\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"type\": \"separator\"\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"type\": \"spacer\",\n" +
                "                                \"space\": 20\n" +
                "                            }\n" +
                "                        ]\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"spacer\",\n" +
                "                        \"space\": 20\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"restore\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"spacer\",\n" +
                "                        \"space\": 12\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"terms\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"spacer\",\n" +
                "                        \"space\": 20\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"items\": [\n" +
                "                    {\n" +
                "                        \"id\": \"m1-r\",\n" +
                "                        \"introOfferText\": \"Get 6 months for \$1\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }"
    }
}