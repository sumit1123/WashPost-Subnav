package com.washingtonpost.customnav.data

import com.google.gson.annotations.SerializedName
import com.wapo.flagship.features.sections.model.SectionType
import java.io.Serializable

data class CustomNavSection(
    @SerializedName("id")
    val id: String,
    @SerializedName("bundleName")
    val bundleName: String,
    @SerializedName("name")
    val displayName: String,
    @SerializedName("sectionType")
    val sectionType: SectionType?,
    @SerializedName("cellType", alternate = ["type"])
    var cellType: CustomNavCellType?
) : Serializable

enum class CustomNavCellType {
    @SerializedName("locked")
    LOCKED,
    @SerializedName("selected")
    SELECTED,
    @SerializedName("recommended")
    RECOMMENDED
}
