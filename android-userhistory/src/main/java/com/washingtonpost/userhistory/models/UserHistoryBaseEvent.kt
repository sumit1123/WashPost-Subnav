package com.washingtonpost.userhistory.models

open class UserHistoryBaseEvent(
    open val eventType: String,
    open val interfase: String,
    open val clientEventTime: String
)
