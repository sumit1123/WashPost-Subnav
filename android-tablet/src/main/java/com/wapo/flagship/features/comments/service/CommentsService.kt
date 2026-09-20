package com.wapo.flagship.features.comments.service

import com.wapo.flagship.features.comments.model.CommentsServiceRequest
import com.wapo.flagship.features.comments.model.CommentsServiceResponse
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface CommentsService {

    @Headers("Content-Type: application/json")
    @POST
    suspend fun getCommentsData(
        @Url endpoint: String,
        @Body request: CommentsServiceRequest
    ): APIResult<CommentsServiceResponse>
}
