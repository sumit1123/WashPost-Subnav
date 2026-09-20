package com.washingtonpost.android.paywall.features.promocodes.viemodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.washingtonpost.android.paywall.PaywallConnector
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.CoroutinesTestRule
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
open class ViewModelTest {
    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    @Mock
    lateinit var paywallService: PaywallService
    @Mock
    lateinit var connector: PaywallConnector
    @Mock
    lateinit var paywallPrefHelper: PaywallPrefHelper

    @Rule
    @JvmField
    var coroutinesTestRule = CoroutinesTestRule()

    @Before
    open fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.mockStatic(PaywallService::class.java).let { ps ->
            ps.`when`<Any> { PaywallService.getInstance() }.thenReturn(paywallService)
            ps.`when`<Any> { PaywallService.getConnector() }.thenReturn(connector)
            ps.`when`<Any> { PaywallService.getPaywallPrefHelper() }.thenReturn(paywallPrefHelper)
        }
    }

    @After
    open fun tearDown() {
        coroutinesTestRule.testDispatcher.cancel()
        Mockito.framework().clearInlineMocks()
    }
}