// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.common.errors

import com.wapo.android.commons.exceptions.LoggableError
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.ServerError

class SectionServerError(
    networkResponse: NetworkResponse,
) : ServerError(networkResponse),
    LoggableError
