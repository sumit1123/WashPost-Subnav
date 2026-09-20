package com.washingtonpost.android.paywall.util

import android.util.Log
import com.wapo.android.commons.util.Logger
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.washingtonpost.android.paywall.PaywallConnector
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.CoroutinesTestRule
import com.washingtonpost.android.paywall.helper.PaywallCounterHelper
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
open class PaywallBaseTest {
    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    @Mock
    lateinit var paywallService: PaywallService
    @Mock
    lateinit var connector: PaywallConnector

    @Rule
    @JvmField
    var coroutinesTestRule = CoroutinesTestRule()

    @Before
    open fun setUp() {
        MockitoAnnotations.initMocks(this)
//        coroutinesTestRule.testDispatcher.pauseDispatcher()

        Mockito.mockStatic(PaywallService::class.java).let { ps ->
            ps.`when`<Any> { PaywallService.getInstance() }.thenReturn(paywallService)
            ps.`when`<Any> { PaywallService.getConnector() }.thenReturn(connector)
        }

        Mockito.mockStatic(Log::class.java).`when`<Any> { Logger.d(any(), any()) }.thenReturn(1)
    }

    @After
    open fun tearDown() {
        coroutinesTestRule.testDispatcher.cancel()
        Mockito.framework().clearInlineMocks()
    }

    protected fun mockRequestHeaders() {
        Mockito.mockStatic(PaywallPrefHelper::class.java).let { ps ->
            ps.`when`<Any> { PaywallPrefHelper.getPrefTetroStateCookie() }
                .thenReturn("H4sIAAAAAAAA/6uuBQBDv6ajAgAAAA==")
        }
        Mockito.mockStatic(PaywallCounterHelper::class.java).let { ps ->
            ps.`when`<Any> { PaywallCounterHelper.getArticleListNotSynced(any()) }
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