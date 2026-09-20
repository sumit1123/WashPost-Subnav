package com.wapo.flagship.config

import com.google.gson.annotations.SerializedName
import com.wapo.android.commons.config.BaseConfig
import com.wapo.flagship.features.sections.model.SectionType
import com.wapo.flagship.json.MenuSection
import com.washingtonpost.android.BuildConfig
import java.io.Serializable
import com.wapo.flagship.features.sections.model.Section as ModelSection

class SiteServiceConfig(
    @SerializedName("version")
    val version: Int,
    @SerializedName("id") val sectionId: String,
    @SerializedName("children") val sections: List<Section>,
) : BaseConfig(),
    Serializable {
    companion object {
        val TAG = "SiteServiceConfig"
        val D = BuildConfig.DEBUG
    }

    fun getModelSections(
        isPhone: Boolean,
        useNavName: Boolean = true,
    ): List<ModelSection> {
        val list: MutableList<ModelSection> = ArrayList()
        return extractModelSections(sections, list, isPhone, useNavName)
    }

    private fun extractModelSections(
        from: List<Section>?,
        to: MutableList<ModelSection>,
        isPhone: Boolean,
        useNavName: Boolean,
    ): List<ModelSection> {
        from ?: return to

        from
            .filter { isSupportedType(it.sectionType) }
            .forEach { s ->
                val sectionPath = s.getDeviceSectionPath(isPhone) // also known as Bundle Name
                if (!sectionPath.isNullOrEmpty()) {
                    val modelSection =
                        ModelSection(
                            id = s.sectionId,
                            bundleName = sectionPath,
                            name = getSectionDisplayName(s, useNavName),
                            title = s.sectionName,
                            sectionType = s.sectionType,
                        )
                    to.add(modelSection)
                    if (!s.sections.isNullOrEmpty()) {
                        extractModelSections(
                            s.sections,
                            modelSection.childSections,
                            isPhone,
                            useNavName,
                        )
                    }
                }
            }

        return to
    }

    fun getSectionsAsMenuSections(
        from: List<Section>?,
        isPhone: Boolean,
        useNavName: Boolean = true,
        isUnlisted: Boolean = false,
    ): List<MenuSection>? {
        from ?: return ArrayList<MenuSection>(0)
        val to = ArrayList<MenuSection>()
        from.forEach { s ->
            val sectionPath = s.getDeviceSectionPath(isPhone) // also known as Bundle Name
            if (!sectionPath.isNullOrEmpty()) {
                to.add(
                    if (s.childrenBeforeFold != null) {
                        MenuSection(
                            s.sectionName,
                            getSectionDisplayName(s, useNavName),
                            getSectionType(s),
                            s.sectionId,
                            sectionPath,
                            getSectionsAsMenuSections(s.sections, isPhone, useNavName)?.toTypedArray(),
                            s.childrenBeforeFold,
                            s.aliases,
                            isUnlisted,
                        )
                    } else {
                        MenuSection(
                            s.sectionName,
                            getSectionDisplayName(s, useNavName),
                            getSectionType(s),
                            s.sectionId,
                            sectionPath,
                            getSectionsAsMenuSections(s.sections, isPhone, useNavName)?.toTypedArray(),
                            3,
                            s.aliases,
                            isUnlisted,
                        )
                    },
                )
            }
        }

        return to
    }

    private fun getSectionType(section: Section): String =
        when {
            section.isComics() -> MenuSection.COMICS_TYPE
            section.isFusion() -> MenuSection.SECTION_TYPE_FUSION
            section.sectionType == SectionType.SECTION -> MenuSection.SECTION_TYPE
            section.sectionType == SectionType.WEB -> MenuSection.WEB_TYPE
            else -> MenuSection.SECTION_TYPE
        }

    private fun getSectionDisplayName(
        section: Section,
        useNavName: Boolean,
    ): String = if (useNavName && !section.sectionNavName.isNullOrEmpty()) section.sectionNavName else section.sectionName

    private fun isSupportedType(type: SectionType?): Boolean {
        // Note: allowing only native sections for now.
        return type == null || type == SectionType.SECTION
    }
}

class Section
    @JvmOverloads
    constructor(
        @SerializedName("id") val sectionId: String,
        @SerializedName("type") val sectionType: SectionType?,
        @SerializedName("path") val sectionPath: String?,
        @SerializedName("path_tablet") val sectionPathTablet: String?,
        @SerializedName("path_comics") val sectionPathComics: String?,
        @SerializedName("name") val sectionName: String,
        @SerializedName("nav_name") val sectionNavName: String?,
        @SerializedName("children_before_fold") val childrenBeforeFold: Int?,
        @SerializedName("children") val sections: List<Section>? = emptyList(),
        @SerializedName("path_fusion") val fusionPath: String?,
        @SerializedName("aliases") val aliases: List<String>? = emptyList(),
        @SerializedName("logo_image") val logoImage: String?,
        @SerializedName("icon") val icon: String?,
        @SerializedName("subtype") val sectionSubType: String?,
        @SerializedName("behavior") val behavior: String?,
        @SerializedName("display_date") val displayDate: String?,
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Section) {
                return false
            }

            return (sectionId == other.sectionId) && (sectionName == other.sectionName)
        }

        override fun hashCode() = (sectionId + sectionName).hashCode()

        fun getDeviceSectionPath(isPhone: Boolean): String? =
            when {
                isFusion() -> fusionPath.orEmpty()
                !isPhone && !sectionPathTablet.isNullOrEmpty() -> sectionPathTablet
                !sectionPath.isNullOrEmpty() -> sectionPath
                else -> null
            }?.trimEnd('/')

        /**
         * Return true or false based whether supplied path matches a Section's id, path(fusion/pb), or one its aliases.
         */
        fun matches(path: String): Boolean {
            var allAliases = mutableListOf<String>()
            allAliases.add(sectionId)
            aliases?.let { allAliases.addAll(it) }
            if (fusionPath != null) {
                allAliases.add(fusionPath.trimEnd('/'))
            } else {
                sectionPath?.let { allAliases.add(it.trimEnd('/')) }
                sectionPathTablet?.let { allAliases.add(it.trimEnd('/')) }
            }

            return allAliases.contains(path)
        }
    }
