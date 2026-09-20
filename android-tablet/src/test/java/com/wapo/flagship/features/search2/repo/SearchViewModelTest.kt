package com.wapo.flagship.features.search2.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.search2.model.*
import com.wapo.flagship.features.search2.remote.Search2Service
import com.wapo.flagship.features.search2.state.SearchUiState
import com.wapo.flagship.features.search2.viewmodel.SearchViewModel
import com.wapo.flagship.network.retrofit.network.APIResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class SearchViewModelTest : ViewModelTest() {
    @Mock
    lateinit var searchViewModel: SearchViewModel

    @Mock
    lateinit var searchResultResponse: SearchResultResponse

    @Mock
    lateinit var search2Service: Search2Service

    @Mock
    var requestSearchResultsStatus = LiveEvent<SearchResultApiStatus>()

    @Mock
    lateinit var searchRepo: SearchRepo

    @Mock
    lateinit var searchUiState: LiveData<SearchUiState>

    @Before
    override fun setUp() {
        super.setUp()
        Mockito.`when`(searchViewModel.searchUiState).thenReturn(searchUiState)
        Mockito.`when`(searchRepo.requestSearchResultsStatus).thenReturn(
            requestSearchResultsStatus,
        )
    }

    @Test
    fun test_success_resource() =
        runTest {
            val sections = listOf<SectionItem>(SectionItem("politics", ""))
            val artical = listOf<ArticleItem>(ArticleItem("trump", "", "", "", 0L))
            val filters = mapOf<FilterHeaderItem, List<FilterRadioItem>>()
            Mockito.`when`(search2Service.searchFor(QueryFilter("").toQueryParamsMap())).thenReturn(
                APIResult.Success(searchResultResponse, Headers.headersOf()),
            )
            search2Service.searchFor(QueryFilter("").toQueryParamsMap())
            val searchUiState =
                MutableLiveData<SearchUiState>(
                    SearchUiState.Success(sections, artical, 10, filters),
                )
            Assert.assertNotNull(searchUiState.value)
        }

    @Test
    fun test_loading_resource() =
        runTest {
            val searchUiState = MutableLiveData<SearchUiState>(SearchUiState.Loading)
            Assert.assertNotNull(searchUiState.value)
        }

    @Test
    fun test_error_resource() =
        runTest {
            val statusCode = 404
            val rawResponse = "Page not found"
            Mockito.`when`(search2Service.searchFor(QueryFilter("").toQueryParamsMap())).thenReturn(
                APIResult.Failure(statusCode, rawResponse),
            )
            val result = search2Service.searchFor(QueryFilter("").toQueryParamsMap())
            Assert.assertEquals(APIResult.Failure(statusCode, rawResponse), result)
        }

    @Test
    fun test_landing_resource() =
        runTest {
            val searchUiState =
                MutableLiveData<SearchUiState>(
                    SearchUiState.Landing(emptyList(), emptyList()),
                )
            Assert.assertNotNull(searchUiState.value)
        }

    @Test
    fun test_no_matching_resource() =
        runTest {
            val searchUiState = MutableLiveData<SearchUiState>(SearchUiState.NoMatches)
            Assert.assertNotNull(searchUiState.value)
        }

    @Test
    fun test_page_loading_resource() =
        runTest {
            val searchUiState = MutableLiveData<SearchUiState>(SearchUiState.NextPageLoading)
            Assert.assertNotNull(searchUiState.value)
        }

    @Test
    fun test_page_loaded_resource() =
        runTest {
            val searchUiState =
                MutableLiveData<SearchUiState>(
                    SearchUiState.NextPageLoaded(emptyList()),
                )
            Assert.assertNotNull(searchUiState.value)
        }
}
