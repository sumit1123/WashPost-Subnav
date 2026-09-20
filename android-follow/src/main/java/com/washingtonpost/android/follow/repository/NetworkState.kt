package com.washingtonpost.android.follow.repository

enum class Status {
    RUNNING,
    SUCCESS,
    FAILED,
    NO_RESULTS
}

@Suppress("DataClassPrivateConstructor")
data class NetworkState private constructor(
        val status: Status) {
    companion object {
        val LOADED = NetworkState(Status.SUCCESS)
        val LOADING = NetworkState(Status.RUNNING)
        val ERROR = NetworkState(Status.FAILED)
        val NOT_FOUND = NetworkState(Status.NO_RESULTS)
    }
}