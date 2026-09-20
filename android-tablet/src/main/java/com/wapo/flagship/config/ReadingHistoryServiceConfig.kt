package com.wapo.flagship.config

import com.google.gson.annotations.SerializedName

data class ReadingHistoryServiceConfig(
    @SerializedName("url")
    val url: String = "https://subscribe.washingtonpost.com/"
)