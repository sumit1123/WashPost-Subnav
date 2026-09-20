package com.washingtonpost.android.wapocontent

open class RequestData(val url : String?, val uuid : String?) {
    var priority = Priority(Priority.Group.FOREGROUND, System.currentTimeMillis())
    var shouldBypassCache = false
}