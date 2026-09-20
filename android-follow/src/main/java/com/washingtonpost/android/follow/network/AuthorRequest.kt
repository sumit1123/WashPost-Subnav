package com.washingtonpost.android.follow.network

import com.wapo.android.commons.util.Logger
import com.google.gson.Gson
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.volley.Cache
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.ParseError
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser
import com.washingtonpost.android.volley.toolbox.JsonRequest
import java.util.concurrent.TimeUnit

class AuthorRequest(url: String?, listener: Response.Listener<AuthorItem>, errorListener: Response.ErrorListener) : JsonRequest<AuthorItem>(Method.GET, url, null, listener, errorListener) {
    private val gson = Gson()

    init {
        Logger.d(TAG, "Author request url=${url}")
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<AuthorItem> {
        return try {
            val authorResponse = gson.fromJson(response.data.toString(Charsets.UTF_8), AuthorItem::class.java)
            var entry = HttpHeaderParser.parseCacheHeaders(response)
            if (entry == null) {
                entry = Cache.Entry()
                entry.data = response.data
                entry.responseHeaders = response.headers
            }

            if (entry.softTtl <= 0) {
                entry.softTtl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
            }
            if (entry.ttl <= 0) {
                entry.ttl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
            }

            Response.success(authorResponse, entry)
        } catch (t: Throwable) {
            Response.error(ParseError(t))
        }
    }

    companion object {
        private val TAG: String = AuthorRequest::class.java.simpleName
    }
}