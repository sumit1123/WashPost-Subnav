package com.wapo.flagship.navigation.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.articles2.placeholder.modifyIf
import com.wapo.view.tooltip.TooltipData
import com.wapo.view.tooltip.TooltipPopupManager
import com.wapo.view.tooltip.TooltipPopupManager.Instance.getSpannableStringWithBoldText
import com.wapo.view.tooltip.TooltipPriority
import com.wapo.view.tooltip.TooltipProperties
import com.washingtonpost.android.R
import com.wpds.theme.Typography
import com.wpds.theme.wpdsColors

enum class TopBarActionItem(val destination: String? = null) {
    Alerts,
    Settings,
    Back,
    MyPost,
    EllipsisHow,
    EllipsisWhy,
    PrivateMode,
    History,
    NewChat,
    Search,
    Share,
    GiveFeedback,
}

enum class TopBarState {
    COLLAPSED,
    TITLE_COLLAPSED,
    EXPANDED,
    HIDE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopAppBar(
    state: TopBarState,
    canGoBack: Boolean,
    destination: BottomTab,
    sectionTitle: String?,
    showBadge: Boolean,
    isLowDataModeEnabled: Boolean,
    showNewChatIcon: Boolean? = false,
    isPrivateMode: Boolean = false,
    inResponseScreen: Boolean = false,
    isUserLoggedIn: Boolean = false,
    showTooltip: Boolean = false,
    canShareChat: Boolean = false,
    showScreenShotToolTip: Boolean = false,
    hasConversationStarted: Boolean,
    actionClick: (TopBarActionItem) -> Unit,
) {
    val showLogo by remember(
        key1 = destination,
        key2 = sectionTitle,
    ) {
        mutableStateOf(destination == BottomTab.Home && sectionTitle == null)
    }

    val sectionTitleScale by animateFloatAsState(
        targetValue =
            if (state == TopBarState.EXPANDED) {
                1.0f
            } else {
                0.7f
            },
        label = "section title scale",
    )

    val sectionTitleTopPadding by animateDpAsState(
        targetValue =
            if (state == TopBarState.EXPANDED) {
                14.dp
            } else {
                3.dp
            },
        label = "section title scale",
    )

    AnimatedVisibility(
        visible = state != TopBarState.COLLAPSED && state != TopBarState.HIDE,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Box(
            modifier =
                Modifier
                    .background(color = wpdsColors.appBarBg)
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            Row(
                modifier =
                    Modifier
                        .wrapContentSize()
                        .align(Alignment.TopStart),
            ) {
                if (canGoBack) {
                    BackButton(actionClick)
                }

                val columnPadding =
                    when (state) {
                        TopBarState.TITLE_COLLAPSED -> 8.dp
                        else ->
                            if (showLogo) {
                                12.dp
                            } else {
                                8.dp
                            }
                    }

                Column(
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .padding(
                                start =
                                    if (canGoBack) {
                                        0.dp
                                    } else {
                                        16.dp
                                    },
                                top = columnPadding,
                                end = columnPadding,
                                bottom = columnPadding,
                            ),
                    horizontalAlignment = Alignment.Start,
                ) {
                    AnimatedVisibility(
                        visible = state != TopBarState.TITLE_COLLAPSED,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        if (showLogo) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier =
                                        Modifier
                                            .wrapContentWidth(),
                                    painter = painterResource(id = R.drawable.twp_logo),
                                    contentDescription = "Logo",
                                    tint = wpdsColors.appBarTitle,
                                )
                                if (AppContextUtils.isBetaBuild()) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "BETA",
                                        color = MaterialTheme.colorScheme.onPrimary, // Adjust based on theme
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .background(
                                                color = wpdsColors.appLogBetaStrip,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            if (destination != BottomTab.Ask) {
                                if (sectionTitle == null) {
                                    Text(
                                        modifier =
                                            Modifier
                                                .wrapContentSize(),
                                        text = destination.pageTitle ?: destination.title,
                                        style = Typography.h1,
                                        fontSize = 24.sp,
                                        color = wpdsColors.appBarTitle,
                                    )
                                } else {
                                    Text(
                                        modifier =
                                            Modifier
                                                .animateContentSize()
                                                .scale(sectionTitleScale)
                                                .wrapContentSize()
                                                .align(Alignment.Start),
                                        text = sectionTitle,
                                        style = Typography.h1,
                                        fontSize = 24.sp,
                                        color = wpdsColors.appBarTitle,
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.wrapContentWidth()
                                        .modifyIf(inResponseScreen, {
                                            this.offset(y = (-5).dp)
                                        }),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (inResponseScreen) {
                                        BackButton(
                                            onClickCallback = actionClick,
                                            modifier = Modifier.padding(end = 8.dp)
                                                .offset(x = (-5).dp)
                                        )
                                    }
                                    Icon(
                                        modifier =
                                            Modifier.size(width = 125.dp, height = 32.dp),
                                        painter = painterResource(id = com.washingtonpost.android.sections.R.drawable.atp_logo),
                                        contentDescription = "Logo",
                                        tint = Color.Unspecified,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .wrapContentSize()
                        .padding(end = 8.dp)
                        .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val showActionButtons by remember(
                    key1 = destination,
                    key2 = sectionTitle,
                ) {
                    mutableStateOf(
                        (destination == BottomTab.Print || sectionTitle == BottomTab.Print.title) || !(destination == BottomTab.Ask || destination == BottomTab.Listen || destination == BottomTab.Games || destination == BottomTab.Watch || sectionTitle?.isNotEmpty() == true),
                    )
                }

                val showSettings by remember(
                    key1 = destination,
                    key2 = sectionTitle,
                ) {
                    mutableStateOf(
                        (sectionTitle == BottomTab.Print.title) || !(destination == BottomTab.Home || sectionTitle?.isNotEmpty() == true),
                    )
                }

                val showNotifications by remember(
                    key1 = destination,
                    key2 = sectionTitle,
                ) {
                    mutableStateOf(
                        sectionTitle?.isNotEmpty() != true,
                    )
                }

                val showEllipsis by remember(
                    key1 = destination,
                ) {
                    mutableStateOf(
                        (destination == BottomTab.Ask),
                    )
                }

                if (showActionButtons) {
                    if (showSettings || (isLowDataModeEnabled && destination == BottomTab.Home)) {
                        SettingsActionButton(
                            onClick = { actionClick(TopBarActionItem.Settings) },
                            painterResourceId = com.washingtonpost.android.save.R.drawable.ic_settings,
                            contentDescription = "settings button",
                            tint = wpdsColors.appBarIcon,
                        )
                    }

                    SearchActionButton(
                        onClick = { actionClick(TopBarActionItem.Search) },
                        painterResourceId = com.wpds.wpds.R.drawable.search,
                        contentDescription = "search button",
                        tint = wpdsColors.appBarIcon,
                        size = 24,
                        showTooltip = showTooltip
                    )

                    if (showNotifications) {
                        SettingsActionButton(
                            onClick = { actionClick(TopBarActionItem.Alerts) },
                            painterResourceId =
                                if (showBadge) {
                                    R.drawable.ic_alert_bubble
                                } else {
                                    R.drawable.ic_alerts
                                },
                            contentDescription = "alerts button",
                            tint = Color.Unspecified,
                        )
                    }

                    if (isUserLoggedIn) {
                        SettingsActionButton(
                            onClick = { actionClick(TopBarActionItem.MyPost) },
                            painterResourceId = R.drawable.my_post_shortcut_icon,
                            contentDescription = "my post",
                            tint = Color.Unspecified,
                            size = 24
                        )
                    } else {
                        SettingsText(
                            onClick = { actionClick(TopBarActionItem.MyPost) },
                            text = stringResource(R.string.anonymous_sign_in)
                        )
                    }

                }
                if (showEllipsis) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .padding(0.dp)
                    ) {
                        Row {
                            if (showNewChatIcon == true && inResponseScreen) {
                                SettingsActionButton(
                                    onClick = { actionClick(TopBarActionItem.NewChat) },
                                    painterResourceId = R.drawable.ic_new_chat,
                                    contentDescription = "new chat button",
                                    tint = wpdsColors.gray0,
                                    size = 24
                                )
                            }
                            SettingsActionButton(
                                onClick = { actionClick(TopBarActionItem.History) },
                                painterResourceId = R.drawable.ic_time,
                                contentDescription = "history button",
                                tint = wpdsColors.gray0,
                                size = 24
                            )

                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    painterResource(R.drawable.ic_more_horizontal),
                                    contentDescription = "More options",
                                    tint = Color.Unspecified
                                )
                            }
                        }

                        ScreenCaptureToolTip(showScreenShotToolTip && isUserLoggedIn)

                        DropdownMenu(
                            modifier = Modifier.background(wpdsColors.appBarBg),
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            offset = DpOffset(x = (-16).dp, y = (6).dp)
                        ) {
                            if (isUserLoggedIn) {
                                DropdownMenuItem(
                                    onClick = {
                                        actionClick(TopBarActionItem.PrivateMode)
                                        expanded = false
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 0.dp
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = if (isPrivateMode) R.drawable.ic_disable_private_mode else R.drawable.ic_enable_private_mode),
                                            contentDescription = "Private chat icon",
                                            tint = wpdsColors.gray0
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = if (isPrivateMode) stringResource(R.string.private_mode_on_label) else stringResource(
                                                R.string.private_mode_off_label
                                            ),
                                            fontWeight = FontWeight.Light,
                                            color = wpdsColors.gray0,
                                            style = Typography.h2
                                        )
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = isPrivateMode,
                                            onCheckedChange = {
                                                actionClick(TopBarActionItem.PrivateMode)
                                                expanded = false
                                            },
                                            thumbContent = {
                                                Box(
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            },
                                            modifier = Modifier.scale(0.6f),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = wpdsColors.gray700,
                                                checkedTrackColor = wpdsColors.gray0,
                                                uncheckedThumbColor = wpdsColors.gray700,
                                                uncheckedTrackColor = wpdsColors.gray200
                                            )

                                        )
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.how_it_works_ask_the_post),
                                        fontWeight = FontWeight.Light,
                                        color = wpdsColors.gray0,
                                        style = Typography.h2
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_info),
                                        contentDescription = "Logo",
                                        tint = Color.Unspecified,
                                    )
                                },
                                onClick = {
                                    actionClick(TopBarActionItem.EllipsisHow)
                                    expanded = false
                                }
                            )
                            if (!isPrivateMode && canShareChat && isUserLoggedIn) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.share_ask_the_post),
                                            fontWeight = FontWeight.Light,
                                            color = wpdsColors.gray0,
                                            style = Typography.h2
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_atp_forward),
                                            contentDescription = "Forward",
                                            tint = wpdsColors.gray0,
                                        )
                                    },
                                    onClick = {
                                        actionClick(TopBarActionItem.Share)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.why_we_built_this),
                                        fontWeight = FontWeight.Light,
                                        color = wpdsColors.gray0,
                                        style = Typography.h2
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_forward),
                                        contentDescription = "Logo",
                                        tint = Color.Unspecified,
                                    )
                                },
                                onClick = {
                                    actionClick(TopBarActionItem.EllipsisWhy)
                                    expanded = false
                                }
                            )
                            if (hasConversationStarted) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.give_feedback),
                                            fontWeight = FontWeight.Light,
                                            color = wpdsColors.gray0,
                                            style = Typography.h2
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = com.wpds.wpds.R.drawable.comment),
                                            contentDescription = "Comment",
                                            tint = Color.Black,
                                        )
                                    },
                                    onClick = {
                                        actionClick(TopBarActionItem.GiveFeedback)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackButton(
    onClickCallback: (TopBarActionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier.wrapContentSize(),
        onClick = {
            onClickCallback(TopBarActionItem.Back)
        },
    ) {
        Icon(
            modifier = Modifier.wrapContentSize(),
            painter = painterResource(id = R.drawable.ic_back_arrow),
            contentDescription = "back arrow",
            tint = wpdsColors.appBarIcon,
        )
    }
}

@Composable
private fun SettingsText(
    onClick: () -> Unit,
    text: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .wrapContentWidth()
            .height(48.dp)
    ) {
        Text(
            text = text,
            color = wpdsColors.appBarIcon,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp)
                .wrapContentWidth(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsActionButton(
    onClick: () -> Unit,
    @DrawableRes painterResourceId: Int,
    contentDescription: String,
    tint: Color,
    size: Int = 18
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            modifier =
                Modifier
                    .height(size.dp)
                    .width(size.dp),
            painter = painterResource(id = painterResourceId),
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

@Composable
private fun NewChatActionButton(
    onClick: () -> Unit,
    @DrawableRes painterResourceId: Int,
    contentDescription: String,
    tint: Color,
    size: Int = 18
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            modifier =
                Modifier
                    .height(size.dp)
                    .width(size.dp),
            painter = painterResource(id = painterResourceId),
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

@Composable
private fun SearchActionButton(
    onClick: () -> Unit,
    @DrawableRes painterResourceId: Int,
    contentDescription: String,
    tint: Color,
    size: Int = 18,
    showTooltip: Boolean
) {
    AndroidView(
        modifier = Modifier.size(48.dp),
        factory = { context ->
            FrameLayout(context).apply {
                id = View.generateViewId()

                val composeView = ComposeView(context).apply {
                    setContent {
                        IconButton(
                            onClick = onClick,
                        ) {
                            Icon(
                                modifier =
                                    Modifier
                                        .height(size.dp)
                                        .width(size.dp),
                                painter = painterResource(id = painterResourceId),
                                contentDescription = contentDescription,
                                tint = tint,
                            )
                        }
                    }
                }
                addView(composeView)
            }
        },
        update = { view ->
            if (showTooltip) {
                view.post {
                    val tooltipData = TooltipData(
                        priorityData = TooltipPriority.SearchToolTipPriority(),
                        sharedPrefKey = TooltipPriority.SearchToolTipPriority().prefKey,
                        tooltipTextResourceId = R.string.search_new_location_tooltip,
                        boldPortionResourceId = R.string.search_new_location_tooltip_bold,
                        gravity = Gravity.BOTTOM,
                        duration = 10_000L,
                        verticalMargin = 24,
                    )

                    val content = getSpannableStringWithBoldText(
                        view.context,
                        tooltipData.tooltipTextResourceId,
                        tooltipData.boldPortionResourceId,
                    )

                    val tooltipProperties = TooltipProperties(
                        content,
                        view,
                        null,
                        tooltipData,
                    )

                    TooltipPopupManager.get(view.context).showToolTip(tooltipProperties)
                }
            }
        }
    )
}

@Composable
fun ScreenCaptureToolTip(
    showTooltip: Boolean
) {
    AndroidView(
        modifier = Modifier.size(24.dp),
        factory = { context ->
            FrameLayout(context).apply {
                val composeView = ComposeView(context).apply {
                    setContent {
                        //Invisible anchor view.
                    }
                }
                addView(composeView)
            }

        },
        update = { view ->
            if (showTooltip) {
                view.post {
                    val tooltipData = TooltipData(
                        priorityData = TooltipPriority.AskToolTipPriority(),
                        sharedPrefKey = TooltipPriority.AskToolTipPriority().prefKey,
                        tooltipTextResourceId = R.string.ask_screenshot_tooltip,
                        boldPortionResourceId = R.string.ask_screenshot_tooltip_bold,
                        gravity = Gravity.BOTTOM,
                        duration = 10_000L,
                        verticalMargin = 24,
                        hideArrow = true
                    )

                    val content = getSpannableStringWithBoldText(
                        view.context,
                        tooltipData.tooltipTextResourceId,
                        tooltipData.boldPortionResourceId,
                    )

                    val tooltipProperties = TooltipProperties(
                        content,
                        view,
                        null,
                        tooltipData,
                    )

                    TooltipPopupManager.get(view.context).showToolTip(tooltipProperties)
                }
            }
        }
    )
}

// Previews

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Expanded_CanGoBack_NoSectionTitle() {
    TopAppBar(
        state = TopBarState.EXPANDED,
        canGoBack = true,
        destination = BottomTab.Home,
        sectionTitle = null,
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Expanded_CantGoBack_NoSectionTitle() {
    TopAppBar(
        state = TopBarState.EXPANDED,
        canGoBack = false,
        destination = BottomTab.Home,
        sectionTitle = null,
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Collapsed_CanGoBack_DestinationFind() {
    TopAppBar(
        state = TopBarState.EXPANDED,
        canGoBack = true,
        destination = BottomTab.Ask,
        sectionTitle = "",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Collapsed_CantGoBack_DestinationFind() {
    TopAppBar(
        state = TopBarState.EXPANDED,
        canGoBack = false,
        destination = BottomTab.Ask,
        sectionTitle = "",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Expanded_CanGoBack() {
    TopAppBar(
        state = TopBarState.EXPANDED,
        canGoBack = true,
        destination = BottomTab.Home,
        sectionTitle = "Some long title",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Expanded_CantGoBack_Destination_Find() {
    TopAppBar(
        state = TopBarState.EXPANDED,
        canGoBack = false,
        destination = BottomTab.Ask,
        sectionTitle = "Some long title",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_TitleExpanded_CanGoBack_Destination_Find() {
    TopAppBar(
        state = TopBarState.EXPANDED,
        canGoBack = true,
        destination = BottomTab.Ask,
        sectionTitle = "Some long title",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_TitleCollapsed_CanGoBack() {
    TopAppBar(
        state = TopBarState.TITLE_COLLAPSED,
        canGoBack = true,
        destination = BottomTab.Home,
        sectionTitle = "Some long title",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_TitleCollapsed_CantGoBack() {
    TopAppBar(
        state = TopBarState.TITLE_COLLAPSED,
        canGoBack = false,
        destination = BottomTab.Home,
        sectionTitle = "Some long title",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Collapsed_CanGoBack() {
    TopAppBar(
        state = TopBarState.COLLAPSED,
        canGoBack = true,
        destination = BottomTab.Home,
        sectionTitle = "Some long title",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun TopAppBar_Collapsed_CantGoBack() {
    TopAppBar(
        state = TopBarState.COLLAPSED,
        canGoBack = false,
        destination = BottomTab.Home,
        sectionTitle = "Some long title",
        showBadge = false,
        isLowDataModeEnabled = false,
        hasConversationStarted = false,
        actionClick = {},
    )
}
