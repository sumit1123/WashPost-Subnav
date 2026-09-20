package com.wapo.flagship.features.unification.viewmodels

import com.wapo.flagship.features.articles2.viewmodels.ViewModelTest
import com.wapo.flagship.features.unification.models.AccountSubState
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.billing.AbstractStoreBillingHelper
import com.washingtonpost.android.paywall.newdata.model.Subscription
import com.washingtonpost.android.paywall.newdata.model.WpUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.stubbing.Answer

@ExperimentalCoroutinesApi
class UnificationOnboardingViewModelTest : ViewModelTest() {
    @Mock
    lateinit var paywallService: PaywallService

    @Mock
    lateinit var wpUser: WpUser

    @Mock
    lateinit var abstractStoreBillingHelper: AbstractStoreBillingHelper

    @Mock
    lateinit var subscription: Subscription

    private lateinit var viewmodel: UnificationOnboardingViewModel

    @Before
    override fun setUp() {
        super.setUp()
        /*
            For mocking static classes.
         */
        Mockito.mockStatic(PaywallService::class.java)
        Mockito.`when`(paywallService.loggedInUser).thenReturn(wpUser)
        viewmodel = UnificationOnboardingViewModel(testCoroutineDispatcherProvider)
    }

    /**
     * This test case is for the failure in case of paywall service being not initialized.
     * Ideally we should fix that issue first but this functionality will let us know how many users are seeing
     * the paywall service to be null.
     *
     * In this case we just show the generic screen for now.
     */
    @Test
    fun testNoPaywallServiceFailure() {
        Mockito.`when`(PaywallService.getInstance()).thenAnswer { null }
        viewmodel.initializeSettingSubAndAccountState()
        viewmodel.accountSubState.observeForever {
            Assert.assertTrue(it is AccountSubState.GenericState)
        }
        viewmodel.remoteLogger.observeForever {
            Assert.assertTrue(it != null)
        }
    }

    /**
     * This case is used to test scenario when there's no user account / subscription
     */
    @Test
    fun testNoAccountNoSub() {
        Mockito.`when`(PaywallService.getInstance()).thenAnswer { paywallService }
        Mockito.`when`(paywallService.loggedInUser).thenReturn(null)
        Mockito.`when`(PaywallService.getBillingHelper()).thenAnswer { abstractStoreBillingHelper }
        Mockito.`when`(abstractStoreBillingHelper.migratedRainbowSubscription).thenReturn(null)
        viewmodel.initializeSettingSubAndAccountState()
        viewmodel.accountSubState.observeForever {
            Assert.assertTrue(it is AccountSubState.GenericState)
        }
    }

    /**
     * This case is used to test scenario when there's no user account but in-app subscription from play store
     */
    @Test
    fun testNoAccountWithSub() {
        Mockito.`when`(PaywallService.getInstance()).thenAnswer { paywallService }
        Mockito.`when`(paywallService.loggedInUser).thenReturn(null)
        Mockito.`when`(PaywallService.getBillingHelper()).thenAnswer { abstractStoreBillingHelper }
        Mockito.`when`(abstractStoreBillingHelper.migratedRainbowSubscription).thenReturn(
            subscription,
        )
        viewmodel.initializeSettingSubAndAccountState()
        viewmodel.accountSubState.observeForever {
            Assert.assertTrue(it is AccountSubState.NoAccountWithSub)
        }
    }

    /**
     * This case is used to test scenario when there's a user account and in-app subscription from play store
     */
    @Test
    fun testAccountWithSub() {
        Mockito.`when`(PaywallService.getInstance()).thenAnswer { paywallService }
        Mockito.`when`(paywallService.loggedInUser).thenReturn(wpUser)
        Mockito.`when`(wpUser.userId).thenReturn("abc@xyz.com")
        viewmodel.initializeSettingSubAndAccountState()
        viewmodel.accountSubState.observeForever {
            Assert.assertTrue(it is AccountSubState.AccountPresent)
            Assert.assertEquals((it as AccountSubState.AccountPresent).accountInfo, "abc@xyz.com")
        }
    }

    /**
     * This case is used to test scenario when there's a user account and in-app subscription from play store
     * In this case the user id (email) is not available e.g. FB login.
     */
    @Test
    fun testAccountWithSubNoUserId() {
        Mockito.`when`(PaywallService.getInstance()).thenAnswer { paywallService }
        Mockito.`when`(paywallService.loggedInUser).thenReturn(wpUser)
        Mockito.`when`(wpUser.userId).thenReturn(null)
        Mockito.`when`(wpUser.displayName).thenReturn("abc")
        viewmodel.initializeSettingSubAndAccountState()
        viewmodel.accountSubState.observeForever {
            Assert.assertTrue(it is AccountSubState.AccountPresent)
            Assert.assertEquals((it as AccountSubState.AccountPresent).accountInfo, "abc")
        }
    }

    /**
     * This case is used to test scenario when there's a user account but no in-app subscription from play store
     */
    @Test
    fun testAccountWithNoSub() {
        /**
         * We do not need a test for this right now as it's same as [testAccountWithSubNoUserId] and [testAccountWithSub]
         */
    }

    /**
     * This case is used to test scenario when user wants to sign in as someone else.
     * They first need to log out.
     * This tests the logic that we only start the sign in flow after current user is logged out.
     */
    @Test
    fun testLogOutUser() = runTest {
        var loggedOut = false
        Mockito.`when`(PaywallService.getInstance()).thenAnswer { paywallService }
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
}
