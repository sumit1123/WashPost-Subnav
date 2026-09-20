package com.wapo.flagship.external

import com.wapo.flagship.common.getMenuSections
import com.wapo.flagship.external.storage.WidgetType
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.WidgetSection
import java.util.ArrayList

class WidgetData {
    data class Articles(
        val categoryName: String,
        val categoryPath: String,
    ) : ArrayList<ArticleWrapper>()

    data class ArticleWrapper(
        val label: String?,
        val headline: String,
        val contentUrl: String,
        val mediaUrl: String?,
    )

    companion object {
        private const val TAG = "WidgetData"
        const val EXTRAS_WIDGET_TYPE = "WidgetType"

        @JvmStatic
        fun getSectionsList(): List<WidgetSection> {
            val config = ConfigManager.getInstance().config
            val widgetSection = config.widgetSections

            val menuSections = getMenuSections()?.flatMap { listOf(it) + it.sectionInfo }
            return widgetSection.filter { ws ->
                menuSections?.firstOrNull { ms ->
                    ms.bundleName == ws.bundleName || ms.bundleName == ws.fusionBundleName
                } != null
            }
        }

        @JvmStatic
        fun getArticlesUrls(items: Articles?): List<String> {
            val urls = ArrayList<String>()
            if (items == null) {
                return urls
            }

            for (item in items) {
                if (item.contentUrl.isNotEmpty()) {
                    urls.add(item.contentUrl)
                }
            }
            return urls
        }

        @JvmStatic
        fun getFusionBundleName(bundleName: String): String? {
            val widgetSection = getSectionsList().firstOrNull { it.bundleName == bundleName }
            return widgetSection?.fusionBundleName
        }
    }
}
