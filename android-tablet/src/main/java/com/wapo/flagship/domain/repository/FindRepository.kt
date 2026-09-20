package com.wapo.flagship.domain.repository

import com.wapo.flagship.data.RecentSection
import com.wapo.flagship.features.find.model.HighlightItem
import com.wapo.flagship.json.MenuSection

interface FindRepository {

    fun getMenuSection(sectionId: String): MenuSection?

    fun getFeaturedSections(): List<MenuSection>

    fun getAZSections(): List<MenuSection>

    suspend fun getRecentSections(): List<RecentSection>

    fun getHighlightItems(): List<HighlightItem>
}
