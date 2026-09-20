package com.wapo.flagship.content

interface ContentUpdateRulesManager {
    enum class TimeType {
        APP_RESUME_TIME,
        APP_PAUSE_TIME,
        PAGE_STOP_TIME,
        ELECTION_LOAD_TIME,
    }

    fun clearTimes()

    fun getTime(
        timeType: TimeType,
        key: String? = null,
    ): Long

    fun setTime(
        timeType: TimeType,
        key: String? = null,
        value: Long,
    )

    fun setTime(
        timeType: TimeType,
        key: String? = null,
    )

    fun refreshNeeded(path: String?): Boolean

    fun shouldConsiderCache(path: String?): Boolean

    fun doesContentNeedRefresh(time: Long?): Boolean
}
