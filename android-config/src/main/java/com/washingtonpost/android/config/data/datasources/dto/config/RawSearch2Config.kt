package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.AskThePost
import com.washingtonpost.android.config.domain.models.config.AskThePostCategory
import com.washingtonpost.android.config.domain.models.config.AskThePostQuestion
import com.washingtonpost.android.config.domain.models.config.AskThePostTimeOfDay
import com.washingtonpost.android.config.domain.models.config.Search2Config
import com.washingtonpost.android.config.domain.models.config.TimeOfDay

@JsonClass(generateAdapter = true)
data class RawSearch2Config(
    @Json(name = "baseUrl") val baseUrl: String? = null,
    @Json(name = "postAnswersEnabled") val postAnswersEnabled: Boolean? = null,
    @Json(name = "askThePost") val askThePost: RawAskThePost? = null,
    @Json(name = "learnMoreUrl") val learnMoreUrl: String? = null,
    @Json(name = "talkToThePostBaseUrl") val talkToThePostBaseUrl: String? = null,
) {
    fun mapToDomain(): Search2Config {
        return Search2Config(
            baseUrl = baseUrl
                ?: "https://tabletapi.washingtonpost.com/apps-data-service/",
            postAnswersEnabled = postAnswersEnabled ?: true,
            askThePost = (askThePost ?: RawAskThePost()).mapToDomain(),
            learnMoreUrl = learnMoreUrl
                ?: "https://www.washingtonpost.com/technology/2024/11/07/faq-ask-the-post-ai/",
            talkToThePostBaseUrl = talkToThePostBaseUrl
                ?: "https://data-ai-dev.washingtonpost.com/voice-chatbot-api/",
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawAskThePost(
    @Json(name = "askThePostTimeOfDay") val askThePostTimeOfDay: RawAskThePostTimeOfDay? = null,
    @Json(name = "questionsBaseUrl") val questionsBaseUrl: String? = null,
    @Json(name = "converseBaseUrl") val converseBaseUrl: String? = null,
    @Json(name = "questions") val questions: List<RawAskThePostQuestion>? = null,
    @Json(name = "categories") val categories: List<RawAskThePostCategory>? = null
) {
    fun mapToDomain(): AskThePost {
        return AskThePost(
            askThePostTimeOfDay = (askThePostTimeOfDay ?: RawAskThePostTimeOfDay()).mapToDomain(),
            questionsBaseUrl = questionsBaseUrl ?: "https://wapo-qa.washingtonpost.com/",
            converseBaseUrl = converseBaseUrl ?: "https://wapo-qa.washingtonpost.com/api/v2/",
            questions = questions?.map { it.mapToDomain() }.orEmpty(),
            categories = categories?.map { it.mapToDomain() }.orEmpty(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawAskThePostQuestion(
    @Json(name = "text") val text: String? = null,
    @Json(name = "uuid") val uuid: String? = null,
    @Json(name = "topicId") val topicId: String? = null,
) {
    fun mapToDomain(): AskThePostQuestion {
        return AskThePostQuestion(
            text = text.orEmpty(),
            uuid = uuid.orEmpty(),
            topicId = topicId.orEmpty(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawAskThePostCategory(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "questions") val questions: List<RawAskThePostQuestion>? = null,
) {
    fun mapToDomain(): AskThePostCategory {
        return AskThePostCategory(
            id = id.orEmpty(),
            name = name.orEmpty(),
            questions = questions?.map { it.mapToDomain() }.orEmpty(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawAskThePostTimeOfDay(
    @Json(name = "lateNight") val lateNight: RawTimeOfDay? = null,
    @Json(name = "goodEvening") val goodEvening: RawTimeOfDay? = null,
    @Json(name = "goodAfternoon") val goodAfternoon: RawTimeOfDay? = null,
    @Json(name = "goodMorning") val goodMorning: RawTimeOfDay? = null,
) {
    fun mapToDomain(): AskThePostTimeOfDay {
        return AskThePostTimeOfDay(
            lateNight = (lateNight ?: RawTimeOfDay()).mapToDomain(),
            goodEvening = (goodEvening ?: RawTimeOfDay()).mapToDomain(),
            goodAfternoon = (goodAfternoon ?: RawTimeOfDay()).mapToDomain(),
            goodMorning = (goodMorning ?: RawTimeOfDay()).mapToDomain(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawTimeOfDay(
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subTitle: String? = null,
) {
    fun mapToDomain(): TimeOfDay {
        return TimeOfDay(
            title = title.orEmpty(),
            subTitle = subTitle.orEmpty(),
        )
    }
}
