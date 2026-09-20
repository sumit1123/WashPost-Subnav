/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.android.commons.logs

/**
 * Class to maintain common keys that the app modules can use them in the RemoteLogs.
 * [EventLog] has default setters to set these.
 */
enum class LogKeys(val keyName: String) {
    CONTENT_URL("content_url"),
    MESSAGE("message"),
    ERROR_MESSAGE("err_msg"),
    ERROR_CODE("err_code"),
    MODULE("module"),
    UUID("uuid"),
    FORCE_UPLOAD("force_upload"),
    SAMPLING_RATE("sampling_rate"),
    MANUFACTURER("manufacturer")
}