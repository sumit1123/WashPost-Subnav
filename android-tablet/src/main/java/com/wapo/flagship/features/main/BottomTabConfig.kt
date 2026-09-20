package com.wapo.flagship.features.main

import com.google.gson.annotations.SerializedName

/**
 * Data class that holds the information for a bottom tab from the config
 */
data class BottomTabConfig(
    @SerializedName("id")
    var id: String?,
    @SerializedName("displayName")
    var displayName: String?,
    @SerializedName("bundleName")
    var bundleName: String?,
    @SerializedName("image")
    var image: String?
)
