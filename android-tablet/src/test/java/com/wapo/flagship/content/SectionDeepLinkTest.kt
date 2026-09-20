package com.wapo.flagship.content

import com.wapo.flagship.config.Section
import com.wapo.flagship.config.SiteServiceConfig
import com.wapo.flagship.config.findSectionByPathAliases
import com.wapo.flagship.features.sections.model.SectionType
import org.junit.Assert
import org.junit.Test

/**
 * Test path matching section metadata logic
 */
class SectionDeepLinkTest {
    val foodSection =
        Section(
            sectionId = "/lifestyle/food",
            sectionType = SectionType.SECTION,
            sectionName = "Food",
            sectionNavName = "Food",
            aliases = listOf("/lifestyle/food/", "/food"),
            childrenBeforeFold = 0,
            sectionPathComics = null,
            fusionPath = "/lifestyle/food",
            sections = null,
            logoImage = null,
            icon = null,
            sectionSubType = null,
            behavior = null,
            displayDate = null,
            sectionPath = null,
            sectionPathTablet = null,
        )

    val siteServiceConfig: SiteServiceConfig =
        SiteServiceConfig(
            1,
            "sectionId",
            listOf(foodSection),
        )

    @Test
    fun pathMatchesAlias() {
        Assert.assertTrue(foodSection.matches("/food"))
    }

    @Test
    fun pathMatchesId() {
        Assert.assertTrue(foodSection.matches("/lifestyle/food"))
    }

    @Test
    fun pathMatchesSectionPath() {
        val foodSection = Section(
            sectionId = "/lifestyle/food",
            sectionType = SectionType.SECTION,
            sectionName = "Food",
            sectionNavName = "Food",
            aliases = listOf("/lifestyle/food/", "/food"),
            childrenBeforeFold = 0,
            sectionPathComics = null,
            fusionPath = null,
            sections = null,
            logoImage = null,
            icon = null,
            sectionSubType = null,
            behavior = null,
            displayDate = null,
            sectionPath = "lifestyle_food",
            sectionPathTablet = "lifestyle_food",
        )
        Assert.assertTrue(foodSection.matches("lifestyle_food"))
    }

    @Test
    fun testFindSectionByPathAliases() {
        Assert.assertTrue(findSectionByPathAliases("/food", siteServiceConfig) != null)
    }
}
