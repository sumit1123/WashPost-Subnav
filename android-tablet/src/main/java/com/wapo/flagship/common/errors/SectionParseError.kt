package com.wapo.flagship.common.errors

import com.wapo.android.commons.exceptions.LoggableError
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.VolleyError

class SectionParseError :
    VolleyError,
    LoggableError {
    constructor() : super()
    constructor(cause: Throwable?) : super(cause)
    constructor(response: NetworkResponse?) : super(response)
    constructor(exceptionMessage: String?) : super(exceptionMessage)
    constructor(exceptionMessage: String?, reason: Throwable?) : super(exceptionMessage, reason)

    var originalJson: String? = null
}
