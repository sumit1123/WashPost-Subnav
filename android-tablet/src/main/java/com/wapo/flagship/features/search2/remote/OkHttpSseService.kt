package com.wapo.flagship.features.search2.remote

import com.squareup.moshi.JsonAdapter
import com.wapo.android.commons.constants.ACCEPT
import com.wapo.android.commons.constants.CACHE_CONTROL
import com.wapo.android.commons.constants.CONTENT_TYPE
import com.wapo.android.commons.constants.COOKIE
import com.wapo.android.commons.constants.USER_AGENT
import com.wapo.android.commons.constants.X_APP_NAME
import com.wapo.android.commons.constants.X_SURFACE_NAME
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.ask.models.AskSamEntryPoint
import com.wapo.flagship.features.search2.events.SseEvent
import com.wapo.flagship.features.search2.model.AskThePostRequest
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants.LOGIN_ID_PARAM
import com.washingtonpost.android.paywall.util.PaywallConstants.SECURE_LOGIN_ID_PARAM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class OkHttpSseService(private val okHttpClient: OkHttpClient, private val requestAdapter: JsonAdapter<AskThePostRequest>) {

    private var eventSource: EventSource? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val sseChannel = Channel<SseEvent>(Channel.UNLIMITED)

    private val _sseEvents = MutableSharedFlow<SseEvent>(
        replay = 0,
        extraBufferCapacity = 5000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val sseEvents: SharedFlow<SseEvent> = _sseEvents

    init {
        serviceScope.launch {
            for (event in sseChannel) {
                _sseEvents.emit(event)
            }
        }
    }

    private val sseListener = object : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            sseChannel.trySend(SseEvent.Open)
        }

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            sseChannel.trySend(SseEvent.Data(data, id, type))
        }

        override fun onClosed(eventSource: EventSource) {
            sseChannel.trySend(SseEvent.Closed)
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            sseChannel.trySend(SseEvent.Error(t, response))
        }
    }

    fun connect(request: Request) {
        // Ensures only one connection is active at a time
        disconnect()
        val factory = EventSources.createFactory(okHttpClient)
        eventSource = factory.newEventSource(request, sseListener)
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
        // Sending a closed event to ensure the state is updated correctly.
        sseChannel.trySend(SseEvent.ForceStop)
    }

    fun buildSseRequest(
        endpointPath: String,
        surfaceName: String?,
        askThePostRequest: AskThePostRequest
    ): Request {
        val jsonBodyString = requestAdapter.toJson(askThePostRequest)
        val requestBody = jsonBodyString.toRequestBody(JSON_MEDIA_TYPE)
        val url = ConfigManager.getInstance().config.search2Config.askThePost.converseBaseUrl

        val httpUrlBuilder = url.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegment(endpointPath)
        val httpUrl = httpUrlBuilder?.build()

        requireNotNull(httpUrl) { "Failed to build SSE URL with parameters." }

        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .header(ACCEPT, "text/event-stream")
            .header(CACHE_CONTROL, "no-cache")
            .header(X_APP_NAME, AskSamEntryPoint.ASK_THE_POST.name.lowercase())
            .header(CONTENT_TYPE, "application/json")
            .header(X_SURFACE_NAME, surfaceName ?: AskSamEntryPoint.LANDING_PAGE.name.lowercase())
            .header("CLIENT-APP", "android-classic")
            .header("User-Agent", AppContextUtils.appApiUserAgent)
            .apply {
                PaywallService.getInstance()?.loggedInUser?.let {
                    header(COOKIE, "$LOGIN_ID_PARAM=${it.uuid}; $SECURE_LOGIN_ID_PARAM=${it.secureLoginID}")
                }
            }
            .post(requestBody)
        return requestBuilder.build()
    }

    companion object {
        const val UUID = "uuid"
        const val CONVERSATION_ID = "conversation_id"
        const val MEDIA_TIMESTAMP_KEY = "timestamp"
        const val TRANSCRIPT_URL = "transcript_url"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
