package com.wapo.flagship.features.grid.model

data class CarouselAudioPlaylist(
    val headlines: List<String>,
    val cta: CompoundLabel?,
    val cardify: Boolean?
): Item()