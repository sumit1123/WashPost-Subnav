package com.washingtonpost.userhistory.repo

import com.wapo.android.commons.di.CoroutineScopeCommonsModule
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.extensions.toUri
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.washingtonpost.android.androidlive.util.DateUtil
import com.washingtonpost.userhistory.domain.UserHistoryPrefRepo
import com.washingtonpost.userhistory.domain.UserHistoryManager
import com.washingtonpost.userhistory.models.ConclusionState
import com.washingtonpost.userhistory.models.ContentListenItem
import com.washingtonpost.userhistory.models.ContentType
import com.washingtonpost.userhistory.models.PushNotificationViewItem
import com.washingtonpost.userhistory.models.UserHistoryBaseEvent
import com.washingtonpost.userhistory.models.UserHistoryEvents
import com.washingtonpost.userhistory.models.UserHistoryServicePostResponse
import com.washingtonpost.userhistory.models.VideoConclusionState
import com.washingtonpost.userhistory.models.VideoViewItem
import com.washingtonpost.userhistory.network.APIResult
import com.washingtonpost.userhistory.remote.UserHistoryEventType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class UserHistoryManagerImpl @Inject constructor(
    @CoroutineScopeCommonsModule.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userHistoryPrefRepo: UserHistoryPrefRepo,
    private val remoteLogRepo: RemoteLogRepo,
    private val deviceUtilRepo: DeviceUtilRepo
) : UserHistoryManager {
    override fun getWapoLoginId(): String? {
        return userHistoryPrefRepo.getUserHistoryMeta().loginId
    }

    override fun getJucId(): String? {
        return userHistoryPrefRepo.getUserHistoryMeta().jucId
    }

    override fun getJtId(): String? {
        return userHistoryPrefRepo.getUserHistoryMeta().jtId?.toString()
    }

    override fun getDeviceId(): String? {
        return userHistoryPrefRepo.getUserHistoryMeta().deviceId
    }

    override fun getAppVersion(): String? {
        return userHistoryPrefRepo.getUserHistoryMeta().appVersion
    }

    override fun getPlatform(): String? {
        return userHistoryPrefRepo.getUserHistoryMeta().platform
    }

    override fun getClientEventTime(): String {
        return DateUtil.getDateInISO8601WithTimezone(Date(System.currentTimeMillis()))
    }

    override suspend fun postUserHistoryEvents() {
        if (!userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven) return
        val events = userHistoryPrefRepo.readEventsFromStorage()
        if (events.isNotEmpty()) {
            withContext(ioDispatcher) {
                val result = userHistoryPrefRepo.postUserHistoryEvents(
                    UserHistoryEventType.ALL,
                    UserHistoryEvents(events.toList())
                )
                processEventPostResponse(events, result)
            }
        }
    }

    override suspend fun postPageViewEvent() {
        if (!userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven) return
        val events = userHistoryPrefRepo.readPageViewEventsFromStorage()
        if (!events.isNullOrEmpty()) {
            withContext(ioDispatcher) {
                val response = userHistoryPrefRepo.postUserHistoryEvents(
                    UserHistoryEventType.PAGEVIEW,
                    UserHistoryEvents(events.toList())
                )
                processEventPostResponse(events, response)
            }
        }
    }

    override suspend fun postPushEvent(
        url: String,
        pushId: String?,
        pushType: String,
        testGroup: String,
        isPushClicked: Boolean,
        loginId: String?
    ) {
        // Do not write or post push events if privacy consent has not been given
        if (!userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven) return

        writePushListenerEvent(url, pushId, pushType, testGroup, loginId, isPushClicked)

        val events = userHistoryPrefRepo.readPushEventsFromStorage()

        if (!events.isNullOrEmpty()) {
            withContext(ioDispatcher) {
                val jucId = getJucId()
                val consentGiven =
                    userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven
                //  Log only if jucId is null
                if (jucId.isNullOrEmpty() && consentGiven) {
                    val eventLogBuilder = EventLog
                        .Builder()
                        .setMessage("Breadcrumb – j_ucid is null in postPushEvent")
                        .set("consentGiven", consentGiven)
                        .setModule(LogModules.FOR_YOU)
                    remoteLogRepo.e(eventLogBuilder.build())
                }
                val response = userHistoryPrefRepo.postUserHistoryEvents(
                    UserHistoryEventType.PUSH_ORIGINATED,
                    UserHistoryEvents(
                        listOf(
                            PushNotificationViewItem(
                                eventType = HEADLINE_EVENT_TYPE,
                                articleId = ARTICLE_ID_NA,
                                eventSubType = PUSH_EVENT_SUB_TYPE,
                                canonicalUrl = url.toUri().path ?: "",
                                loginId = loginId,
                                jucId = jucId,
                                clientEventTime = getClientEventTime(),
                                interfase = EVENT_INTERFACE,
                                surface = PUSH_EVENT_SUB_TYPE,
                                surfaceVariant = if (deviceUtilRepo.isTablet()) "tablet" else "phone",
                                pushId = pushId ?: "",
                                pushCategory = pushType,
                                testGroup = testGroup,
                                clicked = isPushClicked,
                                shared = false,
                                deviceId = getDeviceId(),
                                appVersion = getAppVersion(),
                                devicePlatform = getPlatform()
                            )
                        )
                    )
                )
                processEventPostResponse(events, response)
            }
        }
    }

    private fun processEventPostResponse(
        sentEvents: Set<UserHistoryBaseEvent>,
        response: APIResult<UserHistoryServicePostResponse>?
    ) {
        /* We want to clear cached events if the request is successful or if it fails and the issue
           is a client side error (4xx), since there is an issue with the request and will never
           succeed */
        val is4xxError = response is APIResult.Failure && response.statusCode in 400..499
        if (response is APIResult.Success || is4xxError) {
            if (sentEvents.all { it.eventType == UserHistoryEventType.PAGEVIEW.eventName }) {
                userHistoryPrefRepo.clearPageViewEvents()
            } else if (sentEvents.all { it.eventType == UserHistoryEventType.PUSH_ORIGINATED.eventName }) {
                userHistoryPrefRepo.clearPushEvents()
            } else {
                userHistoryPrefRepo.clearAllEvents()
            }
            if (is4xxError) {
                /* TODO in the future, the API will send back a list of events that were not
                    processed. In that case, we can just write those ones to dead letter */
                userHistoryPrefRepo.writeEventsToDeadLetter(sentEvents)
            }
        }
    }

    override fun writeVideoViewedEvent(
        id: String?,
        section: String,
        autoplayDuration: Long?,
        watchDuration: Long?,
        totalMilliseconds: Long?,
        videoConclusionState: VideoConclusionState
    ) {
        // Do not write or post push events if privacy consent has not been given
        if (!userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven) return

        val videoViewItem = VideoViewItem(
            eventType = UserHistoryEventType.VIDEO_VIEWED.eventName,
            clientEventTime = getClientEventTime(),
            interfase = EVENT_INTERFACE,
            autoplayDuration = autoplayDuration?.div(1000),
            watchTotalSec = watchDuration?.div(1000),
            videoTotalSec = totalMilliseconds?.div(1000) ?: 0L,
            videoSurface = section,
            contentType = "vertical",
            videoConclusionState = videoConclusionState.stateValue,
            wapoLoginId = getWapoLoginId() ?: "",
            jucId = getJucId(),
            contentId = id ?: ""
        )
        userHistoryPrefRepo.writeVideoEventsToStorage(videoViewItem)
    }

    /**
     * Builds a PushNotificationViewItem and writes it to Pref
     */
    override fun writePushListenerEvent(
        url: String, pushId: String?, pushType: String, testGroup: String, loginId: String?, isPushClicked: Boolean
    ) {
        // Do not write or post push events if privacy consent has not been given
        if (!userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven) return

        val pushNotificationViewItem = PushNotificationViewItem(
            eventType = HEADLINE_EVENT_TYPE,
            articleId = ARTICLE_ID_NA,
            eventSubType = PUSH_EVENT_SUB_TYPE,
            canonicalUrl = url.toUri().path ?: "",
            loginId = loginId,
            jucId = getJucId(),
            clientEventTime = getClientEventTime(),
            interfase = EVENT_INTERFACE,
            surface = PUSH_EVENT_SUB_TYPE,
            surfaceVariant = if (deviceUtilRepo.isTablet()) "tablet" else "phone",
            pushId = pushId ?: "",
            pushCategory = pushType,
            testGroup = testGroup,
            clicked = isPushClicked,
            shared = false,
            deviceId = getDeviceId(),
            appVersion = getAppVersion(),
            devicePlatform = getPlatform()
        )
        userHistoryPrefRepo.writePushEventsToStorage(pushNotificationViewItem)
    }

    //region content_listen
    private var contentListenItem: ContentListenItem? = null

    override fun captureContentListenConclusion(
        id: String?,
        conclusionState: ConclusionState,
        listenDepthSec: Long?,
        duration: Long?,
        contentType: String?
    ) {
        // Do not write or post push events if privacy consent has not been given
        if (!userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven) return

        id ?: return
        val jucId = getJucId()
        val consentGiven = userHistoryPrefRepo.getUserHistoryMeta().privacyConsentGiven
        //  Log only if jucId is null and consentGiven
        if (jucId.isNullOrEmpty() && consentGiven) {
            val eventLogBuilder = EventLog
                .Builder()
                .setMessage("Breadcrumb - j_ucid=${jucId} at captureContentListenConclusion")
                .set("consentGiven", consentGiven)
                .setModule(LogModules.FOR_YOU)
            remoteLogRepo.e(eventLogBuilder.build())
        }
        val itemContentType =
            if (contentType == "PODCAST") ContentType.PODCAST else if (contentType == "AUTOMATED" || contentType == "HUMAN") ContentType.AUDIO_ARTICLE else ContentType.UNKNOWN
        val contentListenItem =
            ContentListenItem(
                contentId = id,
                wapoLoginId = getWapoLoginId(),
                jucId = jucId,
                listenDepthSec = (listenDepthSec?.div(1000))?.toInt() ?: -1,
                contentTotalSec = (duration?.div(1000))?.toInt() ?: -1,
                conclusionState = conclusionState.stateName,
                clientEventTime = getClientEventTime(),
                contentType = itemContentType.typeName
            )
        // ensure duplicate item isn't written to prefs
        if (this.contentListenItem != contentListenItem) {
            userHistoryPrefRepo.writeContentListenEventToStorage(contentListenItem)
            Logger.d(
                TAG,
                "content_listen captureContentListenConclusion()\nid: $id\nconclusionState: ${conclusionState.stateName}"
            )
        }
        this.contentListenItem = contentListenItem
    }
    //endregion content_listen

    companion object {
        private val TAG = UserHistoryManager::class.java.simpleName

        const val HEADLINE_EVENT_TYPE = "headline_view"
        const val ARTICLE_ID_NA = "(ID_NA)"
        const val PUSH_EVENT_SUB_TYPE = "push_notification"
        const val EVENT_INTERFACE = "android"
    }
}
