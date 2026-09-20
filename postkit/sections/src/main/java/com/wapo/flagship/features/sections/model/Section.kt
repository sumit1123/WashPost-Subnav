package com.wapo.flagship.features.sections.model

import com.google.gson.annotations.SerializedName

data class Section(val id: String,
                   val bundleName: String,
                   val name: String,
                   val title: String,
                   val childSections: MutableList<Section> = ArrayList(),
                   var displayName: String? = null,
                   val sectionType: SectionType?
)

enum class SectionType {
    @SerializedName("section")
    SECTION,
    @SerializedName("link")
    WEB
}