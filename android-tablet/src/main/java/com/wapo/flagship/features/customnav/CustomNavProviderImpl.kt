package com.wapo.flagship.features.customnav

import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.data.RecentSection
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.features.sections.model.SectionType
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.customnav.CustomNavProvider
import java.util.ArrayList

class CustomNavProviderImpl : CustomNavProvider {
    val isPhone = !AppContextUtils.isTablet()

    override fun getDefaultBarSections(): List<Section> =
        filterWebViewSections(
            FlagshipApplication
                .getInstance()
                .contentManager.wapoConfigManager.sectionsBarConfig
                .getModelSections(
                    isPhone,
                ).distinctBy { it.id },
        )

    override fun getLockedSections(): List<String> = ConfigManager.getInstance().config.customNavConfig.lockedSections

    override fun getRecommendedSections(): List<Section> =
        filterWebViewSections(
            FlagshipApplication
                .getInstance()
                .contentManager.wapoConfigManager.sectionsRecommendedConfig
                .getModelSections(
                    isPhone,
                ).distinctBy { it.id },
        )

    override fun getRecentSections(): List<Section> =
        filterWebViewSections(
            FlagshipApplication.getInstance().cacheManager.recentSections.map { recentSection ->
                Section(
                    recentSection.bundleName,
                    recentSection.bundleName,
                    recentSection.name,
                    recentSection.name,
                    ArrayList<Section>(),
                    recentSection.name,
                    getSectionType(recentSection),
                )
            },
        )

    /**
     * To match iOS, prevent webview sections from displaying on section front ribbon.
     */
    private fun filterWebViewSections(sectionList: List<Section>): List<Section> =
        sectionList.filter {
            it.sectionType != SectionType.WEB
        }

    private fun getSectionType(recentSection: RecentSection): SectionType =
        when (recentSection.sectionType) {
            MenuSection.WEB_TYPE -> SectionType.WEB
            else -> SectionType.SECTION
        }

    override fun trackCustomNavPageView() {
        Measurement.trackCustomNavPageView()
    }

    override fun trackCustomNavEnroll(
        sectionDisplayName: String,
        isEnroll: Boolean,
    ) {
        Measurement.trackCustomNavEnroll(sectionDisplayName, isEnroll)
    }

    override fun trackCustomNavReset() {
        Measurement.trackCustomNavReset()
    }
}
