/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.aixp.repo

import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackRequest
import com.wapo.flagship.features.aixp.models.PostAnswersFeedbackResponse
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.aixp.services.PostAnswersAiService

class PostAnswersFeedbackRepository(
    val postAnswersAiService: PostAnswersAiService
) {
    suspend fun submitConversationFeedback(
        feedback: PostAnswersFeedbackRequest,
        uuid: String?
    ): APIResult<PostAnswersFeedbackResponse> {
        return postAnswersAiService.submitConversationFeedback(feedback, PostAnswersAiService.getHeaders(), uuid)
    }
}
