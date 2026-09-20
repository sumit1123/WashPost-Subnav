package com.wapo.flagship.features.map.models

data class MapWallUiData(
    val label: String?,
    val wpdsIconName: String?,
    val title: String?,
    val byline: String?,
    val url: String?,
    val imageUrl: String?,
    val type: MapWallType = MapWallType.FLEX
)

enum class MapWallType(val typeName: String) {
    FLEX("flex")
}
