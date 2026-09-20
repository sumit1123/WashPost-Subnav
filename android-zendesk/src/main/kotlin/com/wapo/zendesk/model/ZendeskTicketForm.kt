package com.wapo.zendesk.model

import com.google.gson.annotations.SerializedName

data class ZendeskTicketForm(@SerializedName("ticket_forms") val ticketForms: List<TicketForm>?)

/**
 *  Can't transform this to an interface, since the @SerializedName annotations are used in
 *  Zendesk repository
 */
data class TicketForm(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?
)