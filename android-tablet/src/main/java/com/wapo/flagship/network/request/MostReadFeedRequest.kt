/*
 * Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.flagship.network.request

import com.google.gson.Gson
import com.wapo.flagship.features.articles.recirculation.model.MostReadFeed
import com.washingtonpost.android.volley.Cache
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.ParseError
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser
import com.washingtonpost.android.volley.toolbox.JsonRequest
import java.util.concurrent.TimeUnit

/**
 * Created by Jayesh Elamgodil on 08/24/19.
 */
class MostReadFeedRequest(
    url: String,
    listener: Response.Listener<MostReadFeed>,
    errorListener: Response.ErrorListener,
) : JsonRequest<MostReadFeed>(Method.GET, url, null, listener, errorListener) {
    private val gson = Gson()

    override fun parseNetworkResponse(response: NetworkResponse): Response<MostReadFeed> =
        try {
            val mostReadFeed =
                gson.fromJson(
                    response.data.toString(Charsets.UTF_8),
                    MostReadFeed::class.java,
                )
            var entry = response.entry
            if (entry == null) {
                entry = HttpHeaderParser.parseCacheHeaders(response)
                if (entry == null) {
                    entry = Cache.Entry()
                    entry.data = response.data
                    entry.responseHeaders = response.headers
                }

                entry.softTtl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3)
                entry.ttl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(48)
            }

            Response.success(mostReadFeed, entry)
        } catch (t: Throwable) {
            Response.error(ParseError(t))
        }
}
