/* Copyright (c) 2023 The Washington Post. All rights reserved. */

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.gson.Gson
import com.wapo.android.commons.util.AppContextUtils
import com.washingtonpost.android.save.network.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.collections.HashMap

@RunWith(AndroidJUnit4::class)
class SavedStoriesApiTest {

    private lateinit var preferenceNetwork: SavedRetrofit.PreferenceNetwork

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        AppContextUtils.init(context, "appName")
        preferenceNetwork = SavedRetrofit.getInstance().getPreferenceNetwork(BASE_URL)
    }

    @Test
    fun listRequest() {
        val uris = mutableListOf<UrisRequestValue>()
        val request = preferenceNetwork.getSavedArticlesList(getHeaders(), SavedStoriesRequest(uris)).request()
        println(request)
        assertEquals(POST, request.method)
        val lastPathSegment = request.url.pathSegments.last()
        println(lastPathSegment)
        assertEquals(LIST, lastPathSegment)
    }

    @Test
    fun listResponse() {
        val response = Gson().fromJson(MOCK_LIST_RESPONSE, SavedStoriesResponse::class.java)
        assertEquals(6, response.saved?.size)
        assert(response.status != null)
        assert(response.state != null)
        assert(response.uris == null)
        assert(response.saved != null)
        response.saved?.forEach {
            assert(it.location != null)
            assert(it.userCreated != null)
            assert(it.userUpdated != null)
            assert(it.arcId != null)
            assert(it.contentType != null)
            assert(it.section != null)
        }
    }

    @Test
    fun saveRequest() {
        val uris = mutableListOf<UrisRequestValue>()
        uris.add(UrisRequestValue("url0", System.currentTimeMillis()))
        uris.add(UrisRequestValue("url1", System.currentTimeMillis()))
        uris.add(UrisRequestValue("url2", System.currentTimeMillis()))
        val request = preferenceNetwork.saveArticles(getHeaders(), SavedStoriesRequest(uris)).request()
        println(request)
        assertEquals(POST, request.method)
        val lastPathSegment = request.url.pathSegments.last()
        println(lastPathSegment)
        assertEquals(SAVE, lastPathSegment)
    }

    @Test
    fun saveResponse() {
        val response = Gson().fromJson(MOCK_SAVE_RESPONSE, SavedStoriesResponse::class.java)
        assertEquals(3, response.uris?.size)
        assert(response.status != null)
        assert(response.state != null)
        assert(response.uris != null)
        response.uris?.forEach {
            assert(it.location != null)
            assert(it.userCreated != null)
            assert(it.userUpdated != null)
            assert(it.archived != null)
            assert(it.status != null)
        }
        assert(response.saved == null)
    }

    @Test
    fun deleteRequest() {
        val uris = mutableListOf<UrisRequestValue>()
        uris.add(UrisRequestValue("url0", System.currentTimeMillis()))
        uris.add(UrisRequestValue("url1", System.currentTimeMillis()))
        uris.add(UrisRequestValue("url2", System.currentTimeMillis()))
        val request = preferenceNetwork.deleteArticles(getHeaders(), SavedStoriesRequest(uris)).request()
        println(request)
        assertEquals(POST, request.method)
        val lastPathSegment = request.url.pathSegments.last()
        println(lastPathSegment)
        assertEquals(DELETE, lastPathSegment)
    }

    @Test
    fun deleteResponse() {
        val response = Gson().fromJson(MOCK_DELETE_RESPONSE, SavedStoriesResponse::class.java)
        assertEquals(3, response.uris?.size)
        assert(response.status != null)
        assert(response.state != null)
        assert(response.uris != null)
        response.uris?.forEach {
            assert(it.location != null)
            assert(it.userCreated != null)
            assert(it.userUpdated != null)
            assert(it.archived != null)
            assert(it.status != null)
        }
        assert(response.saved == null)
    }

/*
    //  The following tests were commented out because they make real network requests requiring
    //  a bearer token. While they may pass initially, they will eventually fail with 401 errors
    //  once the token expires.

    @Test
    fun saveArticlesSaved() {
        clearSavedStories()

        val transactionList = mutableListOf<UrisRequestValue>()
        transactionList.add(UrisRequestValue("url0", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url1", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url2", System.currentTimeMillis()))
        val request = preferenceNetwork.saveArticles(getHeaders(), SavedStoriesRequest(transactionList)).request()
        println(request)

//        response.body()?.uris?.forEach { uri ->
//            assertEquals("SAVED", uri.status)
//        } // TODO fix bearer ?: fail("Invalid response: ${response.body()}")
    }

    @Test
    fun deleteArticlesDeleted() {
        clearSavedStories()

        var transactionList = mutableListOf<UrisRequestValue>()
        transactionList.add(UrisRequestValue("url0", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url1", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url2", System.currentTimeMillis()))
        preferenceNetwork.saveArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()

        transactionList = mutableListOf()
        transactionList.add(UrisRequestValue("url0", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url1", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url2", System.currentTimeMillis()))
        val response = preferenceNetwork.deleteArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()
        response.body()?.uris?.forEach { uri ->
            assertEquals("DELETED", uri.status)
        } // TODO fix bearer ?: fail("Invalid response: ${response.body()}")
    }

    @Test
    fun deleteArticlesRejected() {
        clearSavedStories()

        var transactionList = mutableListOf<UrisRequestValue>()
        transactionList.add(UrisRequestValue("url0", System.currentTimeMillis()))
        preferenceNetwork.saveArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()

        transactionList = mutableListOf()
        transactionList.add(UrisRequestValue("url0", System.currentTimeMillis() - TWENTY_FOUR_HOURS_IN_MILLISECONDS))
        val response = preferenceNetwork.deleteArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()
        response.body()?.uris?.forEach { uri ->
            assertEquals("REJECTED", uri.status)
        } // TODO fix bearer ?: fail("Invalid response: ${response.body()}")
    }

    @Test
    fun deleteArticlesNotFound() {
        clearSavedStories()

        val transactionList = mutableListOf<UrisRequestValue>()
        transactionList.add(UrisRequestValue("url3", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url4", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url5", System.currentTimeMillis()))
        val response = preferenceNetwork.deleteArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()
        response.body()?.uris?.forEach { uri ->
            assertEquals("NOT_FOUND", uri.status)
        } // TODO fix bearer ?: fail("Invalid response: ${response.body()}")
    }

    @Test
    fun fetchArticles() {
        clearSavedStories()

        var transactionList = mutableListOf<UrisRequestValue>()
        transactionList.add(UrisRequestValue("url0", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url1", System.currentTimeMillis()))
        transactionList.add(UrisRequestValue("url2", System.currentTimeMillis()))
        preferenceNetwork.saveArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()

        transactionList = mutableListOf()
        transactionList.add(UrisRequestValue("url0", System.currentTimeMillis()))
        preferenceNetwork.deleteArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()

        val response = preferenceNetwork.getSavedArticlesList(getHeaders(), SavedStoriesRequest(listOf())).execute()
        val actualSavedStories = response.body()?.saved?.map { saved -> saved.location }
        val expectedSavedStories = listOf("url1", "url2")
//        assertTrue(
//            actualSavedStories != null &&
//            actualSavedStories.size == expectedSavedStories.size &&
//            actualSavedStories.containsAll(expectedSavedStories)
//        ) // TODO fix bearer
        assertTrue(true)
    }

    private fun clearSavedStories() {
        val response = preferenceNetwork.getSavedArticlesList(getHeaders(), SavedStoriesRequest(mutableListOf<UrisRequestValue>())).execute()
        val savedStories = response.body()?.saved?.map { saved -> saved.location }
        val transactionList = mutableListOf<UrisRequestValue>()
        savedStories?.forEach { url ->
            transactionList.add(UrisRequestValue(url!!, System.currentTimeMillis() + TWENTY_FOUR_HOURS_IN_MILLISECONDS))
        }
        preferenceNetwork.deleteArticles(getHeaders(), SavedStoriesRequest(transactionList)).execute()
    }*/

    private fun getHeaders(isArchive: Boolean = false) : HashMap<String, String> {
        val headers = hashMapOf<String,String>()

        headers["authorization"] = "Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiIyMGNkMGU0Yy1jMTdjLTQ4Y2UtYWU1MC1iYWU0NjFhZjc0ODMiLCJzdWIiOiIyMGNkMGU0Yy1jMTdjLTQ4Y2UtYWU1MC1iYWU0NjFhZjc0ODMiLCJhdWQiOiJTTUFDREZDMjVGNTI0ODQ0RTA1MzAxMDAwMDdGQURCNCIsIm5iZiI6MTY5MDMxMjM5NiwiaWF0IjoxNjkwMzEyMzk2LCJleHAiOjE2OTA0ODUxOTZ9.oCfPTfu-RnW5w2bGRcnSUeXlBW0RjzoU5mBTO0lNDBMp-w1Yv_lipCIyt7GF4RNE1CTq1X-cR18peKfCEQw5Ig"
        headers["clientId"] = "SMACDFC25F524844E0530100007FADB4"
        headers["Content-Type"] = "application/json"
        headers["Client-IP"] = "0.0.0.0"
        headers["Client-App"] = "android-classic"
        headers["Request-ID"] = UUID.randomUUID().toString()
        headers["deviceId"] = "644f0139ae0cc2ae"
        headers["Client-UserAgent"] = "Mozilla/5.0 (Linux; Android 13; Pixel 5a Build/TQ3A.230605.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/114.0.5735.196 Mobile Safari/537.36"
        headers["Client-App-Version"] = "playstore-dev"
        headers["OS-Version"] = "33"
        headers["Device-Name"] = "Google-Android SDK built for x86"
        headers["archive"] = isArchive.toString()

        return headers
    }

    companion object {
        const val BASE_URL = "https://id.digitalink.com/"//"https://subs-stage.washingtonpost.com/preferenceapi/v1/saved/story/"
        const val TWENTY_FOUR_HOURS_IN_MILLISECONDS = 86400000
        const val POST = "POST"
        const val LIST = "list"
        const val SAVE = "save"
        const val DELETE = "delete"
        const val MOCK_LIST_RESPONSE = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"state\": \"192\",\n" +
                "  \"saved\": [\n" +
                "    {\n" +
                "      \"arcId\": \"ABC123\",\n" +
                "      \"contentType\": \"sample-type\",\n" +
                "      \"section\": \"top-stories\",\n" +
                "      \"location\": \"https://www.washingtonpost.com/politics/2023/06/16/stock-prices-are-an-imperfect-way-measure-culture-war-casualties/\",\n" +
                "      \"userCreated\": 1686974817835,\n" +
                "      \"userUpdated\": 1686974817438\n" +
                "    },\n" +
                "    {\n" +
                "      \"arcId\": \"ABC123\",\n" +
                "      \"contentType\": \"sample-type\",\n" +
                "      \"section\": \"top-stories\",\n" +
                "      \"location\": \"https://www.washingtonpost.com/investigations/2023/06/19/fbi-resisted-opening-probe-into-trumps-role-jan-6-more-than-year/\",\n" +
                "      \"userCreated\": 1687290356750,\n" +
                "      \"userUpdated\": 1687293956398\n" +
                "    },\n" +
                "    {\n" +
                "      \"arcId\": \"ABC123\",\n" +
                "      \"contentType\": \"sample-type\",\n" +
                "      \"section\": \"top-stories\",\n" +
                "      \"location\": \"https://www.washingtonpost.com/national-security/2023/06/20/trump-trial-date/\",\n" +
                "      \"userCreated\": 1687290509933,\n" +
                "      \"userUpdated\": 1687294463504\n" +
                "    },\n" +
                "    {\n" +
                "      \"arcId\": \"ABC123\",\n" +
                "      \"contentType\": \"sample-type\",\n" +
                "      \"section\": \"top-stories\",\n" +
                "      \"location\": \"https://www.washingtonpost.com/politics/2023/06/22/biden-impeachment-house-committees-lauren-boebert/\",\n" +
                "      \"userCreated\": 1687459155157,\n" +
                "      \"userUpdated\": 1687459154629\n" +
                "    },\n" +
                "    {\n" +
                "      \"arcId\": \"ABC123\",\n" +
                "      \"contentType\": \"sample-type\",\n" +
                "      \"section\": \"top-stories\",\n" +
                "      \"location\": \"https://www.washingtonpost.com/investigations/2023/06/20/veterans-work-foreign-governments-bill/\",\n" +
                "      \"userCreated\": 1687459665913,\n" +
                "      \"userUpdated\": 1687459665913\n" +
                "    },\n" +
                "    {\n" +
                "      \"arcId\": \"ABC123\",\n" +
                "      \"contentType\": \"sample-type\",\n" +
                "      \"section\": \"top-stories\",\n" +
                "      \"location\": \"https://www.washingtonpost.com/opinions/2023/06/22/dictators-trade-toolkits-cling-power/\",\n" +
                "      \"userCreated\": 1687461229526,\n" +
                "      \"userUpdated\": 1687461224189\n" +
                "    }\n" +
                "  ]\n" +
                "}"

        const val MOCK_SAVE_RESPONSE = "{\n" +
                "    \"status\": \"SUCCESS\",\n" +
                "    \"state\": \"192\",\n" +
                "    \"uris\": [\n" +
                "        {\n" +
                "            \"location\": \"url0\",\n" +
                "            \"userCreated\": 1686664122572,\n" +
                "            \"userUpdated\": 1687534181886,\n" +
                "            \"archived\": false,\n" +
                "            \"status\": \"SAVED\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"location\": \"url1\",\n" +
                "            \"userCreated\": 1686664122572,\n" +
                "            \"userUpdated\": 1687534181886,\n" +
                "            \"archived\": false,\n" +
                "            \"status\": \"SAVED\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"location\": \"url2\",\n" +
                "            \"userCreated\": 1686664122572,\n" +
                "            \"userUpdated\": 1687534181886,\n" +
                "            \"archived\": false,\n" +
                "            \"status\": \"SAVED\"\n" +
                "        }\n" +
                "    ]\n" +
                "}"

        const val MOCK_DELETE_RESPONSE = "{\n" +
                "    \"status\": \"SUCCESS\",\n" +
                "    \"state\": \"192\",\n" +
                "    \"uris\": [\n" +
                "        {\n" +
                "            \"location\": \"url0\",\n" +
                "            \"userCreated\": 1686664122572,\n" +
                "            \"userUpdated\": 1687534181886,\n" +
                "            \"archived\": false,\n" +
                "            \"status\": \"DELETED\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"location\": \"url1\",\n" +
                "            \"userCreated\": 1686664122572,\n" +
                "            \"userUpdated\": 1687534181886,\n" +
                "            \"archived\": false,\n" +
                "            \"status\": \"DELETED\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"location\": \"url2\",\n" +
                "            \"userCreated\": 1686664122572,\n" +
                "            \"userUpdated\": 1687534181886,\n" +
                "            \"archived\": false,\n" +
                "            \"status\": \"DELETED\"\n" +
                "        }\n" +
                "    ]\n" +
                "}"
    }
}
