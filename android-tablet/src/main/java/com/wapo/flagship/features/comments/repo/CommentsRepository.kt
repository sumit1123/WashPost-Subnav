package com.wapo.flagship.features.comments.repo

import com.wapo.flagship.features.comments.model.CommentsServiceRequest
import com.wapo.flagship.features.comments.model.CommentsServiceResponse
import com.wapo.flagship.features.comments.service.CommentsService
import com.wapo.flagship.network.retrofit.network.APIResult

class CommentsRepository(val service: CommentsService) {

    suspend fun getCommentsData(
        body: CommentsServiceRequest,
        endpoint: String
    ): APIResult<CommentsServiceResponse> {
        return service.getCommentsData(endpoint, body)
    }
}
