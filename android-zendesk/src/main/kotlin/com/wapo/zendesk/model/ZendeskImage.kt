package com.wapo.zendesk.model

import java.io.Serializable

data class ZendeskImage(val uri: String, val fileName: String, val mimeType: String) :
    Serializable {
    var status: Status = Status.IDLE
}

enum class Status {
    IDLE, PENDING, PROGRESS, FINISHED
}
