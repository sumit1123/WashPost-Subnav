/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.washingtonpost.android.paywall.billing.test

import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.config.domain.models.config.paywallconf.PaywallConf
import com.washingtonpost.android.config.domain.models.config.paywallconf.PaywallOffer
import com.washingtonpost.android.paywall.billing.playstore.IAPPlayStoreSubItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations


@ExperimentalCoroutinesApi
class IAPSubItemTest {

    @Mock
    lateinit var configManager: ConfigManager

    @Mock
    lateinit var config: Config

    private val defaultPaywallConf = PaywallConf(
        abTestName = null,
        blockers = null,
        metering = null,
        frontSubscriptionBanner = null,
        productToSkuMap = null,
        paywallOffers = null,
        allActiveOffers = null,
    )

    private val productId = "wp.classic.basic"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.mockStatic(ConfigManager::class.java).`when`<Any> { ConfigManager.getInstance() }
            .thenReturn(configManager)
        Mockito.`when`(configManager.config).thenReturn(config)
    }

    @Test
    fun testSingle() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0)
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE).mock)
        )

        Assert.assertEquals(INTRO_PRICE, id)
    }

    @Test
    fun testMultiDifferent() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(FREE_TRIAL, 1),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = FREE_TRIAL).mock,
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE).mock,
            )
        )

        Assert.assertEquals(FREE_TRIAL, id)
    }

    @Test
    fun testMultiSame() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(INTRO_PRICE2, 1),
                        PaywallOffer(FREE_TRIAL, 1),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = FREE_TRIAL).mock,
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE).mock,
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE2).mock,
            )
        )

        Assert.assertEquals(INTRO_PRICE2, id)
    }

    @Test
    fun testMultiDifferentNotHighest() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(FREE_TRIAL, 1),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE).mock,
            )
        )

        Assert.assertEquals(INTRO_PRICE, id)
    }

    @Test
    fun testMultiSameNotHighest() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(INTRO_PRICE2, 1),
                        PaywallOffer(FREE_TRIAL, 1),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE).mock,
            )
        )

        Assert.assertEquals(INTRO_PRICE, id)
    }

    @Test
    fun testEmptyAvailable() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf()
        )

        Assert.assertEquals(null, id)
    }

    @Test
    fun testEmptyEligible() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf()
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = FREE_TRIAL).mock,
            )
        )

        Assert.assertEquals(null, id)
    }

    @Test
    fun testNullAvailable() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = null
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE).mock,
            )
        )

        Assert.assertEquals(null, id)
    }

    @Test
    fun testNullEligible() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(FREE_TRIAL, 1),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(null),
        )

        Assert.assertEquals(null, id)
    }

    @Test
    fun testNoMatch() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = FREE_TRIAL).mock,
            )
        )

        Assert.assertEquals(null, id)
    }

    @Test
    fun testMultiSameNoMatch() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(INTRO_PRICE2, 1),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = FREE_TRIAL).mock,
            )
        )

        Assert.assertEquals(null, id)
    }

    @Test
    fun testMultiDifferentNoMatch() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(INTRO_PRICE2, 1),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = FREE_TRIAL).mock,
            )
        )

        Assert.assertEquals(null, id)
    }

    @Test
    fun testThreeDifferent() = runTest {
        Mockito.`when`(config.paywallConf).thenReturn(
            defaultPaywallConf.copy(
                paywallOffers = mapOf(
                    productId to listOf(
                        PaywallOffer(INTRO_PRICE, 0),
                        PaywallOffer(INTRO_PRICE2, 1),
                        PaywallOffer(FREE_TRIAL, 2),
                    )
                )
            )
        )

        val id = IAPPlayStoreSubItem.getOfferIdToDisplayOnPaywall(
            productId,
            listOf(
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE).mock,
                SubscriptionOfferDetailsWrapper(offerId = INTRO_PRICE2).mock,
                SubscriptionOfferDetailsWrapper(offerId = FREE_TRIAL).mock,
            )
        )

        Assert.assertEquals(FREE_TRIAL, id)
    }

    companion object {
        const val FREE_TRIAL = "freetrial"
        const val INTRO_PRICE = "introprice"
        const val INTRO_PRICE2 = "introprice2"
    }

    class SubscriptionOfferDetailsWrapper(
        offerId: String? = null,
        offerTags: List<String>? = emptyList(),
        offerToken: String = "",
        basePlanId: String = "",
    ) {
        val mock: SubscriptionOfferDetails = Mockito.mock(SubscriptionOfferDetails::class.java)

        init {
            Mockito.`when`(mock.offerId).thenReturn(offerId)
            Mockito.`when`(mock.offerTags).thenReturn(offerTags)
            Mockito.`when`(mock.offerToken).thenReturn(offerToken)
            Mockito.`when`(mock.basePlanId).thenReturn(basePlanId)
        }
    }
}
