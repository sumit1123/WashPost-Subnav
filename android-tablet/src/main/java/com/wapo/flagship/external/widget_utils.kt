@file:JvmName("WidgetUtils")

package com.wapo.flagship.external

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.features.grid.GridProcessor
import com.wapo.flagship.features.grid.model.BreakPoints
import com.wapo.flagship.features.grid.model.HomepageStory
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.features.grid.model.PageModelMapper
import com.wapo.flagship.features.grid.model.ScreenSizeLayout
import com.washingtonpost.android.config.domain.manager.ConfigManager

fun getWidgetArticles(
    gridEntity: GridEntity?,
    sectionName: String,
    bundleName: String,
    context: Context,
): WidgetData.Articles {
    val articles = WidgetData.Articles(sectionName, bundleName)
    val intentHelper = IntentHelper()

    gridEntity?.let {
        val grid = PageModelMapper.getGrid(
            it,
            pageConfig = PageConfig.build(ConfigManager.getInstance().config)
        )
        GridProcessor().process(grid, getScreenSize(context))
        grid.regions
            .flatMap { it.items }
            .flatMap { it.items }
            .flatMap { it.items }
            .filterIsInstance<HomepageStory>()
            .mapNotNull { story ->
                var articleWrapper: WidgetData.ArticleWrapper? = null
                if (story.resolvedColumn != -1) {
                    val label = story.label?.text
                    val headline = story.headline?.text
                    val url = story.link?.url
                    val mediaUrl = story.media?.url
                    if (!headline.isNullOrBlank() && !url.isNullOrBlank()) {
                        if (isUrlAllowedToBeListed(url, intentHelper)) {
                            articleWrapper =
                                WidgetData.ArticleWrapper(
                                    label,
                                    headline,
                                    url,
                                    mediaUrl,
                                )
                        }
                    }
                }
                articleWrapper
            }.forEach { articles.add(it) }
    }
    return articles
}

private fun getScreenSize(context: Context): ScreenSizeLayout {
    val dm = context.resources.displayMetrics
    return BreakPoints.getScreenSizeLayout(dm.widthPixels.toDp(dm.density))
}

internal fun Int.toDp(density: Float): Int = (this / density).toInt()

fun isUrlAllowedToBeListed(
    @Nullable url: String?,
    @NonNull intentHelper: IntentHelper,
): Boolean = !intentHelper.isSectionURL(url)

fun notifyAppWidgets(context: Context) {
    Widget.notifyAppWidgetViewDataChanged(context)
    TabletWidget.notifyAppWidgetViewDataChanged(context)
}
