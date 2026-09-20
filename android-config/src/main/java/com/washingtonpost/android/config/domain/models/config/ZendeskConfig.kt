package com.washingtonpost.android.config.domain.models.config

data class ZendeskConfig(
    val url: String,
    val customFields: ZendeskCustomFields,
    val ticketForms: List<TicketForm>,
)

data class ZendeskCustomFields(
    val metadataId: Long,
    val appType: String,
    val appTypeId: Long,
    val mobileApp: String,
    val mobileAppId: Long,
)

data class TicketForm(
    val id: Long,
    val name: String,
)
