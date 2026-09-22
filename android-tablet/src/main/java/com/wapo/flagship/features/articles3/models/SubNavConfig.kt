package com.wapo.flagship.features.articles3.models

import com.google.gson.annotations.SerializedName
import com.wapo.android.commons.config.BaseConfig
import java.io.Serializable

class SubNavConfig(
    @SerializedName("items") val items: List<SubNavConfigItem>?,
) : BaseConfig(), Serializable

class SubNavConfigItem(
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("style") val style: String?,
    @SerializedName("behavior") val behavior: String?,
    @SerializedName("subtype") val subtype: String?,
    @SerializedName("children") val children: List<SubNavConfigItem>?,
) : Serializable
