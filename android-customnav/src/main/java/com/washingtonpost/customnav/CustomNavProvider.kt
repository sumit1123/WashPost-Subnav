package com.washingtonpost.customnav

import com.wapo.flagship.features.sections.model.Section

interface CustomNavProvider {

    fun getDefaultBarSections(): List<Section>

    fun getLockedSections(): List<String>

    fun getRecommendedSections(): List<Section>

    fun getRecentSections(): List<Section>

    fun trackCustomNavPageView()

    fun trackCustomNavEnroll(sectionDisplayName: String, isEnroll: Boolean)

    fun trackCustomNavReset()
}