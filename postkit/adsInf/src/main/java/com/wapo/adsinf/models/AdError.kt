package com.wapo.adsinf.models

data class AdError(
    val type: ErrorType,
    override val message: String,
) : Exception(message) {

    enum class ErrorType(val errorCode: Int) {
        NO_FILL(10),
        NETWORK_ERROR(11),
        INVALID_REQUEST(12),
        RENDER_ERROR(13),
        TIMEOUT(14),
        SDK_NOT_INITIALIZED(15),
        INVALID_RENDERER(16),
        UNKNOWN(-1),
    }
}
