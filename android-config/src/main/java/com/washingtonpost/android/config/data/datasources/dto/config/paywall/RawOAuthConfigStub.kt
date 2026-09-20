package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywall.OAuthConfigStub
import java.util.UUID

@JsonClass(generateAdapter = true)
data class RawOAuthConfigStub(
    @Json(name = "authorizationUrl") val authorizationUrl: String? = null,
    @Json(name = "signUpUrl") val signUpUrl: String? = null,
    @Json(name = "freeTrialUrl") val freeTrialUrl: String? = null,
    @Json(name = "authorizationScope") val authorizationScope: String? = null,
    @Json(name = "authorizationState") val authorizationState: String? = null,
    @Json(name = "tokenUrlV2") val tokenUrl: String? = null,
    @Json(name = "oneLinkTokenUrl") val oneLinkTokenUrl: String? = null,
    @Json(name = "profileUrlV2") val profileUrl: String? = null,
    @Json(name = "revokeUrl") val revokeUrl: String? = null,
    @Json(name = "migrateUrl") val migrateUrl: String? = null,
    @Json(name = "appType") val appType: String? = null,
    @Json(name = "saveIdentityPreferencesUrl") val saveIdentityPreferencesUrl: String? = null,
    @Json(name = "nonceUrl") val nonceUrl: String? = null,
) {
    fun mapToDomain(): OAuthConfigStub {
        return OAuthConfigStub(
            baseAuthorizationUrl = authorizationUrl
                ?: "https://www.washingtonpost.com/subscribe/signin?case=noa&wpflow=native-android",
            signUpUrl = signUpUrl ?: "/signup",
            freeTrialUrl = freeTrialUrl ?: "/signup/free-trial",
            authorizationScope = "profile_access_scope",
            authorizationState = UUID.randomUUID().toString(),
            tokenUrl = tokenUrl ?: "https://login.washingtonpost.com/identity/oauth/v2/token",
            oneLinkTokenUrl = oneLinkTokenUrl ?: "https://subscribe.washingtonpost.com/oauth/one-link/token/",
            profileUrl = profileUrl ?: "https://login.washingtonpost.com/identity/oauth/v2/profile",
            revokeUrl = revokeUrl ?: "https://login.washingtonpost.com/identity/oauth/v1/revoke",
            migrateUrl = migrateUrl ?: "https://login.washingtonpost.com/identity/oauth/v1/migrate",
            appType = appType ?: "classic",
            saveIdentityPreferencesUrl = saveIdentityPreferencesUrl ?: "https://subscribe.washingtonpost.com/user/save-identity-preferences",
            nonceUrl = nonceUrl.orEmpty(),
        )
    }
}