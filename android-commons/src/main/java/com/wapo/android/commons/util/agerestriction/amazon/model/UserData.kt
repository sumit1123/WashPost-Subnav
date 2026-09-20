package com.wapo.android.commons.util.agerestriction.amazon.model

import java.util.Locale

data class UserData(
    val responseStatus: String?,
    val userStatus: String?,
    val ageLower: Int?,
    val ageUpper: Int?,
    val userId: String?,
    val mostRecentApprovalDate: String?
)

enum class ResponseStatus {
    SUCCESS,
    APP_NOT_OWNED,
    INTERNAL_TRANSIENT_ERROR,
    INTERNAL_ERROR,
    FEATURE_NOT_SUPPORTED
}

fun getSaveResponseStatus(value: String?): ResponseStatus {
    if (value == null) return ResponseStatus.INTERNAL_ERROR

    return try {
        ResponseStatus.valueOf(value.trim { it <= ' ' }.uppercase(Locale.getDefault()))
    } catch (e: Exception) {
        ResponseStatus.INTERNAL_ERROR
    }
}

enum class UserStatus {
    VERIFIED,
    UNKNOWN,
    SUPERVISED,
    CONSENT_NOT_GRANTED
}

fun getSaveUserStatus(value: String?): UserStatus {
    if (value.isNullOrEmpty()) return UserStatus.SUPERVISED

    return try {
        UserStatus.valueOf(value.trim { it <= ' ' }.uppercase(Locale.getDefault()))
    } catch (e: Exception) {
        UserStatus.UNKNOWN
    }
}
