/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.data.repository

import android.content.SharedPreferences
import com.wapo.flagship.features.sections.data.SectionRibbonRepoImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SectionRibbonRepoTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var sectionRibbonRepo: SectionRibbonRepoImpl

    @Before
    fun setUp() {
        sharedPreferences = mockk(relaxed = true)
        sharedPreferencesEditor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.remove(any()) } returns sharedPreferencesEditor

        sectionRibbonRepo = SectionRibbonRepoImpl(sharedPreferences)
    }

    @Test
    fun `Given a last viewed section exists, when getLastViewed is called, then it returns the correct value`() {
        // Given
        val expectedValue = "test-section-id"
        every { sharedPreferences.getString("last_viewed_section_id", null) } returns expectedValue

        // When
        val result = sectionRibbonRepo.getLastViewed()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `Given no last viewed section exists, when getLastViewed is called, then it returns null`() {
        // Given
        every { sharedPreferences.getString("last_viewed_section_id", null) } returns null

        // When
        val result = sectionRibbonRepo.getLastViewed()

        // Then
        assertNull(result)
    }

    @Test
    fun `When setLastViewed is called, then the correct key and value are saved to prefs`() {
        // Given
        val keySlot = slot<String>()
        val valueSlot = slot<String>()
        every { sharedPreferencesEditor.putString(capture(keySlot), capture(valueSlot)) } returns sharedPreferencesEditor
        val valueToSave = "new-section-id"

        // When
        sectionRibbonRepo.setLastViewed(valueToSave)

        // Then
        verify { sharedPreferencesEditor.apply() }
        assertEquals("last_viewed_section_id", keySlot.captured)
        assertEquals(valueToSave, valueSlot.captured)
    }

    @Test
    fun `When removeLastViewed is called, then the correct key is removed from prefs`() {
        // Given
        val keySlot = slot<String>()
        every { sharedPreferencesEditor.remove(capture(keySlot)) } returns sharedPreferencesEditor

        // When
        sectionRibbonRepo.removeLastViewed()

        // Then
        verify { sharedPreferencesEditor.apply() }
        assertEquals("last_viewed_section_id", keySlot.captured)
    }

    @Test
    fun `When setOpenedOnKey is called, then the correct key and value are saved to prefs`() {
        // Given
        val keySlot = slot<String>()
        val valueSlot = slot<String>()
        every { sharedPreferencesEditor.putString(capture(keySlot), capture(valueSlot)) } returns sharedPreferencesEditor
        val valueToSave = "opened-on-section"

        // When
        sectionRibbonRepo.setOpenedOnKey(valueToSave)

        // Then
        verify { sharedPreferencesEditor.apply() }
        assertEquals("app_opened_on", keySlot.captured)
        assertEquals(valueToSave, valueSlot.captured)
    }

    @Test
    fun `When removeOpenedOnKey is called, then the correct key is removed from prefs`() {
        // Given
        val keySlot = slot<String>()
        every { sharedPreferencesEditor.remove(capture(keySlot)) } returns sharedPreferencesEditor

        // When
        sectionRibbonRepo.removeOpenedOnKey()

        // Then
        verify { sharedPreferencesEditor.apply() }
        assertEquals("app_opened_on", keySlot.captured)
    }
}
