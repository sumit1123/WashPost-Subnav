// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ask.services

import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.COOKIE
import com.wapo.android.commons.constants.USER_AGENT
import com.wapo.android.commons.constants.X_APP_NAME
import com.wapo.android.commons.constants.X_SURFACE_NAME
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.ask.models.AskQuestionsResponse
import com.wapo.flagship.features.ask.repo.AskQuestionsRepo
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants.LOGIN_ID_PARAM
import com.washingtonpost.android.paywall.util.PaywallConstants.SECURE_LOGIN_ID_PARAM
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interface for accessing AskQuestions feed
 */
interface AskQuestionsService {

    @POST("questions/landing-page/suggested")
    suspend fun getFeedV2(
        @Header(AskQuestionsRepo.WP_TIMEOUT) timeoutMs: Int,
        @Body request: AskQuestionsServiceRequest
    ): APIResult<AskQuestionsResponse>

    companion object {
        fun getHeaders(): HashMap<String, String> {
            val hashMap = hashMapOf(
                Pair(X_APP_NAME, "ask_the_post"),
                Pair(X_SURFACE_NAME, "landing_page"),
                Pair(CLIENT_APP, "android-classic"),
                Pair(USER_AGENT, AppContextUtils.appApiUserAgent)
            )
            PaywallService.getInstance()?.loggedInUser?.let {
                hashMap.put(
                    COOKIE,
                    "$LOGIN_ID_PARAM=${it.uuid}; $SECURE_LOGIN_ID_PARAM=${it.secureLoginID}"
                )
            }
            return hashMap
        }
    }
}
