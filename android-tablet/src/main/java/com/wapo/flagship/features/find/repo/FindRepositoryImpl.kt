package com.wapo.flagship.features.find.repo

import androidx.compose.ui.graphics.Color
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.config.Section
import com.wapo.flagship.content.WapoConfigManager
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.data.RecentSection
import com.wapo.flagship.domain.repository.FindRepository
import com.wapo.flagship.features.find.model.HighlightBoxType
import com.wapo.flagship.features.find.model.HighlightItem
import com.wapo.flagship.features.sections.model.SectionType
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FindRepositoryImpl
    @Inject
    constructor(
        val configManager: WapoConfigManager?,
        val cacheManager: CacheManager?,
        val dispatcherProvider: DispatcherProvider,
    ): FindRepository {
        val isPhone = !AppContextUtils.isTablet()

        private var barCache: List<MenuSection>? = null
        private var azCache: List<MenuSection>? = null
        private var featuredCache: List<MenuSection>? = null

        private var recentSections = linkedSetOf<RecentSection>()

        private val allSections: MutableList<MenuSection> = mutableListOf()

        private var highlightedBundleNames: List<String> = listOf()

        init {
            highlightedBundleNames =
                getHighlightItems().map {
                    it.link
                }
        }

        override fun getMenuSection(sectionId: String): MenuSection? {
            getAllSections()
                .flatMap { listOf(it) + it.sectionInfo }
                .find {
                    sectionId == it.databaseId
                }?.let {
                    return it
                }

            return null
        }

        private fun getAllSections(): List<MenuSection> {
            if (allSections.isNotEmpty()) {
                return allSections
            }

            allSections.clear()
            allSections.addAll(getFeaturedSections())
            allSections.addAll(getAZSections())
            allSections.addAll(getBarSections())

            return allSections
        }

        override fun getFeaturedSections(): List<MenuSection> {
            featuredCache?.let {
                return it
            }
            val list = configManager?.sectionsFeaturedConfig?.sections
            featuredCache = getSectionsAsMenuSections(list, isPhone)
            return featuredCache ?: listOf()
        }

        override fun getAZSections(): List<MenuSection> {
            azCache?.let {
                return it
            }
            val list = configManager?.sectionsAZConfig?.sections
            azCache = getSectionsAsMenuSections(list, isPhone)
            return azCache ?: listOf()
        }

        private fun getBarSections(): List<MenuSection> {
            barCache?.let {
                return it
            }
            val list = configManager?.sectionsBarConfig?.sections
            barCache = getSectionsAsMenuSections(list, isPhone)
            return barCache ?: listOf()
        }

        /**
         * Gets the 4 most recently viewed sections excluding Highlighted sections.
         */
        override suspend fun getRecentSections(): List<RecentSection> =
            withContext(dispatcherProvider.io) {
                recentSections.clear()
                val recentSectionsExcludingHighlights =
                    cacheManager?.recentSections?.filter { section ->
                        !highlightedBundleNames.contains(section.bundleName)
                    }
                recentSectionsExcludingHighlights?.takeLast(4)?.reversed()?.forEach {
                    recentSections.add(it)
                }
                recentSections.toList()
            }

        /**
         * TODO - Remove these and depended functions when we do compose refactor. We need these right now to map Sections to MenuSection.
         */
        private fun getSectionsAsMenuSections(
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

        private fun getSectionDisplayName(
            section: Section,
            useNavName: Boolean,
        ): String = if (useNavName && !section.sectionNavName.isNullOrEmpty()) section.sectionNavName else section.sectionName

        private fun getSectionType(section: Section): String =
            when {
                section.isComics() -> MenuSection.COMICS_TYPE
                section.isFusion() -> MenuSection.SECTION_TYPE_FUSION
                section.sectionType == SectionType.SECTION -> MenuSection.SECTION_TYPE
                section.sectionType == SectionType.WEB -> MenuSection.WEB_TYPE
                else -> MenuSection.SECTION_TYPE
            }

        private fun Section?.isFusion(): Boolean = !this?.fusionPath.isNullOrEmpty()

        private fun Section?.isComics(): Boolean = !this?.sectionPathComics.isNullOrEmpty()

        override fun getHighlightItems(): List<HighlightItem> {
            val config = ConfigManager.getInstance().config
            val itemConfigs = config.findHighlightItemsConfig
                    .associateBy({ it.id }, { it.position })

            val highlightItems = mutableListOf<HighlightItem>().apply {
                addAll(
                    listOf(
                        HighlightItem(
                            "Print Edition",
                            "print_edition",
                            R.drawable.find_print,
                            Color.White,
                            HighlightBoxType.PRINT,
                            HighlightBoxType.PRINT.link,
                        ),
                        HighlightItem(
                            "Recipes",
                            "recipes",
                            R.drawable.find_recipes,
                            Color.Black,
                            HighlightBoxType.RECIPES,
                            HighlightBoxType.RECIPES.link,
                        ),
                        HighlightItem(
                            "Your year in news",
                            "newsprint",
                            R.drawable.find_newsprint,
                            Color.White,
                            HighlightBoxType.NEWSPRINT,
                            HighlightBoxType.NEWSPRINT.link,
                            true,
                        ),
                        HighlightItem(
                            "Comics",
                            "comics",
                            R.drawable.find_comics,
                            Color.Black,
                            HighlightBoxType.COMICS,
                            HighlightBoxType.COMICS.link,
                        ),
                        HighlightItem(
                            "Climate Answers",
                            "climate",
                            R.drawable.find_climate,
                            Color.Black,
                            HighlightBoxType.CLIMATE,
                            HighlightBoxType.CLIMATE.link,
                            true,
                        ),
                        HighlightItem(
                            "Election results",
                            "election",
                            R.drawable.find_elections,
                            Color.White,
                            HighlightBoxType.ELECTIONS,
                            HighlightBoxType.ELECTIONS.link,
                        ),
                        HighlightItem(
                            "Horoscopes",
                            "horoscopes",
                            R.drawable.horoscopes,
                            Color.Black,
                            HighlightBoxType.HOROSCOPES,
                            HighlightBoxType.HOROSCOPES.link,
                        ),
                        HighlightItem(
                            "Ripple: Outside Voices",
                            "ripple",
                            R.drawable.find_ripple,
                            Color.Black,
                            HighlightBoxType.RIPPLE,
                            HighlightBoxType.RIPPLE.link,
                        )
                    )
                )
            }

            val map = mutableMapOf<Int, HighlightItem>()

            highlightItems.forEach { item ->
                val position = itemConfigs[item.id]

                position?.let {
                    map[it] = item
                }
            }

            if (map.isEmpty()) return emptyList()
            return map
                .toSortedMap()
                .values
                .toList()
                .subList(0, 4.coerceAtMost(map.size))
        }
    }
