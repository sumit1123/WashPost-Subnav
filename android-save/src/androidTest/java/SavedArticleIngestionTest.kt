/* Copyright (c) 2019 The Washington Post. All rights reserved. */

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.wapo.android.commons.util.AppContextUtils
import com.washingtonpost.android.save.network.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Call
import retrofit2.Callback
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

@RunWith(AndroidJUnit4::class)
class SavedArticleIngestionTest {

    private val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private lateinit var preferenceNetwork : SavedRetrofit.PreferenceNetwork


    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getTargetContext()
        AppContextUtils.init(appContext, "appName")
        preferenceNetwork = SavedRetrofit.getInstance().getPreferenceNetwork(BASE_URL)
    }

    @Test
    @Throws(Exception::class)
    fun parseReadSavedArticlesResponse() {

        preferenceNetwork.getSavedArticlesList(getHeaders(), SavedStoriesRequest(listOf())).enqueue(
            object : Callback<SavedStoriesResponse> {
                override fun onFailure(p0: Call<SavedStoriesResponse>, throwable: Throwable) {
                    fail()
                }

                override fun onResponse(p0: Call<SavedStoriesResponse>, response: retrofit2.Response<SavedStoriesResponse>) {
                    assertTrue(true)
                }
            }
        )
    }

    @Test
    @Throws(Exception::class)
    fun postAndReadSavedArticlesResponse(){

        val transactionList = mutableListOf<UrisRequestValue>()
        transactionList.add(UrisRequestValue("url1", System.currentTimeMillis()))
        val transactionsRequest = SavedStoriesRequest(transactionList)

        preferenceNetwork.saveArticles(getHeaders(), transactionsRequest).enqueue(
            object : Callback<SavedStoriesResponse> {
                override fun onFailure(call: Call<SavedStoriesResponse>, throwable: Throwable) {
                    fail()
                }

                override fun onResponse(call: Call<SavedStoriesResponse>, response: retrofit2.Response<SavedStoriesResponse>) {
                    assertTrue(true)
                }
            }
        )
    }

    private fun getHeaders() : HashMap<String, String> {
        val headers = hashMapOf<String,String>()

        headers["authorization"] = "Bearer " + "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiIwODY5Q0Y0ODY1NjVFQjZDRTA1MDAwN0YwMTAwNkRFMSIsInN1YiI6IjA4NjlDRjQ4NjU2NUVCNkNFMDUwMDA3RjAxMDA2REUxIiwiYXVkIjoiNkJEMDlGQzI1RjUyNDg0NEUwNTMwMTAwMDA3RkFEQjQiLCJuYmYiOjE1NDgzMzUxMDgsImlhdCI6MTU0ODMzNTEwOCwiZXhwIjoxNjExNDA3MTA4fQ.lNFrdosyzsauycFrm93K3g8BSV3i9191qvr7hK3cQ_ljCzbrksivGiKAHrd3kgBM_uTP4HwhD-qcAi19J2cm0A"
        headers["clientId"] = "6BD09FC25F524844E0530100007FADB4"
        headers["Content-Type"] = "application/json"
        headers["Client-IP"] = "0.0.0.0"
        headers["Client-App"] = "android-classic"
        headers["Request-ID"] = UUID.randomUUID().toString()
        headers["deviceId"] = "644f0139ae0cc2ae"
        headers["Client-UserAgent"] = "Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.026; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Safari/537.36 "
        headers["Client-App-Version"] = "playstore-dev"
        headers["OS-Version"] = "27"
        headers["Device-Name"] = "Google-Android SDK built for x86"

        return headers
    }

    companion object {
        const val BASE_URL = "https://id.digitalink.com/"
    }
}