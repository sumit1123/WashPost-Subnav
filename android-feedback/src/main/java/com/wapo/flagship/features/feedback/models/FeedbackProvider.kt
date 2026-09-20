package com.wapo.flagship.features.feedback.models

interface FeedbackProvider {
    fun type(): String
    fun contentId(): String
    fun metadata(): FeedbackMetadata?
    fun description(): String?
    fun scale(): Int?
}