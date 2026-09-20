package com.wapo.flagship.features.articles2.typeconverters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.ATPQuestions
import com.wapo.flagship.features.articles2.models.ATPQuestionsJsonAdapter

class ATPQuestionTypeConverter {
    @TypeConverter
    fun toJson(atpQuestions: ATPQuestions?): String? {
        if (atpQuestions == null) {
            return null
        }
        return ATPQuestionsJsonAdapter(Moshi.Builder().build()).toJson(atpQuestions)
    }

    @TypeConverter
    fun fromJson(data: String?): ATPQuestions? {
        if (data == null) {
            return null
        }
        return ATPQuestionsJsonAdapter(Moshi.Builder().build()).fromJson(data)
    }
}
