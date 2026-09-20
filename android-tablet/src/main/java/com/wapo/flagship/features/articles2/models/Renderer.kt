package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json

enum class Renderer(
    val value: String,
) {
    @Json(name = "default")
    DEFAULT("default"),

    @Json(name = "web")
    WEB("web"),

    @Json(name = "native")
    NATIVE("native"),
}
