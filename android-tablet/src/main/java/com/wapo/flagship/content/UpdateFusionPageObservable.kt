// Copyright (c) 2020 The Washington Post. All rights reserved.

package com.wapo.flagship.content

import com.google.gson.Gson
import com.wapo.flagship.common.errors.SectionParseError
import com.wapo.flagship.common.errors.SectionServerError
import com.wapo.flagship.features.grid.FusionMapper
import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.json.NativeFourFifteen
import com.washingtonpost.android.volley.*
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser
import com.washingtonpost.android.wapocontent.Priority
import rx.Observable
import rx.Subscriber
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class UpdateFusionPageObservable private constructor(
    onSubscribe: OnSubscribe<GridEntity>,
) : Observable<GridEntity>(onSubscribe) {
    companion object {
        @JvmStatic
        fun create(
            url: String,
            queue: RequestQueue,
            priority: Priority,
            shouldByPassCache: Boolean = false,
        ): UpdateFusionPageObservable {
            val onSubscribe =
                OnSubscribe<GridEntity> { subscriber ->
                    val request = FreshDataOnlyPageRequest(url, subscriber)
                    request.priority = Request.Priority(priority.toInt())
                    request.setShouldBypassCache(shouldByPassCache)
                    queue.add(request)
                }

            return UpdateFusionPageObservable(onSubscribe)
        }

        private val gson = Gson()
        private val pattern = Pattern.compile(".*charset=(.+)", Pattern.CASE_INSENSITIVE)
    }

    class FreshDataOnlyPageRequest(
        url: String,
        subscriber: Subscriber<in GridEntity>,
    ) : RxHelperRequest<GridEntity?>(Request.Method.GET, url, null) {
        private var subscriber: Subscriber<in GridEntity>? = subscriber
        private var cacheHitReported = false

        override fun parseNetworkResponse(response: NetworkResponse?): Response<GridEntity?> =
            try {
                val json = String(response?.data ?: byteArrayOf(), Charset.defaultCharset())
                val page =
                    FusionMapper.gson.fromJson(
                        json,
                        GridEntity::class.java,
                    )

                val entry = HttpHeaderParser.parseCacheHeaders(response)

                if (entry.ttl <= 0) {
                    entry.ttl = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12)
                }

                Response.success(page, entry)
            } catch (e: Exception) {
                Response.error<GridEntity>(SectionParseError(e).apply { originalJson = response?.data?.let { String(it, Charsets.UTF_8) } })
            }

        override fun parseNetworkError(volleyError: VolleyError?): VolleyError {
            val nr = volleyError?.networkResponse ?: return super.parseNetworkError(volleyError)

            if (nr.statusCode == NativeFourFifteen.RESPONSE_CODE) {
                try {
                    val matcher = pattern.matcher(nr.headers["Content-Type"] ?: "")
                    val charSet =
                        if (matcher.matches() && matcher.groupCount() >= 1) {
                            try {
                                Charset.forName(matcher.group(1))
                            } catch (e: Exception) {
                                Charsets.UTF_8
                            }
                        } else {
                            Charsets.UTF_8
                        }

                    val redirect =
                        gson.fromJson(
                            String(nr.data, charSet),
                            NativeFourFifteen::class.java,
                        )
                    return FourFifteenError(redirect.contentUrl)
                } catch (e: Exception) {
                }
            } else if (volleyError is ServerError) {
                return SectionServerError(nr)
            }

            return super.parseNetworkError(volleyError)
        }

        @Synchronized
        override fun onComplete() {
            val subs = subscriber ?: return

            if (!subs.isUnsubscribed) {
                subs.onCompleted()
            }

            subscriber = null
        }

        @Synchronized
        override fun deliverResponse(response: GridEntity?) {
            response ?: return
            val subs = subscriber ?: return
            if (subs.isUnsubscribed) {
                subscriber = null
                return
            }

            //
            // cached data reported first. ignore it
            //
            if (isCacheHit && !cacheHitReported) {
                cacheHitReported = true
                return
            }

            subs.onNext(response)
        }

        @Synchronized
        override fun deliverError(error: VolleyError?) {
            val subs = subscriber ?: return

            if (subs.isUnsubscribed) {
                subscriber = null
                return
            }

            subs.onError(error)
            subscriber = null
        }
    }
}
