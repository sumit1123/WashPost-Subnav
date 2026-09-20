package com.wapo.flagship.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.DisplayMetrics
import com.wapo.android.commons.data.repository.DeviceUtilRepoImpl
import com.wapo.android.commons.domain.BuildProviderRepo
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.util.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeviceUtilRepoTest {

    private lateinit var mockContext: Context
    private lateinit var buildProviderRepo: BuildProviderRepo
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var deviceUtilRepo: DeviceUtilRepo

    @Before
    fun setUp() {
        // Mock static Android classes
        mockkStatic(Logger::class)
        mockkStatic(PreferenceManager::class)
        mockkStatic(Settings.Secure::class)
        mockkStatic(UUID::class)

        // Mock dependencies
        mockContext = mockk(relaxed = true)
        buildProviderRepo = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        sharedPreferencesEditor = mockk(relaxed = true)

        // Define default behavior for mocks
        every { Logger.d(any(), any()) } just Runs
        every { PreferenceManager.getDefaultSharedPreferences(mockContext) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor

        // Instantiate the class under test
        deviceUtilRepo = DeviceUtilRepoImpl(mockContext, buildProviderRepo)
    }

    @Test
    fun `isTablet returns true for large screen width`() {
        // Given
        val resources = mockk<Resources>()
        val configuration = mockk<Configuration>()
        val displayMetrics = mockk<DisplayMetrics>()
        configuration.smallestScreenWidthDp = 700
        every { mockContext.resources } returns resources
        every { resources.configuration } returns configuration
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = deviceUtilRepo.isTablet()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isTablet returns false for small screen width`() {
        // Given
        val resources = mockk<Resources>()
        val configuration = mockk<Configuration>()
        val displayMetrics = mockk<DisplayMetrics>()
        configuration.smallestScreenWidthDp = 500
        displayMetrics.widthPixels = 1080
        displayMetrics.heightPixels = 1920
        displayMetrics.xdpi = 420f
        displayMetrics.ydpi = 420f
        every { mockContext.resources } returns resources
        every { resources.configuration } returns configuration
        every { resources.displayMetrics } returns displayMetrics

        // When
        val result = deviceUtilRepo.isTablet()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getUniqueDeviceId returns existing new ID when present`() {
        // Given
        every { mockSharedPreferences.getString("pref.new.logging_id", null) } returns "existing_new_id"

        // When
        val deviceId = deviceUtilRepo.getUniqueDeviceId()

        // Then
        assertEquals("existing_new_id", deviceId)
        verify(exactly = 0) { sharedPreferencesEditor.putString(any(), any()) }
    }

    @Test
    fun `getUniqueDeviceId migrates from old ID if new one is missing`() {
        // Given
        every { mockSharedPreferences.getString("pref.new.logging_id", null) } returns null
        every { mockSharedPreferences.getString("pref.logging_id", null) } returns "old_id"
        val slot = slot<String>()
        every { sharedPreferencesEditor.putString("pref.new.logging_id", capture(slot)) } returns sharedPreferencesEditor

        // When
        val deviceId = deviceUtilRepo.getUniqueDeviceId()

        // Then
        assertEquals("old_id", deviceId)
        assertEquals("old_id", slot.captured)
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `getUniqueDeviceId generates new ID if both old and new are missing`() {
        // Given
        val fakeUuid = "newly_generated_uuid"
        every { mockSharedPreferences.getString("pref.new.logging_id", null) } returns null
        every { mockSharedPreferences.getString("pref.logging_id", null) } returns null
        every { UUID.randomUUID().toString() } returns fakeUuid
        val slot = slot<String>()
        every { sharedPreferencesEditor.putString("pref.new.logging_id", capture(slot)) } returns sharedPreferencesEditor

        // When
        val deviceId = deviceUtilRepo.getUniqueDeviceId()

        // Then
        assertEquals(fakeUuid, deviceId)
        assertEquals(fakeUuid, slot.captured)
        verify { Logger.d(any(), "getUniqueDeviceId(), new id is generated! id=$fakeUuid") }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `getDeviceSerialId returns Secure ANDROID_ID when no serial is stored`() {
        // Given
        every { mockSharedPreferences.getString("pref.serial_id", null) } returns null
        every { buildProviderRepo.getUnknown() } returns "unknown"
        every { Settings.Secure.getString(mockContext.contentResolver, Settings.Secure.ANDROID_ID) } returns "secure_android_id"
        val slot = slot<String>()
        every { sharedPreferencesEditor.putString("pref.serial_id", capture(slot)) } returns sharedPreferencesEditor

        // When
        val serialId = deviceUtilRepo.getDeviceSerialId()

        // Then
        assertEquals("secure_android_id", serialId)
        assertEquals("secure_android_id", slot.captured)
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `getDeviceName combines manufacturer and model correctly`() {
        // Given
        every { buildProviderRepo.getManufacturer() } returns "Google"
        every { buildProviderRepo.getModel() } returns "Pixel 8"

        // When
        val deviceName = deviceUtilRepo.getDeviceName()

        // Then
        assertEquals("Google Pixel 8", deviceName)
    }
}
