package com.wapo.flagship.network.request

import com.google.gson.Gson
import com.wapo.flagship.features.grid.views.vote.VoteGuide
import com.washingtonpost.android.volley.Cache
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.ParseError
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser
import com.washingtonpost.android.volley.toolbox.JsonRequest
import java.util.concurrent.TimeUnit

class VoteRequest(
    url: String,
    listener: Response.Listener<VoteGuide>,
    errorListener: Response.ErrorListener,
) : JsonRequest<VoteGuide>(
        Method.GET,
        url,
        null,
        listener,
        errorListener,
    ) {
    private val gson = Gson()

    override fun parseNetworkResponse(response: NetworkResponse): Response<VoteGuide> =
        try {
            val result =
                gson.fromJson<VoteGuide>(
                    response.data.toString(Charsets.UTF_8),
                    VoteGuide::class.java,
                )
            var entry = HttpHeaderParser.parseCacheHeaders(response)
            if (entry == null) {
                entry = Cache.Entry()
                entry.data = response.data
                entry.responseHeaders = response.headers
            }

            if (entry.softTtl <= 0) {
                entry.softTtl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
            }
            if (entry.ttl <= 0) {
                entry.ttl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12)
            }

            Response.success(result, entry)
        } catch (t: Throwable) {
            Response.error(ParseError(t))
        }
}
