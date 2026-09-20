package com.wapo.android.commons.engagement

import com.wapo.android.commons.util.Logger
import java.util.UUID

abstract class EngagementTrace(
    val id: String= UUID.randomUUID().toString(),
    var startTimeMillis: Long? = null,
    var endTimeMillis: Long? = null,
) {
    val engagedTimeMillis: Long
        get() {
            val startTime = this.startTimeMillis
            val endTime = this.endTimeMillis
            if (startTime == null || endTime == null || endTime < startTime) {
                return -1L
            }
            return endTime - startTime
        }

    open fun isValid(): Boolean = engagedTimeMillis > 0L

    fun startTrace() {
        Logger.v(TAG, "starting trace: id=$id")
        if (startTimeMillis != null) {
            Logger.w(TAG, "trace $id has already been started")
            return
        }
        startTimeMillis = System.currentTimeMillis()
    }

    fun stopTrace() {
        Logger.v(TAG, "stopping trace: id=$id")
        if (endTimeMillis != null) {
            Logger.w(TAG, "trace $id has already been stopped")
            return
        }
        endTimeMillis = System.currentTimeMillis()
    }

    companion object {
        private const val TAG = "EngagementTrace"
    }
}

class SessionEngagementTrace(
    id: String = "session",
    startTimeMillis: Long? = null,
    endTimeMillis: Long? = null
) : EngagementTrace(id, startTimeMillis, endTimeMillis)

class PageEngagementTrace(
    id: String = UUID.randomUUID().toString(),
    startTimeMillis: Long? = null,
    endTimeMillis: Long? = null
) : EngagementTrace(id, startTimeMillis, endTimeMillis) {

    var pageName: String = ""
        private set
    var contentType: String = ""
        private set
    var tabName: String = ""
        private set

    fun updateTrackingInfo(
        pageName: String? = null,
        contentType: String? = null,
        tabName: String? = null
    ) {
        if (pageName != null) this.pageName = pageName
        if (contentType != null) this.contentType = contentType
        if (tabName != null) this.tabName = tabName
    }

    override fun isValid(): Boolean = super.isValid() && pageName.isNotEmpty()
}
