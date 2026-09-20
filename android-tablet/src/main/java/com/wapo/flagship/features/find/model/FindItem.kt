package com.wapo.flagship.features.find.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.wapo.flagship.features.newsprint.NewsprintHelper
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.Config
import com.wpds.theme.wpdsColors

open class FindItem(
    open val title: String,
    val type: FindType,
    val span: Int = 1,
    var navType: NavigationBehavior? = null,
    val new: Boolean? = false,
)

data class HighlightItem(
    override val title: String,
    val id: String,
    val imageId: Int,
    val textColor: Color,
    val highlightBoxType: HighlightBoxType,
    val link: String,
    val newLabel: Boolean? = false,
) : FindItem(
        title,
        FindType.HIGHLIGHT,
        navType = NavigationBehavior.FIND_TAB_HIGHLIGHT,
        new = newLabel,
    )

data class SectionBoxItem(
    override val title: String,
    val bundleId: String,
) : FindItem(title, FindType.SECTION_BOX)

data class SectionBarItem(
    override val title: String,
    val bundleId: String,
    var hasDivider: Boolean = true,
) : FindItem(title, FindType.SECTION_BAR, 2)

data class HeaderItem(
    override val title: String,
) : FindItem(title, FindType.HEADER, 2)

data class AskQuestionsItem(
    override val title: String,
) : FindItem(title, FindType.ASK_QUESTIONS, 2)

enum class HighlightBoxType {
    PRINT,
    COMICS,
    RECIPES,
    NEWSPRINT,
    CLIMATE,
    ELECTIONS,
    HOROSCOPES,
    RIPPLE
    ;

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() =
            when (this) {
                PRINT -> wpdsColors.findPrintBox
                COMICS -> wpdsColors.findComicsBox
                RECIPES -> wpdsColors.findRecipesBox
                NEWSPRINT -> wpdsColors.findNewsprintBox
                CLIMATE -> wpdsColors.findClimateBox
                ELECTIONS -> wpdsColors.findElectionsBox
                HOROSCOPES -> wpdsColors.findHoroscopesBox
                RIPPLE -> wpdsColors.findRippleBox
            }

    val link: String
        get() {
            return when (this) {
                COMICS -> "/entertainment/comics"
                CLIMATE -> "/climate-environment/climate-answers/"
                RECIPES -> "/tablet/" +
                        ""
                NEWSPRINT -> NewsprintHelper.NEWSPRINT_URL
                ELECTIONS -> "/electionsearch/?_focus=term"
                HOROSCOPES -> "https://www.washingtonpost.com/entertainment/horoscopes/"
                RIPPLE -> "https://washingtonpost.com/ripple/"
                else -> ""
            }
        }
}

enum class FindType {
    HEADER,
    HIGHLIGHT,
    SECTION_BOX,
    SECTION_BAR,
    ASK_QUESTIONS,
}
