package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json

enum class Host {
    @Json(name = "vimeo")
    VIMEO,

    @Json(name = "posttv")
    POSTTV,

    @Json(name = "youtube")
    YOUTUBE,
}
