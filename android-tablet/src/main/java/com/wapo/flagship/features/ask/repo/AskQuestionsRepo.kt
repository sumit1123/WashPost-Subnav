package com.wapo.flagship.features.ask.repo

import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.ask.models.AskQuestionsResponse
import com.wapo.flagship.features.ask.models.CategoryItem
import com.wapo.flagship.features.ask.models.QuestionItem

interface AskQuestionsRepo {
    val defaultQuestions: List<QuestionItem>
    val defaultCategories: List<CategoryItem>

    suspend fun getQuestions(skipCache: Boolean = false): APIResult<AskQuestionsResponse>

    fun setDefaultCategories(categories: List<CategoryItem>)

    fun setDefaultQuestions(questions: List<QuestionItem>)

    companion object {
        const val WP_TIMEOUT = "WP_TIMEOUT"
    }
}