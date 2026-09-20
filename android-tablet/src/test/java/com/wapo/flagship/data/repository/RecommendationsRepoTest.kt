package com.wapo.flagship.data.repository

import com.wapo.flagship.data.model.RecommendationsRepoResult
import com.wapo.flagship.domain.repository.RecommendationsRepo
import com.washingtonpost.foryou.data.ForYouResponse
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.network.APIResult
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl.Companion.SURFACE_HOME
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl.Companion.SURFACE_RECIRC_SOFTWALL
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecommendationsRepoTest {

    lateinit var recommendationsRepo: RecommendationsRepo

    val itemOne = mockk<RecommendationsItem>()
    val itemTwo = mockk<RecommendationsItem>()

    val recommendationsResponse = listOf(itemOne, itemTwo)

    fun createRecommendationsRepo(
        forYouFeedRepository: ForYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {

        }
    ): RecommendationsRepo {
        return RecommendationsRepoImpl(forYouFeedRepository)
    }


    @Test
    fun recommendationsRepo_fetchData_fetchFromCache_HOME_surface_Test() {
        runTest {
            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {
                    coEvery { getCache(SURFACE_HOME) } returns successResult
                }
            )

            val testData = RequestFetchDataTest().copy(
                surface = SURFACE_HOME
            )
            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.FeedData)
            assertEquals(recommendationsResponse, (result as RecommendationsRepoResult.FeedData).response?.recommendations)
        }
    }

    @Test
    fun recommendationsRepo_fetchData_fetchFromCache_RECIRC_surface_Test() {
        runTest {
            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {
                    coEvery { getCache(SURFACE_RECIRC_SOFTWALL) } returns successResult
                }
            )

            val testData = RequestFetchDataTest().copy(
                surface = SURFACE_RECIRC_SOFTWALL
            )
            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.MapData)
            assertEquals(itemOne, (result as RecommendationsRepoResult.MapData).item)
            assertEquals(false, result.getMore)
        }
    }

    @Test
    fun recommendationsRepo_fetchData_fetchFromCache_Error_Test() {
        runTest {
            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {
                    coEvery { getCache(any()) } returns APIResult.Failure(
                        statusCode = 0,
                        rawResponse = null
                    )
                }
            )

            val testData = RequestFetchDataTest()
            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.Error)
        }
    }

    @Test
    fun recommendationsRepo_fetchData_fetchFromRemote_HOME_getMore_surface_Test() {
        runTest {
            val testData = RequestFetchDataTest().copy(
                surface = SURFACE_HOME,
                fetchFromCache = false,
                getMore = true
            )

            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {
                    coEvery {
                        getMoreRecommendations(
                            surface = testData.surface,
                            limit = testData.limit,
                            currentUrl = testData.currentUrl
                        )
                    } returns successResult
                }
            )

            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.FeedData)
            assertEquals(recommendationsResponse, (result as RecommendationsRepoResult.FeedData).response?.recommendations)
        }
    }

    @Test
    fun recommendationsRepo_fetchData_fetchFromRemote_RECIRC_getMore_surface_Test() {
        runTest {
            val testData = RequestFetchDataTest().copy(
                surface = SURFACE_RECIRC_SOFTWALL,
                fetchFromCache = false,
                getMore = true
            )

            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {
                    coEvery {
                        getMoreRecommendations(
                            surface = testData.surface,
                            limit = testData.limit,
                            currentUrl = testData.currentUrl
                        )
                    } returns successResult
                }
            )

            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.MapData)
            assertEquals(itemTwo, (result as RecommendationsRepoResult.MapData).item)
            assertEquals(testData.getMore, result.getMore)
        }
    }

    @Test
    fun recommendationsRepo_fetchData_fetchFromRemote_HOME_getMore_ERROR_surface_Test() {
        runTest {
            val testData = RequestFetchDataTest().copy(
                surface = SURFACE_HOME,
                fetchFromCache = false,
                getMore = true
            )

            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {
                    coEvery {
                        getMoreRecommendations(
                            testData.surface,
                            testData.limit,
                            testData.currentUrl
                        )
                    } returns APIResult.Failure(
                        statusCode = 0,
                        rawResponse = null
                    )
                }
            )

            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.Error)
        }
    }

    @Test
    fun recommendationsRepo_fetchData_fetchFromRemote_HOME_NO_getMore_surface_Test() {
        runTest {
            val testData = RequestFetchDataTest().copy(
                surface = SURFACE_HOME,
                fetchFromCache = false,
                getMore = false
            )

            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {

                    coEvery {
                        getForYouRecommendations(
                            skipReadingFromCache = false,
                            surface = testData.surface,
                            limit = testData.limit,
                            excludeList = emptyList(),
                            currentUrl = testData.currentUrl
                        )
                    } returns successResult
                }
            )

            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.FeedData)
            assertEquals(recommendationsResponse, (result as RecommendationsRepoResult.FeedData).response?.recommendations)
        }
    }

    @Test
    fun recommendationsRepo_fetchData_fetchFromRemote_RECIRC_NO_getMore_surface_Test() {
        runTest {
            val testData = RequestFetchDataTest().copy(
                surface = SURFACE_RECIRC_SOFTWALL,
                fetchFromCache = false,
                getMore = false
            )

            recommendationsRepo = createRecommendationsRepo(
                forYouFeedRepository = mockk<ForYouFeedRepository>(relaxed = true) {

                    coEvery {
                        getForYouRecommendations(
                            skipReadingFromCache = true,
                            surface = testData.surface,
                            limit = testData.limit,
                            excludeList = emptyList(),
                            currentUrl = testData.currentUrl
                        )
                    } returns successResult
                }
            )

            val result = fetchDataFromRequestFetchDataTest(testData)

            assert(result is RecommendationsRepoResult.MapData)
            assertEquals(itemTwo, (result as RecommendationsRepoResult.MapData).item)
            assertEquals(testData.getMore, result.getMore)
        }
    }

    private suspend fun fetchDataFromRequestFetchDataTest(
        testData: RequestFetchDataTest
    ): RecommendationsRepoResult {
        return recommendationsRepo.fetchData(
            surface = testData.surface,
            fetchFromCache = testData.fetchFromCache,
            limit = testData.limit,
            currentUrl = testData.currentUrl,
            getMore = testData.getMore
        )
    }

    private data class RequestFetchDataTest(
        val surface: String = SURFACE_HOME,
        val fetchFromCache: Boolean = true,
        val limit: Int? = 0,
        val currentUrl: String? = "testUrl",
        val getMore: Boolean = false
    )

    private val successResult: APIResult<ForYouResponse> = APIResult.Success(
        data = ForYouResponse(
            requestId = REQUEST_ID,
            recipeId = RECIPE_ID,
            testId = TEST_ID,
            recommendations = recommendationsResponse
        )
    )

    companion object {
        const val REQUEST_ID = "1234"
        const val RECIPE_ID = "1234"
        const val TEST_ID = "1234"
    }
}
