package com.wapo.flagship.features.ask.domain

enum class SseDataType(val value: String) {
    NEW_CONVERSATION_ID("new_conversation_id"),
    SYSTEM_MESSAGE("system_message"),
    REPLY("reply"),
    AUDIO_REPLY("audio_reply"),
    SOURCES("sources"),
    TURN_ID("turn_id"),
    THINKING_STATE_UPDATE("thinking_state_update"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): SseDataType {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

enum class ConversationRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): ConversationRole {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}
