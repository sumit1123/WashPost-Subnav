// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.washingtonpost.userhistory.repo

interface UserHistoryMetaProvider {
    fun getUserHistoryMeta(): UserHistoryMetaData

    fun getAccessToken(): AccessToken
}

/**
 * Additional information from the app to pass along to Flex Api or User History Service
 */
data class UserHistoryMetaData(
    val loginId: String?,
    val jucId: String?,
    val jtId: Long?,
    val clientId: String?,
    val deviceId: String?,
    val appVersion: String?,
    val platform: String?,
    val privacyConsentGiven: Boolean,
)

data class AccessToken(
    val accessToken: String?
)