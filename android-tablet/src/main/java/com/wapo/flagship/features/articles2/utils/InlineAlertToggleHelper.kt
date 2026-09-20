package com.wapo.flagship.features.articles2.utils

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.InlineAlertToggleItem
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml
import com.wapo.flagship.features.articles2.models.deserialized.Toggle
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.InlineAlertToggleMappingConfig

/**
 * Helper class for inline alert toggle logic
 */
class InlineAlertToggleHelper {
    /**
     * @returns the InlineAlertToggleItem for the article if the conditions are met,
     * otherwise null
     * Conditions:
     * - Article sourcesection has a mapping match in the config
     * - Article sourcesubsection is not in the excluded sourcesubsections for the mapping
     * - Article has at least MIN_PARAGRAPHS paragraphs of text
     * - User is not already opted into the corresponding alert segment
     */
    fun getInlineAlertToggleItem(
        article: Article2,
        items: MutableList<Item>,
    ): InlineAlertToggleItem? {
        getAlertSegmentMapping(article)?.let {
            if (alertsDisabledForSegment(it.alertSegmentKey) && getParagraphCount(items) >= MIN_PARAGRAPHS) {
                return InlineAlertToggleItem(
                    topicDisplayName = getTopicDisplayName(it.alertSegmentKey),
                    topicKey = it.alertSegmentKey,
                )
            }
        }
        return null
    }

    fun articleHasToggle(article: Article2): Boolean {
        val mapping = getAlertSegmentMapping(article)
        val items = article.items
        return mapping != null &&
            items != null &&
            alertsDisabledForSegment(mapping.alertSegmentKey) &&
            getParagraphCount(items) >= MIN_PARAGRAPHS
    }

    private fun getTopicDisplayName(alertSegmentKey: String?): String? {
        val alertsTopicsList = FlagshipApplication.getInstance().alertsSettings.getAlertsTopicsList()
        val topicInfo =
            alertsTopicsList.firstOrNull {
                it.topic.topicKey == alertSegmentKey
            }
        return topicInfo?.topic?.displayName
    }

    fun convertToggleToInlineAlertToggleItem(toggle: Toggle): InlineAlertToggleItem? {
        val alertsTopicsList = FlagshipApplication.getInstance().alertsSettings.getAlertsTopicsList()
        val alertIndex =
            alertsTopicsList.indexOfFirst {
                it.topic.topicKey == toggle.key
            }
        if (alertIndex == -1) return null
        val alert = alertsTopicsList[alertIndex].topic
        return InlineAlertToggleItem(toggle.type, alert.displayName, alert.topicKey).apply {
            group = toggle.group
        }
    }

    /**
     * @returns the appropriate inlineAlertToggleMappingConfigs object for the article
     * or null if there is no mapping match,
     * or null if there is a match, but sourcesubsection is in the excluded sourcesubsections list
     */
    private fun getAlertSegmentMapping(article: Article2): InlineAlertToggleMappingConfig? {
        val segmentMappingConfigs = ConfigManager.getInstance().config.inlineAlertToggleMappingConfigs
        val mapping =
            segmentMappingConfigs.firstOrNull {
                article.sourcesection in it.includedSourceSections
            } ?: return null

        return if (article.sourcesubsection !in mapping.excludedSourceSubsections) {
            mapping
        } else {
            null
        }
    }

    /**
     * @returns true if alerts for segment are disabled
     */
    fun alertsDisabledForSegment(alertSegmentKey: String?): Boolean {
        val alertsTopicsList = FlagshipApplication.getInstance().alertsSettings.getAlertsTopicsList()
        val appAlertTopic =
            alertsTopicsList.firstOrNull {
                it.topic.topicKey == alertSegmentKey
            }

        return appAlertTopic?.let { !it.isEnabled } ?: false
    }

    /**
     * @returns the index after the date item for The Seven Briefs articles
     * or the index after the PARAGRAPHS_BEFORE_TOGGLE paragraph for all other article types
     * or -1 if there are less than PARAGRAPHS_BEFORE_TOGGLE paragraphs
     */
    fun getToggleIndex(
        items: MutableList<Item>,
        inlineAlertToggleItem: InlineAlertToggleItem,
        article: Article2,
    ): Int {
        val indexOfItemBeforeToggle =
            when {
                inlineAlertToggleItem.topicKey == THE_SEVEN_BRIEFS_TOPIC_KEY ->
                    items.indexOf(
                        getDate(items),
                    )
                getParagraphCount(items) >= PARAGRAPHS_BEFORE_TOGGLE ->
                    items.indexOf(
                        getParagraphs(items)[PARAGRAPHS_BEFORE_TOGGLE - 1],
                    )
                else -> -1
            }

        return if (indexOfItemBeforeToggle != -1) {
            indexOfItemBeforeToggle + 1
        } else {
            EventLog
                .Builder()
                .apply {
                    setMessage(
                        "Tried to get inline alert toggle index for article that does not meet toggle conditions",
                    )
                    setModule(LogModules.ARTICLES)
                    setContentUrl(article.contenturl)
                }.run {
                    RemoteLog.e(FlagshipApplication.getInstance(), build())
                }
            indexOfItemBeforeToggle
        }
    }

    private fun getDate(items: MutableList<Item>): Item? = items.firstOrNull { it::class == Date::class }

    /**
     * @returns the number of paragraphs in the article
     */
    private fun getParagraphCount(items: List<Item>): Int =
        items.count {
            isParagraph(it)
        }

    private fun getParagraphs(items: MutableList<Item>): List<Item> =
        items.filter {
            isParagraph(it)
        }

    private fun isParagraph(item: Item): Boolean = (item as? SanitizedHtml)?.subtype == "paragraph"

    companion object {
        private val TAG: String = InlineAlertToggleHelper::class.java.simpleName
        private const val MIN_PARAGRAPHS = 5
        private const val PARAGRAPHS_BEFORE_TOGGLE = 3
        private const val THE_SEVEN_BRIEFS_TOPIC_KEY = "the7_briefs"
    }
}
