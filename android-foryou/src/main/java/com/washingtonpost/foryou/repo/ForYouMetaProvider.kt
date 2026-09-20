package com.washingtonpost.foryou.repo

interface ForYouMetaProvider {
    fun getForYouMeta() : ForYouMetaData
}

/**
 * Additional information from the app to pass along to Flex Api or User History Service
 */
data class ForYouMetaData(
    val loginId: String?,
    val sessionId: String?,
    val clientId: String?,
    val privacyConsentGiven: Boolean
)