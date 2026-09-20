/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingRepoTest {

    private lateinit var context: Context
    private lateinit var prefUtilsRepo: PrefUtilsRepo
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var onboardingRepo: OnboardingRepoImpl

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        prefUtilsRepo = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        sharedPreferencesEditor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putBoolean(any(), any()) } returns sharedPreferencesEditor

        onboardingRepo = OnboardingRepoImpl(context, prefUtilsRepo)
    }

    @Test
    fun `Given all pages should be shown, when loadPersonalizePages, then step size is 3`() {
        // Given
        every { prefUtilsRepo.shouldShowAlertsOnboarding() } returns true
        every { prefUtilsRepo.shouldShowContentPacksOnboarding() } returns true
        every { prefUtilsRepo.shouldShowAudioOnboarding() } returns true

        // When
        onboardingRepo.loadPersonalizePages()

        // Then
        assertEquals(3, onboardingRepo.getStepSize())
    }

    @Test
    fun `Given only one page should be shown, when loadPersonalizePages, then shouldShowSteps is false`() {
        // Given
        every { prefUtilsRepo.shouldShowAlertsOnboarding() } returns true
        every { prefUtilsRepo.shouldShowContentPacksOnboarding() } returns false
        every { prefUtilsRepo.shouldShowAudioOnboarding() } returns false

        // When
        onboardingRepo.loadPersonalizePages()

        // Then
        assertFalse(onboardingRepo.shouldShowSteps())
    }

    @Test
    fun `When nextPage and previousPage are called, then page index is updated correctly`() {
        // Given
        every { prefUtilsRepo.shouldShowAlertsOnboarding() } returns true
        every { prefUtilsRepo.shouldShowContentPacksOnboarding() } returns true
        onboardingRepo.loadPersonalizePages()
        assertEquals(0, onboardingRepo.getCurrentStep())
        assertNull(onboardingRepo.getCurrentPageId()) // Should be null before first page

        // When
        onboardingRepo.nextPage()

        // Then
        assertEquals(1, onboardingRepo.getCurrentStep())

        // When
        onboardingRepo.nextPage()
        assertTrue(onboardingRepo.shouldShowBack())

        // When
        onboardingRepo.previousPage()

        // Then
        assertEquals(1, onboardingRepo.getCurrentStep())
        assertFalse(onboardingRepo.shouldShowBack())
    }

    @Test
    fun `Given onboarding not shown, when isAlreadyShown is called, it returns false`() {
        // Given
        every { sharedPreferences.getBoolean("first_install_onboarding", false) } returns false

        // When
        val result = onboardingRepo.isAlreadyShown()

        // Then
        assertFalse(result)
    }

    @Test
    fun `When setShown is called, then shared preferences are updated`() {
        // Given
        val keySlot = slot<String>()
        val valueSlot = slot<Boolean>()
        every { sharedPreferencesEditor.putBoolean(capture(keySlot), capture(valueSlot)) } returns sharedPreferencesEditor

        // When
        onboardingRepo.setShown()

        // Then
        verify { sharedPreferencesEditor.apply() }
        assertEquals("first_install_onboarding", keySlot.captured)
        assertTrue(valueSlot.captured)
    }

    @Test
    fun `When continue active flags are set, then they are retrieved correctly`() {
        // When
        onboardingRepo.setAlertsContinueActive(true)
        onboardingRepo.setContentPacksContinueActive(false)

        // Then
        assertTrue(onboardingRepo.getAlertsContinueActive())
        assertFalse(onboardingRepo.getContentPacksContinueActive())
    }
}
