package com.wapo.flagship.auto.ask

interface AskErrorAudioProvider {
    suspend fun loadErrorAudio(type: AskErrorAudioType): ByteArray?
}

enum class AskErrorAudioType {
    RECOGNITION,
    RECOGNITION_MAX,
    API,
}
