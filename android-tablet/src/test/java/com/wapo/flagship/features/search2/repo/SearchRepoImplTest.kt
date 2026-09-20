package com.wapo.flagship.features.search2.repo

import com.wapo.flagship.features.search2.model.QueryFilter
import com.wapo.flagship.features.search2.model.SearchResultResponse
import com.wapo.flagship.features.search2.remote.Search2Service
import com.wapo.flagship.network.retrofit.network.APIResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import java.net.HttpURLConnection

@ExperimentalCoroutinesApi
class SearchRepoImplTest : ViewModelTest() {
    @Mock
    lateinit var search2Service: Search2Service

    @Mock
    lateinit var searchResultResponse: SearchResultResponse

    @Test
    fun test_SearchFor_Sucess() =
        runTest {
            val queryFilter = QueryFilter("trump")
            Mockito.`when`(search2Service.searchFor(queryFilter.toQueryParamsMap())).thenReturn(
                APIResult.Success(searchResultResponse, Headers.headersOf()),
            )
            val result = search2Service.searchFor(queryFilter.toQueryParamsMap())
            Assert.assertEquals(APIResult.Success(searchResultResponse, Headers.headersOf()), result)
        }

    @Test
    fun test_SearchFor_Failure() =
        runTest {
            val statusCode = 404
            val rawResponse = "Page not found"
            val queryFilter = QueryFilter("hhhhh")
            Mockito.`when`(search2Service.searchFor(queryFilter.toQueryParamsMap())).thenReturn(
                APIResult.Failure(statusCode, rawResponse),
            )
            val result = search2Service.searchFor(queryFilter.toQueryParamsMap())
            Assert.assertEquals(APIResult.Failure(statusCode, rawResponse), result)
        }

    @Test
    fun test_ApiResult_Failure_isConnectionProblem_returns_true_for_HTTP_CLIENT_TIMEOUT() =
        runTest {
            val statusCode = HttpURLConnection.HTTP_CLIENT_TIMEOUT
            val rawResponse = "Request timed out"
            val queryFilter = QueryFilter("hhhhh")
            Mockito.`when`(search2Service.searchFor(queryFilter.toQueryParamsMap())).thenReturn(
                APIResult.Failure(statusCode, rawResponse),
            )
            val result = search2Service.searchFor(queryFilter.toQueryParamsMap())
            Assert.assertEquals(APIResult.Failure(statusCode, rawResponse), result)
        }

    @Test
    fun test_ApiResult_Failure_isConnectionProblem_returns_true_for_HTTP_FORBIDDEN() =
        runTest {
            val statusCode = HttpURLConnection.HTTP_FORBIDDEN
            val rawResponse = "Access denied"
            val queryFilter = QueryFilter("hhhhh")
            Mockito.`when`(search2Service.searchFor(queryFilter.toQueryParamsMap())).thenReturn(
                APIResult.Failure(statusCode, rawResponse),
            )
            val result = search2Service.searchFor(queryFilter.toQueryParamsMap())
            Assert.assertEquals(APIResult.Failure(statusCode, rawResponse), result)
        }

    @Test
    fun test_ApiResult_Failure_isConnectionProblem_returns_false_for_other_status_codes() =
        runTest {
            val statusCode = 500
            val rawResponse = "Internal server error"
            val queryFilter = QueryFilter("hhhhh")
            Mockito.`when`(search2Service.searchFor(queryFilter.toQueryParamsMap())).thenReturn(
                APIResult.Failure(statusCode, rawResponse),
            )
            val result = search2Service.searchFor(queryFilter.toQueryParamsMap())
            Assert.assertEquals(APIResult.Failure(statusCode, rawResponse), result)
        }
}
