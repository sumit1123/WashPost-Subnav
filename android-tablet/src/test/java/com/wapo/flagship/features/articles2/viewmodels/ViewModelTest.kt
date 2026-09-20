package com.wapo.flagship.features.articles2.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wapo.flagship.helper.CoroutinesTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
open class ViewModelTest {
    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    var coroutinesTestRule = CoroutinesTestRule()

    val testCoroutineDispatcherProvider =
        TestCoroutineDispatcherProvider(
            coroutinesTestRule.testDispatcher,
        )

    @Before
    open fun setUp() {
        MockitoAnnotations.initMocks(this)
//        coroutinesTestRule.testDispatcher.pauseDispatcher()
    }

    @After
    open fun tearDown() {
        coroutinesTestRule.testDispatcher.cancel()
        Mockito.framework().clearInlineMocks()
    }
}
