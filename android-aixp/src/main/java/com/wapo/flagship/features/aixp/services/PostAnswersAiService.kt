package com.wapo.flagship.features.aixp.services

import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.COOKIE
import com.wapo.android.commons.constants.USER_AGENT
import com.wapo.android.commons.constants.X_APP_NAME
import com.wapo.android.commons.constants.X_SURFACE_NAME
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.aixp.models.AskThePostShare
import com.wapo.flagship.features.aixp.models.AskThePostShareTurnRequest
import com.wapo.flagship.features.aixp.models.ConversationTurn
import com.wapo.flagship.features.aixp.models.HistoryResponse
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackRequest
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants.LOGIN_ID_PARAM
import com.washingtonpost.android.paywall.util.PaywallConstants.SECURE_LOGIN_ID_PARAM
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PostAnswersAiService {
    @Headers("Content-Type: application/json")
    @GET("converse/history")
    suspend fun getSessionHistory(
        @HeaderMap headers: HashMap<String, String>,
        @Query("uuid") uuid: String?,
    ): APIResult<List<HistoryResponse>>

    @Headers("Content-Type: application/json")
    @GET("converse/history/{conversation_id}")
    suspend fun getConversationHistory(
        @Path("conversation_id") conversationId: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("uuid") uuid: String?,
    ): APIResult<List<ConversationTurn>>

    @Headers("Content-Type: application/json")
    @POST("converse/feedback")
    suspend fun submitConversationFeedback(
        @Body feedback: PostAnswersFeedbackRequest,
        @HeaderMap headers: HashMap<String, String>,
        @Query("uuid") uuid: String?
    ): APIResult<PostAnswersFeedbackResponse>

    @Headers("Content-Type: application/json")
    @DELETE("converse/history/{conversation_id}")
    suspend fun deleteConversation(
        @Path("conversation_id") conversationId: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("uuid") uuid: String?,
    ): APIResult<PostAnswersFeedbackResponse>

    @Headers("Content-Type: application/json")
    @POST("converse/{conversation_id}/shares")
    suspend fun shareChat(
        @Path("conversation_id") conversationId: String,
        @HeaderMap headers: HashMap<String, String>,
        @Body shareTurnRequest: AskThePostShareTurnRequest?
    ): APIResult<String?>

    @Headers("Content-Type: application/json")
    @GET("shares/{share_id}")
    suspend fun getSharedConversation(
        @Path("share_id") shareId: String,
        @HeaderMap headers: HashMap<String, String>,
    ): APIResult<HistoryResponse>

    @Headers("Content-Type: application/json")
    @GET("shares")
    suspend fun getShares(
        @HeaderMap headers: HashMap<String, String>,
    ): APIResult<List<AskThePostShare>>

    @Headers("Content-Type: application/json")
    @DELETE("shares/{share_id}")
    fun deleteShareLink(
        @Path("share_id") shareId: String,
        @HeaderMap headers: HashMap<String, String>,
    ): Call<Void>

    @Headers("Content-Type: application/json")
    @DELETE("shares")
    fun deleteAllShareLinks(
        @HeaderMap headers: HashMap<String, String>,
    ): Call<Void>

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
