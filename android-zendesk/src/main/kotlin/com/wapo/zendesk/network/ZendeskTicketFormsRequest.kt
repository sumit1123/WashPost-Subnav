package com.wapo.zendesk.network

import com.wapo.android.commons.util.Logger
import com.google.gson.Gson
import com.wapo.zendesk.model.ZendeskTicketForm
import com.washingtonpost.android.volley.Cache
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.ParseError
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser
import com.washingtonpost.android.volley.toolbox.JsonRequest
import java.util.concurrent.TimeUnit

class ZendeskTicketFormsRequest(
    url: String?,
    listener: Response.Listener<ZendeskTicketForm>,
    errorListener: Response.ErrorListener
) : JsonRequest<ZendeskTicketForm>(Method.GET, url, null, listener, errorListener) {
    private val gson = Gson()

    init {
        Logger.d(TAG, "Zendesk request url=${url}")
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<ZendeskTicketForm> {
        return try {
            val zendeskResponse =
                gson.fromJson(response.data.toString(Charsets.UTF_8), ZendeskTicketForm::class.java)
            var entry = HttpHeaderParser.parseCacheHeaders(response)
            if (entry == null) {
                entry = Cache.Entry()
                entry.data = response.data
                entry.responseHeaders = response.headers
            }

            if (entry.softTtl <= 0) {
                entry.softTtl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
            }
            if (entry.ttl <= 0) {
                entry.ttl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
            }

            Response.success(zendeskResponse, entry)
        } catch (t: Throwable) {
            Response.error(ParseError(t))
        }
    }

    companion object {
        private val TAG: String = ZendeskTicketFormsRequest::class.java.simpleName
    }
}