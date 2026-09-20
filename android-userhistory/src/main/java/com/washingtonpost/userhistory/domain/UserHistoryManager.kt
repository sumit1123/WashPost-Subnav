package com.washingtonpost.userhistory.domain

import com.washingtonpost.userhistory.models.ConclusionState
import com.washingtonpost.userhistory.models.VideoConclusionState

interface UserHistoryManager {
    fun getWapoLoginId(): String?
    fun getJucId(): String?
    fun getJtId(): String?
    fun getDeviceId(): String?
    fun getAppVersion(): String?
    fun getPlatform(): String?
    fun getClientEventTime(): String
    suspend fun postUserHistoryEvents()
    suspend fun postPageViewEvent()
    suspend fun postPushEvent(
        url: String,
        pushId: String?,
        pushType: String,
        testGroup: String,
        isPushClicked: Boolean,
        loginId: String?
    )
    fun writeVideoViewedEvent(
        id: String?,
        section: String,
        autoplayDuration: Long?,
        watchDuration: Long?,
        totalMilliseconds: Long?,
        videoConclusionState: VideoConclusionState
    )
    fun writePushListenerEvent(
        url: String,
        pushId: String?,
        pushType: String,
        testGroup: String,
        loginId: String?,
        isPushClicked: Boolean
    )
    fun captureContentListenConclusion(
        id: String?,
        conclusionState: ConclusionState,
        listenDepthSec: Long?,
        duration: Long?,
        contentType: String?
    )
}