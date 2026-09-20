/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.onboarding

import com.wapo.flagship.domain.repository.OnboardingRepo
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.onboarding2.models.OnboardingEvent
import com.wapo.flagship.features.onboarding2.models.OnboardingUiState
import com.wapo.flagship.features.onboarding2.viewmodel.onboarding.OnboardingViewModel
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.paywall.PaywallConnector
import com.washingtonpost.android.paywall.PaywallService
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest : BaseViewModelTest<OnboardingUiState, OnboardingEvent>() {

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var onboardingRepo: OnboardingRepo
    private lateinit var paywallService: PaywallService
    private lateinit var paywallConnector: PaywallConnector

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        // Mock dependencies
        dispatcherProvider = mockk {
            every { main } returns Dispatchers.Unconfined
            every { io } returns Dispatchers.Unconfined
        }
        onboardingRepo = mockk(relaxed = true)
        paywallService = mockk(relaxed = true)
        paywallConnector = mockk(relaxed = true)

        // Mock static dependencies
        mockkStatic(PaywallService::class)
        mockkStatic(Measurement::class)

        // Setup default mock behavior
        every { PaywallService.getInstance() } returns paywallService
        every { PaywallService.getConnector() } returns paywallConnector
        every { Measurement.trackOnboardingSeen(any()) } returns Unit

        viewModel = OnboardingViewModel(dispatcherProvider, onboardingRepo)
    }

    override fun collectUIStates(): StateFlow<OnboardingUiState> {
        return viewModel.uiState
    }

    override fun collectEvents(): SharedFlow<OnboardingEvent> {
        return viewModel.onboardingEvent
    }

    @Test
    fun `When logOutUser is called, then PaywallService logOutCurrentUser is invoked`() = runTest {
        // When
        viewModel.logOutUser()

        // Then
        coVerify { paywallService.logOutCurrentUser() }
    }

    @Test
    fun `Given onboarding not shown, when shouldShow is called, it returns true`() {
        // Given
        every { paywallService.isWpUserLoggedIn } returns false
        every { onboardingRepo.isAlreadyShown() } returns false

        // When
        val result = viewModel.shouldShow()

        // Then
        assertTrue(result)
    }

    @Test
    fun `When setShown is called, then repo setShown is called`() {
        // When
        viewModel.setShown()

        // Then
        verify { onboardingRepo.setShown() }
    }

    @Test
    fun `When loadPersonalizePages is called, then repo loadPersonalizePages is called`() {
        // When
        viewModel.loadPersonalizePages()

        // Then
        verify { onboardingRepo.loadPersonalizePages() }
    }

    @Test
    fun `When trackCurrentScreen is called and screen not seen, then measurement is tracked`() {
        // Given
        every { onboardingRepo.isCurrentScreenAlreadySeen() } returns false
        every { onboardingRepo.getCurrentPageId() } returns 123 // some random id

        // When
        viewModel.trackCurrentScreen()

        // Then
        verify { Measurement.trackOnboardingSeen(any()) }
        verify { onboardingRepo.addSeenPage(123) }
    }

    @Test
    fun `When nextPage is called, then repo nextPage is called`() {
        // When
        viewModel.nextPage()

        // Then
        verify { onboardingRepo.nextPage() }
    }
}
