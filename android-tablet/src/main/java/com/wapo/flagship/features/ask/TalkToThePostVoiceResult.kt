package com.wapo.flagship.features.ask

sealed class TalkToThePostVoiceResult {
    data class Success(val text: String): TalkToThePostVoiceResult()
    data class Failure(val errorCode: Int): TalkToThePostVoiceResult()
}
