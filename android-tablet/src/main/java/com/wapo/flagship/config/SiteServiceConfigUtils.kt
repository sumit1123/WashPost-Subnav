@file:JvmName("SiteServiceConfigUtils")

package com.wapo.flagship.config

import android.content.Context
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.sections.model.SectionType
import com.wapo.flagship.util.UIUtil
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.contains
import kotlin.collections.emptyList
import kotlin.collections.find
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.plus
import com.wapo.flagship.features.sections.model.Section as ModelSection

val deepLinkArbitrarySections = mutableMapOf<String, String>()

/**
 * Path is the section's Bundle Name
 */
fun findSectionByPath(
    path: String,
    vararg configs: SiteServiceConfig,
): Section? {
    for (config in configs) {
        val flatten = config.sections + config.sections.flatMap { it.sections ?: emptyList() }
        val section =
            flatten.find {
                path.trim('/') in
                    arrayOf(
                        it.fusionPath?.trim('/'),
                        it.sectionPath?.trim('/'),
                        it.sectionPathTablet?.trim('/'),
                    ) ||
                    // to allow us to use a url as a path while we wait for proper environments from jsonApp
                    path in arrayOf(it.fusionPath, it.sectionPath, it.sectionPathTablet)
            }
        if (section != null) return section
    }
    return null
}

fun findSectionByPathAliases(
    path: String,
    vararg configs: SiteServiceConfig,
): Section? {
    for (config in configs) {
        val flatten = config.sections + config.sections.flatMap { it.sections ?: emptyList() }
        val section = flatten.find { it.matches(path) }
        if (section != null) return section
    }
    return null
}

fun Section?.isFusion(): Boolean = !this?.fusionPath.isNullOrEmpty()

fun Section?.isComics(): Boolean = !this?.sectionPathComics.isNullOrEmpty()

fun getSections(
    context: Context,
    configManager: DefaultConfigManager,
): List<ModelSection> =
    ArrayList<ModelSection>().apply {
        val isPhone = UIUtil.isPhone(context)
        val topStoriesIds = context.resources.getStringArray(R.array.top_stories_ids)
        val topStoriesName = context.resources.getString(R.string.top_stories_string)

        // remove and add all bar sections to Top Stories section
        // to be consistent with featured / az sections.
        val barSections = configManager.sectionsBarConfig.getModelSections(isPhone)
        val topStoriesSection = barSections.firstOrNull { it.id in topStoriesIds }
        if (topStoriesSection != null) {
            barSections.toMutableList().apply {
                remove(topStoriesSection)
                addDebugSections(this, configManager, isPhone)
                addArbitraryDeepLinkSections(this, isPhone)
                forEach { s -> topStoriesSection.childSections.add(s) }
            }
            add(topStoriesSection)
        }

        // add features sections to the list and update displayName as "Top Stories" for all top level sections
        addAll(
            configManager.sectionsFeaturedConfig.getModelSections(isPhone).apply { forEach { it.displayName = topStoriesName } },
        )

        // add az sections to the list and update displayName as "Top Stories" for all top level sections
        addAll(
            configManager.sectionsAZConfig.getModelSections(isPhone).apply { forEach { it.displayName = topStoriesName } },
        )
        addAll(configManager.sectionsUnlistedConfig.getModelSections(isPhone))
    }

fun addDebugSections(
    sectionsList: MutableList<ModelSection>,
    configManager: DefaultConfigManager,
    isPhone: Boolean,
) {
    if (BuildConfig.DEBUG) {
        val testSectionsConfig: SiteServiceConfig = configManager.sectionsBarTestConfig
        val fusionDebug = createFusionLocalSiteService()
        val fusionSections: List<ModelSection> = fusionDebug.getModelSections(isPhone)
        val testSections: List<ModelSection> = testSectionsConfig.getModelSections(isPhone)
        sectionsList.addAll(testSections)
        sectionsList.addAll(fusionSections)
        for ((_, bundleName, name) in sectionsList) {
            Logger.d(
                SiteServiceConfig.TAG,
                String.format("Section: %s Section Path: %s", name, bundleName),
            )
        }
    }
}

fun addArbitraryDeepLinkSections(sectionsList: MutableList<ModelSection>, isPhone: Boolean) {
    if (AppContextUtils.isDebuggableBuild()) {
        val tempConfig = SiteServiceConfig(1, "fusion", getArbitrarySections())
        val arbitrarySections = tempConfig.getModelSections(isPhone)
        sectionsList.addAll(arbitrarySections)
        for ((_, bundleName, name) in arbitrarySections) {
            Logger.d(
                SiteServiceConfig.TAG,
                String.format("Section: %s Section Path: %s", name, bundleName),
            )
        }
    }
}

@Deprecated("Debug purpose, should be removed in future")
fun createFusionLocalSiteService(): SiteServiceConfig = SiteServiceConfig(1, "fusion", getFusionSection())

fun createFusionArbitrarySiteService(): SiteServiceConfig = SiteServiceConfig(1, "fusion_beta", getArbitrarySections())

private fun getFusionSection(): List<Section> =
    listOf(
        "all_features",
        "seven_live_carousel",
        "action_buttons",
        "human_read",
        "human_read_captions",
        "all_layouts",
        "all_carousels",
        "audio_carousel",
        "immersion_carousel",
        "immersion_carousel_2",
        "in_table_ad",
        "autoplay_videos",
        "live_image_dark_mode_testing",
        "cardify_autoplay_videos",
        "cardify_current",
        "kitchen_sink",
        "apps_playground",
        "videos_carousel_test",
        "videos_carousel_dev",
        "fusion_hp",
        "vertical_media",
        "brights",
        "live_image",
        "labels",
        "labels2",
        "art_position_labels",
        "headline_sizes",
        "art_widths",
        "art_widths2",
        "table_row_spans",
        "how_to_vote",
        "elections_test",
        "live_blog_wrapping",
        "arrangements",
        "olympics",
        "olympics2",
        "briefs",
        "slideshow",
        "v_align",
        "topper_label",
        "headline_font_styles",
        "listen_to_the_post_1",
        "vast_listen",
        "vast_prod_listen",
        "vast_listen_od_spy",
        "ripple_carousel",
        "hp_habit_tiles",
        "ripple_test",
        "ripple_brand_promo_test_10",
        "ripple_brand_promo_test_12",
        "side_by_side_carousel",
        "articles_tts_audio",
        "partial_jsonapp",
        "track_webview_event_jsonapp",
        "autoplay_videos_playadflag",
        "acast_test_spectrum"
    ).map {
        Section(
            sectionId = it,
            sectionType = SectionType.SECTION,
            sectionPath = "",
            sectionPathTablet = "",
            sectionPathComics = "",
            fusionPath = "/$it/",
            sectionName = it.replace("_", " "),
            sectionNavName = it.replace("_", " "),
            childrenBeforeFold = 0,
            sections = emptyList(),
            logoImage = it.replace("_", " "),
            icon = it.replace("_", " "),
            sectionSubType = it.replace("_", " "),
            behavior = it.replace("_", " "),
            displayDate = it.replace("_", " "),
        )
    }

fun getArbitrarySections(): List<Section> =
    deepLinkArbitrarySections.map {
        Section(
            sectionId = it.value,
            sectionType = SectionType.SECTION,
            sectionPath = "",
            sectionPathTablet = "",
            sectionPathComics = "",
            fusionPath = it.value,
            sectionName = it.key.replace("_", " ").replace("-", " "),
            sectionNavName = it.key.replace("_", " ").replace("-", " "),
            childrenBeforeFold = 0,
            sections = emptyList(),
            logoImage = it.key.replace("_", " ").replace("-", " "),
            icon = it.key.replace("_", " ").replace("-", " "),
            sectionSubType = it.key.replace("_", " ").replace("-", " "),
            behavior = it.key.replace("_", " ").replace("-", " "),
            displayDate = it.key.replace("_", " ").replace("-", " "),
        )
    }

private const val TOP_STORIES_PAGE_NAME = "Top Stories"
private const val TOP_STORIES_PAGE_PATH = "."

fun isTopStoriesSection(page: String?): Boolean =
    TOP_STORIES_PAGE_NAME.equals(page, ignoreCase = true) ||
        TOP_STORIES_PAGE_PATH.equals(page?.trim('/'), ignoreCase = true)
