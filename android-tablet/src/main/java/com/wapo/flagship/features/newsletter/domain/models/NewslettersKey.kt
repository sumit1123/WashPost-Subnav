package com.wapo.flagship.features.newsletter.domain.models

sealed class NewslettersKey {
    data class Id(val value: String) : NewslettersKey()
    data class List(val value: String) : NewslettersKey()
}