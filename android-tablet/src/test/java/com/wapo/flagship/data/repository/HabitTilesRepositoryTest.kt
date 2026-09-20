package com.wapo.flagship.data.repository

import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.data.repository.TestData.Tile.fakeTestData
import com.wapo.flagship.util.CoroutinesTestRule
import com.washingtonpost.foryou.ConsumedListProvider
import com.washingtonpost.foryou.data.ApiDataListener
import com.washingtonpost.foryou.data.ConsumedArticles
import com.washingtonpost.foryou.data.HabitTilesRequestBody
import com.washingtonpost.foryou.domain.HabitTilesCache
import com.washingtonpost.foryou.domain.HabitTilesRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.remote.ForYouService
import com.washingtonpost.foryou.repo.ForYouMetaData
import com.washingtonpost.foryou.repo.ForYouMetaProvider
import com.washingtonpost.foryou.repo.HabitTilesRepositoryImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HabitTilesRepositoryTest {

    private lateinit var forYouService: ForYouService
    private lateinit var consumedListProvider: ConsumedListProvider
    private lateinit var habitTilesCache: HabitTilesCache
    private lateinit var forYouMetaProvider: ForYouMetaProvider
    private lateinit var apiDataListener: ApiDataListener
    private lateinit var deviceUtilRepo: DeviceUtilRepo
    private lateinit var remoteLogRepo: RemoteLogRepo
    private lateinit var habitTilesRepository: HabitTilesRepository

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private val testScope = TestScope(coroutinesTestRule.dispatcher)

    @Before
    fun setUp() {
        mockkStatic(Logger::class)
        every { Logger.d(any(), any()) } just Runs

        forYouService = mockk(relaxed = true)
        consumedListProvider = mockk(relaxed = true)
        habitTilesCache = mockk(relaxed = true)
        forYouMetaProvider = mockk(relaxed = true)
        apiDataListener = mockk(relaxed = true)
        deviceUtilRepo = mockk(relaxed = true)
        remoteLogRepo = mockk(relaxed = true)

        habitTilesRepository = HabitTilesRepositoryImpl(
            ioDispatcher = coroutinesTestRule.dispatcher,
            coroutineScope = testScope,
            forYouService = forYouService,
            consumedListProvider = consumedListProvider,
            cache = habitTilesCache,
            forYouMetaProvider = forYouMetaProvider,
            apiDataListener = apiDataListener,
            deviceUtilRepo = deviceUtilRepo,
            remoteLog = remoteLogRepo
        )
    }

    @Test
    fun `getHabitTilesFeed returns cached data when cache is valid`() = runTest {
        every { habitTilesCache.isCacheValid() } returns true
        every { habitTilesCache.getTilesList() } returns fakeTestData

        val result = habitTilesRepository.getHabitTilesFeed(false, FAKE_SURFACE)

        assertTrue(result is APIResult.Success)
        assertEquals(fakeTestData, (result as APIResult.Success).data)
    }

    @Test
    fun `getHabitTilesFeed requests without personal data when no privacy consent is given`() = runTest {
        every { habitTilesCache.isCacheValid() } returns false
        every { forYouMetaProvider.getForYouMeta() } returns fakeForYouMetaData.copy(privacyConsentGiven = false)
        coEvery { consumedListProvider.getReadList() } returns testConsumedArticlesTwo

        habitTilesRepository.getHabitTilesFeed(false, FAKE_SURFACE)

        val slot = slot<HabitTilesRequestBody>()
        coVerify { forYouService.getHabitTiles(any(), fakeForYouMetaData.clientId, capture(slot)) }

        val capturedRequest = slot.captured
        assertTrue(capturedRequest.readList?.isEmpty() == true)
        assertNull(capturedRequest.userTimeZone)
        assertEquals(fakeForYouMetaData.sessionId, capturedRequest.jucId)
        assertEquals(fakeForYouMetaData.loginId, capturedRequest.wapoLoginId)
    }

    @Test
    fun `getHabitTilesFeed requests with personal data when privacy consent is given`() = runTest {
        every { habitTilesCache.isCacheValid() } returns false
        every { forYouMetaProvider.getForYouMeta() } returns fakeForYouMetaData.copy(privacyConsentGiven = true)
        coEvery { consumedListProvider.getReadList() } returns testConsumedArticlesTwo

        habitTilesRepository.getHabitTilesFeed(false, FAKE_SURFACE)

        val slot = slot<HabitTilesRequestBody>()
        coVerify { forYouService.getHabitTiles(any(), fakeForYouMetaData.clientId, capture(slot)) }

        val capturedRequest = slot.captured
        assertEquals(testConsumedArticlesTwo, capturedRequest.readList)
        assertNotNull(capturedRequest.userTimeZone)
        assertEquals(fakeForYouMetaData.sessionId, capturedRequest.jucId)
        assertEquals(fakeForYouMetaData.loginId, capturedRequest.wapoLoginId)
    }

    @Test
    fun `getHabitTilesFeed logs success and saves to cache on successful fetch`() = runTest {
        every { habitTilesCache.isCacheValid() } returns false
        every { forYouMetaProvider.getForYouMeta() } returns fakeForYouMetaData.copy(privacyConsentGiven = true)
        coEvery { consumedListProvider.getReadList() } returns testConsumedArticlesTwo
        coEvery { forYouService.getHabitTiles(any(), any(), any()) } returns APIResult.Success(fakeTestData)

        val result = habitTilesRepository.getHabitTilesFeed(false, FAKE_SURFACE)

        assertTrue(result is APIResult.Success)
        verify { habitTilesCache.saveTilesList(fakeTestData) }
        verify { apiDataListener.setABTestGroup(fakeTestData.testGroup) }

        val slot = slot<EventLog>()
        verify { remoteLogRepo.d(capture(slot)) }

        val capturedLog = slot.captured
        val tiles = fakeTestData.tiles?.joinToString("|") { "${it?.tileCategory}:${it?.tileLabel}" }.orEmpty()
        assertTrue(capturedLog.dataString.contains("test_group=\"${fakeTestData.testGroup}\""))
        assertTrue(capturedLog.dataString.contains("data_size=\"${fakeTestData.tiles?.size}\""))
        assertTrue(capturedLog.dataString.contains("tiles=\"$tiles\""))
        assertTrue(capturedLog.dataString.contains("message=\"HabitTiles List\""))
    }

    @Test
    fun `getHabitTilesFeed returns cached data on fetch failure`() = runTest {
        every { habitTilesCache.isCacheValid() } returns false
        every { habitTilesCache.getTilesList() } returns fakeTestData
        every { forYouMetaProvider.getForYouMeta() } returns fakeForYouMetaData.copy(privacyConsentGiven = true)
        coEvery { forYouService.getHabitTiles(any(), any(), any()) } returns APIResult.Failure(0, null)

        val result = habitTilesRepository.getHabitTilesFeed(false, FAKE_SURFACE)

        assertTrue(result is APIResult.Success)
        assertEquals(fakeTestData, (result as APIResult.Success).data)

        val slot = slot<EventLog>()
        verify { remoteLogRepo.e(capture(slot)) }

        val capturedLog = slot.captured
        assertTrue(capturedLog.dataString.contains("cache_size=\"${fakeTestData.tiles?.size}\""))
        assertTrue(capturedLog.dataString.contains("message=\"HabitTiles Failure\""))
    }

    @Test
    fun `getHabitTilesFeed returns failure when fetch fails and cache is empty`() = runTest {
        val failure = APIResult.Failure(0, null)
        every { habitTilesCache.isCacheValid() } returns false
        every { habitTilesCache.getTilesList() } returns null
        every { forYouMetaProvider.getForYouMeta() } returns fakeForYouMetaData.copy(privacyConsentGiven = true)
        coEvery { forYouService.getHabitTiles(any(), any(), any()) } returns failure

        val result = habitTilesRepository.getHabitTilesFeed(false, FAKE_SURFACE)

        assertEquals(failure, result)

        val slot = slot<EventLog>()
        verify { remoteLogRepo.e(capture(slot)) }

        val capturedLog = slot.captured
        assertTrue(capturedLog.dataString.contains("cache_size=\"null\""))
        assertTrue(capturedLog.dataString.contains("message=\"HabitTiles Failure\""))
    }

    @Test
    fun `getHabitTilesFeed returns network error when fetch fails and cache is empty`() = runTest {
        val networkError = APIResult.NetworkError(Throwable())
        every { habitTilesCache.isCacheValid() } returns false
        every { habitTilesCache.getTilesList() } returns null
        every { forYouMetaProvider.getForYouMeta() } returns fakeForYouMetaData.copy(privacyConsentGiven = true)
        coEvery { forYouService.getHabitTiles(any(), any(), any()) } returns networkError

        val result = habitTilesRepository.getHabitTilesFeed(false, FAKE_SURFACE)

        assertEquals(networkError, result)

        val slot = slot<EventLog>()
        verify { remoteLogRepo.e(capture(slot)) }

        val capturedLog = slot.captured
        assertTrue(capturedLog.dataString.contains("cache_size=\"null\""))
        assertTrue(capturedLog.dataString.contains("message=\"HabitTiles Network Error\""))
    }

    companion object {
        private val consumedArticlesThree = ConsumedArticles(articleUrl = "three", timestamp = 1L)
        private val consumedArticlesFour = ConsumedArticles(articleUrl = "four", timestamp = 1L)

        val testConsumedArticlesTwo: List<ConsumedArticles> = listOf(consumedArticlesThree, consumedArticlesFour)

        const val FAKE_SURFACE = "fake_surface"

        val fakeForYouMetaData = ForYouMetaData(
            loginId = "loginId",
            sessionId = "sessionId",
            clientId = "clientId",
            privacyConsentGiven = false
        )
    }
}
