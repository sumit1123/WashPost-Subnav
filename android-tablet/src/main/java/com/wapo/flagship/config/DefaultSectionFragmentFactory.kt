package com.wapo.flagship.config

import com.wapo.flagship.WebSectionFragment
import com.wapo.flagship.content.WapoConfigManager
import com.wapo.flagship.features.comics.ComicsListFragment
import com.wapo.flagship.features.grid.FusionSectionFragment
import com.wapo.flagship.features.sections.BaseSectionFragment
import com.wapo.flagship.features.sections.SectionFragmentFactory
import com.wapo.flagship.features.sections.model.SectionType
import com.wapo.flagship.features.wpvideos.fragments.WatchVideoFragment
import com.washingtonpost.foryou.ui.ForYouFragment
import com.washingtonpost.foryou.ui.ForYouFragment.Companion.FOR_YOU_BUNDLE_NAME

/**
 * Default factory that that creates SingleSectionFrontFragment for regular sections and WebSectionFragment for the sections with "type" == "WEB"
 */
open class DefaultSectionFragmentFactory(
    private val configManager: WapoConfigManager,
) : SectionFragmentFactory {
    override fun createFragment(
        bundleName: String?,
        displayName: String?,
    ): BaseSectionFragment =
        when {
            isComicsSection(bundleName) -> createComicsSection(bundleName, displayName)
            isWebSection(bundleName, displayName) -> createWebSection(bundleName, displayName)
            isForYouSection(bundleName) -> createForYouSection(bundleName, displayName)
            isWpVideosSection(bundleName)-> createWpVideosSection(bundleName, displayName)
            else -> createFusionSection(bundleName, displayName)
        }

    private fun createComicsSection(
        bundleName: String?,
        displayName: String?,
    ): BaseSectionFragment = ComicsListFragment.create(bundleName, displayName)

    private fun isComicsSection(bundleName: String?): Boolean {
        bundleName ?: return false
        val section = findSectionInConfigs(bundleName)
        return section != null && section.isComics()
    }


    private fun createWpVideosSection(
        bundleName: String?,
        displayName: String?,
    ): BaseSectionFragment {
        return WatchVideoFragment().create(bundleName, displayName)
    }



    private fun isWpVideosSection(bundleName: String?): Boolean {
        return bundleName == WatchVideoFragment.WP_VIDEO_BUNDLE_NAME
    }

    private fun isForYouSection(bundleName: String?): Boolean = bundleName == FOR_YOU_BUNDLE_NAME

    private fun createForYouSection(
        bundleName: String?,
        displayName: String?,
    ): BaseSectionFragment = ForYouFragment().create(bundleName, displayName)

    private fun createFusionSection(
        bundleName: String?,
        displayName: String?,
    ): BaseSectionFragment = FusionSectionFragment().create(bundleName, displayName)

    private fun isFusionSection(bundleName: String?): Boolean {
        bundleName ?: return false
        var section = findSectionInConfigs(bundleName)
        if (section != null && section.isFusion()) return true
        section = findSectionByPath(bundleName, createFusionLocalSiteService())
        if (section != null && section.isFusion()) return true
        return false
    }

    open fun createWebSection(
        bundleName: String?,
        displayName: String?,
    ) = WebSectionFragment().create(bundleName, displayName)

    open fun isWebSection(
        bundleName: String?,
        name: String?,
    ): Boolean {
        val section = findSectionInConfigs(bundleName)
        return section != null && section.sectionType == SectionType.WEB
    }

    private fun findSectionInConfigs(bundleName: String?): Section? {
        bundleName ?: return null
        return findSectionByPath(
            bundleName,
            configManager.sectionsBarConfig,
            configManager.sectionsFeaturedConfig,
            configManager.sectionsAZConfig,
            configManager.sectionsUnlistedConfig,
        )
    }
}
