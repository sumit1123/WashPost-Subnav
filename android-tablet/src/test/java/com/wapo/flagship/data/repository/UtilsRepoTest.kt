package com.wapo.flagship.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.wapo.android.commons.data.repository.UtilsRepoImpl
import com.wapo.android.commons.domain.BuildConfigProvider
import com.wapo.android.commons.domain.BuildProviderRepo
import com.wapo.android.commons.domain.UtilsRepo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class UtilsRepoTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockPackageInfo: PackageInfo
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetworkInfo: NetworkInfo
    private lateinit var buildProviderRepo: BuildProviderRepo
    private lateinit var buildConfigProvider: BuildConfigProvider

    private lateinit var utilsRepo: UtilsRepo

    @Before
    fun setUp() {
        // Mock Android framework classes
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        mockPackageInfo = PackageInfo()
        mockConnectivityManager = mockk(relaxed = true)
        mockNetworkInfo = mockk(relaxed = true)
        buildProviderRepo = mockk(relaxed = true)
        buildConfigProvider = mockk(relaxed = true)

        // Define mock behavior
        every { mockContext.packageManager } returns mockPackageManager
        every { mockContext.packageName } returns "com.wapo.android"
        every { mockPackageManager.getPackageInfo("com.wapo.android", 0) } returns mockPackageInfo
        every { mockContext.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        every { mockConnectivityManager.activeNetworkInfo } returns mockNetworkInfo

        // Instantiate the class under test
        utilsRepo = UtilsRepoImpl(mockContext, buildProviderRepo, buildConfigProvider)
    }

    @Test
    fun `isAmazonBuild returns true when manufacturer is Amazon`() {
        every { buildProviderRepo.getManufacturer() } returns "Amazon"
        assertTrue(utilsRepo.isAmazonBuild())
    }

    @Test
    fun `isAmazonBuild returns false when manufacturer is not Amazon`() {
        every { buildProviderRepo.getManufacturer() } returns "Google"
        assertFalse(utilsRepo.isAmazonBuild())
    }

    @Test
    fun `inputStreamToString converts stream to string correctly`() {
        val testString = "Hello, World!"
        val inputStream = ByteArrayInputStream(testString.toByteArray())
        val result = utilsRepo.inputStreamToString(inputStream)
        assertEquals(testString, result)
    }

    @Test
    fun `getAppVersionCode returns correct version code`() {
        val expectedVersionCode = 123
        val mockPackageInfo = PackageInfo().apply {
            versionCode = expectedVersionCode
        }
        every { mockPackageManager.getPackageInfo("com.wapo.android", 0) } returns mockPackageInfo
        val versionCode = utilsRepo.getAppVersionCode()
        assertEquals(expectedVersionCode, versionCode)
    }

    @Test
    fun `getAppVersionCode returns -1 on NameNotFoundException`() {
        every { mockPackageManager.getPackageInfo("com.wapo.android", 0) } throws PackageManager.NameNotFoundException()
        val versionCode = utilsRepo.getAppVersionCode()
        assertEquals(-1, versionCode)
    }

    @Test
    fun `getAppVersionName returns correct version name`() {
        val expectedVersionName = "1.2.3"
        val mockPackageInfo = PackageInfo().apply {
            versionName = expectedVersionName
        }
        every { mockPackageManager.getPackageInfo("com.wapo.android", 0) } returns mockPackageInfo
        val versionName = utilsRepo.getAppVersionName()
        assertEquals(expectedVersionName, versionName)
    }

    @Test
    fun `ellipsize returns original string if within max length`() {
        val text = "Short text"
        val ellipsized = utilsRepo.ellipsize(text, 20)
        assertEquals(text, ellipsized)
    }

    @Test
    fun `ellipsize shortens string and adds ellipsis`() {
        val text = "This is a very long string that needs to be ellipsized"
        val ellipsized = utilsRepo.ellipsize(text, 20)
        assertEquals("This is a very long...", ellipsized)
    }

    @Test
    fun `getSharedUrl returns original url if it is a full url`() {
        val url = "http://www.washingtonpost.com/some-article"
        val result = utilsRepo.getSharedUrl(url)
        assertEquals(url, result)
    }

    @Test
    fun `getSharedUrl prepends base url for partial url`() {
        val partialUrl = "/some-article/path"
        val expectedUrl = "http://www.washingtonpost.com/some-article/path"
        val result = utilsRepo.getSharedUrl(partialUrl)
        assertEquals(expectedUrl, result)
    }

    @Test
    fun `isAmazonDevice returns true for Kindle Fire models`() {
        every { buildProviderRepo.getManufacturer() } returns "Amazon"
        every { buildProviderRepo.getModel() } returns "Kindle Fire"
        assertTrue(utilsRepo.isAmazonDevice)
    }

    @Test
    fun `isAmazonDevice returns false for non-Amazon devices`() {
        every { buildProviderRepo.getManufacturer() } returns "Google"
        every { buildProviderRepo.getModel() } returns "Pixel 8"
        assertFalse(utilsRepo.isAmazonDevice)
    }

    @Test
    fun `isConnectedOrConnecting returns true when network is connected`() {
        every { mockNetworkInfo.isConnectedOrConnecting } returns true
        assertTrue(utilsRepo.isConnectedOrConnecting())
    }

    @Test
    fun `isConnectedOrConnecting returns false when network is not connected`() {
        every { mockNetworkInfo.isConnectedOrConnecting } returns false
        assertFalse(utilsRepo.isConnectedOrConnecting())
    }

    @Test
    fun `isConnectedOrConnecting returns false when network info is null`() {
        every { mockConnectivityManager.activeNetworkInfo } returns null
        assertFalse(utilsRepo.isConnectedOrConnecting())
    }

    @Test
    fun `removeCurrencySignFromPrice removes currency symbols`() {
        assertEquals("19.99", utilsRepo.removeCurrencySignFromPrice("$19.99"))
        assertEquals("0.99", utilsRepo.removeCurrencySignFromPrice("€0.99"))
        assertEquals("100", utilsRepo.removeCurrencySignFromPrice("¥100"))
    }

    @Test
    fun `coalesce returns first non-null item`() {
        val item1: String? = null
        val item2 = "Hello"
        val item3 = "World"
        assertEquals("Hello", utilsRepo.coalesce(item1, item2, item3))
    }

    @Test
    fun `coalesce returns null if all items are null`() {
        val item1: String? = null
        val item2: String? = null
        assertNull(utilsRepo.coalesce(item1, item2))
    }

    @Test
    fun `isFreshInstall returns true when install and update times are equal`() {
        val mockPackageInfo = PackageInfo().apply {
            firstInstallTime = 1000L
            lastUpdateTime = 1000L
        }
        every { mockPackageManager.getPackageInfo("com.wapo.android", 0) } returns mockPackageInfo
        assertTrue(utilsRepo.isFreshInstall())
    }

    @Test
    fun `isFreshInstall returns false when update time is after install time`() {
        val mockPackageInfo = PackageInfo().apply {
            firstInstallTime = 1000L
            lastUpdateTime = 2000L
        }
        every { mockPackageManager.getPackageInfo("com.wapo.android", 0) } returns mockPackageInfo
        assertFalse(utilsRepo.isFreshInstall())
    }
}