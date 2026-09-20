package com.wapo.flagship.wapomain.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wapo.flagship.features.backendhealth.FallbackScreen
import com.wapo.flagship.features.backendhealth.models.FailoverState
import com.wapo.flagship.features.lowdata.LowDataModeNotificationConfigurationImpl
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.Companion.DISMISS_START_COUNTER
import com.wapo.flagship.features.lowdata.LowDataModeNotificationImpl.LowDataModeNotificationState
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import com.wapo.flagship.features.main.ui.BottomSheetPrompt
import com.wapo.flagship.features.onboarding2.composable.NotificationComposable
import com.wapo.flagship.features.onboarding2.composable.PushAlertPromptEvent
import com.wapo.flagship.features.search2.events.ConversationItem
import com.wapo.flagship.navigation.ui.BottomNavigationBar
import com.wapo.flagship.navigation.ui.BottomTab
import com.wapo.flagship.navigation.ui.BottomTabNavigation
import com.wapo.flagship.navigation.ui.FloatingBottomBarsContainer
import com.wapo.flagship.navigation.ui.TopAppBar
import com.wapo.flagship.navigation.ui.TopBarState
import com.wapo.flagship.snackbars.model.SnackBarEvent
import com.wapo.flagship.snackbars.model.SnackBarType
import com.wapo.flagship.snackbars.ui.AddToPlaylistSnackbar
import com.wapo.flagship.views.LowDataModeDismissDialog
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainActivityUI(
    state: MainActivityUIState,
    navController: NavHostController,
    onUIEvent: (MainActivityUIEvent) -> Unit,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val signInPrompt = state.bottomBarsState.bottomSheetPrompt
    val sheetValue = remember { mutableStateOf(SheetValue.Hidden) }

    val nestedScrollConnection =
        remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    onUIEvent(
                        MainActivityUIEvent.DetermineTopBarState(
                            consumed.y,
                            state.topAppBarState.sectionTitle != null
                        )
                    )
                    return super.onPostScroll(consumed, available, source)
                }
            }
        }
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
            confirmValueChange = { newValue ->
                if (newValue == SheetValue.Hidden || newValue == SheetValue.PartiallyExpanded && sheetValue.value != newValue) {
                    signInPrompt?.let {
                        onUIEvent.invoke(MainActivityUIEvent.BottomPromptDismissed(it))
                    }
                }
                //Prevent the bottom sheet from firing dismiss event multiple times
                sheetValue.value = newValue
                true
            }
        )
    )
    val showSignInPrompt by remember(signInPrompt) {
        mutableStateOf(signInPrompt != null)
    }

    LaunchedEffect(showSignInPrompt) {
        if (showSignInPrompt) {
            bottomSheetScaffoldState.bottomSheetState.expand()
        } else {
            bottomSheetScaffoldState.bottomSheetState.hide()
        }
    }

    BottomSheetScaffold(
        sheetDragHandle = null,
        scaffoldState = bottomSheetScaffoldState,
        sheetMaxWidth = Dp.Unspecified,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            signInPrompt?.let { message ->
                BottomSheetPrompt(
                    message,
                    onButtonClicked = {
                        onUIEvent(
                            MainActivityUIEvent.BottomPromptActionClicked(message)
                        )
                    },
                    onDismiss = {
                        scope.launch {
                            bottomSheetScaffoldState.bottomSheetState.hide()
                        }
                    })
            }
        },
        sheetTonalElevation = 24.dp,
        sheetShadowElevation = 24.dp,
        sheetPeekHeight = 0.dp,
        containerColor = wpdsColors.appBarBg,
        sheetContainerColor = wpdsColors.appBarBg,
    ) { innerPadding ->
        Scaffold(
            modifier = Modifier.Companion.nestedScroll(nestedScrollConnection),
            bottomBar = {
                val bottomTabState = state.bottomTabState
                BottomNavigationBar(
                    isVisible = bottomTabState.isVisibleBottomNav,
                    topBarState = state.appBarState ?: TopBarState.EXPANDED,
                    tabs = bottomTabState.bottomNavTabs,
                    currentTab = bottomTabState.currentTab,
                    navTo = { tab ->
                        onUIEvent(
                            MainActivityUIEvent.SwitchBottomTab(tab)
                        )
                    },
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState) {
                    Column {
                        when (state.snackbar) {
                            is SnackBarType.AddedToPlaylist ->
                                AddToPlaylistSnackbar {
                                    when (it) {
                                        SnackBarEvent.Dismiss -> {
                                            onUIEvent(
                                                MainActivityUIEvent.SnackBarDismiss
                                            )
                                        }

                                        SnackBarEvent.OpenListenToThePost -> {
                                            onUIEvent(
                                                MainActivityUIEvent.OpenListenToThePost
                                            )
                                        }

                                        else -> {}
                                    }
                                }

                            else -> {}
                        }
                    }
                }
            },
            containerColor = wpdsColors.appBarBg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val topAppBarState = state.topAppBarState
                TopAppBar(
                    state = state.appBarState ?: TopBarState.EXPANDED,
                    canGoBack = topAppBarState.showBackButton ?: false,
                    destination = state.destination,
                    sectionTitle = topAppBarState.sectionTitle,
                    showBadge = topAppBarState.showAlertBadge ?: false,
                    showNewChatIcon = topAppBarState.conversationHistory.isNotEmpty(),
                    isLowDataModeEnabled = state.lowDataBannerState?.isLowDataBannerEnable ?: false,
                    isPrivateMode = topAppBarState.toolBarState.isPrivateMode,
                    inResponseScreen = topAppBarState.toolBarState.inResponseScreen,
                    isUserLoggedIn = topAppBarState.toolBarState.isUserLoggedIn,
                    showTooltip = topAppBarState.toolBarState.showTooltip,
                    canShareChat = topAppBarState.toolBarState.canShareChat,
                    showScreenShotToolTip = topAppBarState.toolBarState.showScreenShotToolTip,
                    hasConversationStarted = topAppBarState.conversationHistory.lastOrNull() is ConversationItem.Sources
                ) { action ->
                    onUIEvent(
                        MainActivityUIEvent.HandleTopBarAction(action)
                    )
                }

                Box(modifier = Modifier.Companion.fillMaxSize()) {
                    BottomTabNavigation(
                        navController,
                        state.bottomTabState.bottomTabContainerId
                    ) {
                        onUIEvent(
                            MainActivityUIEvent.OnBackPressed
                        )
                    }

                    // This flag is to avoid renderer problems on the preview,
                    // We are not able to preview this UI due to a mix with a not composable Fragment
                    val isPreview = LocalInspectionMode.current
                    if (!isPreview) {
                        FloatingBottomBarsContainer(
                            isPlayerShown = state.bottomBarsState.shouldShowPersistentPlayer == true,
                            isNoConnectionShown = state.snackbar is SnackBarType.NoNetworkConnection,
                            isTurnOnLowDataModeShown = state.snackbar is SnackBarType.LowDataConnection && !state.isLowDataModeEnable,
                            appBarState = state.appBarState ?: TopBarState.EXPANDED,
                            lowDataModeNotification = state.lowDataModeNotificationConfig,
                        ) {
                            when (it) {
                                SnackBarEvent.OpenSettings -> {
                                    onUIEvent(
                                        MainActivityUIEvent.SnackBarOpenSettings
                                    )
                                }

                                SnackBarEvent.TurnOnLowDataMode -> {
                                    onUIEvent(
                                        MainActivityUIEvent.SnackBarTurnOnLowDataMode
                                    )
                                }

                                SnackBarEvent.Dismiss, SnackBarEvent.DismissLowDataMode -> {
                                    onUIEvent(
                                        MainActivityUIEvent.SnackBarDismiss
                                    )
                                }

                                else -> {}
                            }
                        }
                    }

                    val fallbackState = state.fallbackState
                    FallbackScreen(
                        state = fallbackState.failoverState,
                        onRetry = {
                            onUIEvent(MainActivityUIEvent.FallbackRetry)
                        },
                        onCallSite = {
                            fallbackState.failoverState.fallbackURL?.let {
                                onUIEvent(MainActivityUIEvent.FallbackCallSite(it))
                            }
                        },
                        onArticleClicked = {
                            onUIEvent(
                                MainActivityUIEvent.FallbackArticleClicked(
                                    it.contentUrl
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    if (state.lowDataModeNotificationConfig?.isLowDataModeDialogVisible() == true) {
        onUIEvent(MainActivityUIEvent.LowDataModeModalSeen)
        LowDataModeDismissDialog(
            state.lowDataModeNotificationConfig,
            {
                onUIEvent(MainActivityUIEvent.LowDataModeAllow)
            },
            {
                onUIEvent(MainActivityUIEvent.LowDataModeDismiss)
            },
            {
                onUIEvent(MainActivityUIEvent.LowDataModeSnooze)
            },
        )
    }

    if (state.appResumeCount == 2L) {
        NotificationComposable(
            stringResource(R.string.push_prompt_title),
            stringResource(R.string.push_prompt_description),
            R.drawable.alert_bell,
            state.isNightModeEnable
        ) { event ->
            when (event) {
                is PushAlertPromptEvent.OnNotificationIconClick -> {
                    onUIEvent(
                        MainActivityUIEvent.OnNotificationIconClick(
                            event.response
                        )
                    )
                }
            }
        }
    }

    // Handle snackbar display
    LaunchedEffect(key1 = state.snackbar) {
        if (state.snackbar != null) {
            val result =
                snackBarHostState.showSnackbar(
                    "",
                    duration = if (state.snackbar.persistent) SnackbarDuration.Indefinite else SnackbarDuration.Short,
                )

            when (result) {
                SnackbarResult.Dismissed -> {
                    onUIEvent(MainActivityUIEvent.SnackBarResultDismissed)
                }

                SnackbarResult.ActionPerformed -> {
                }
            }
        } else {
            snackBarHostState.currentSnackbarData?.dismiss()
        }
    }
}

@Preview
@Composable
fun MainActivityUIPreview() {
    AndroidClassicTheme {
        MainActivityUI(
            state = MainActivityUIState(
                appBarState = TopBarState.EXPANDED,
                destination = BottomTab.Home,
                snackbar = SnackBarType.LowDataConnection(),
                lowDataModeNotificationConfig = LowDataModeNotificationConfigurationImpl(
                    notificationState = LowDataModeNotificationState.Allow,
                    dismissCounter = DISMISS_START_COUNTER
                ),
                lowDataBannerState = LowDataBanner(true),
                isLowDataModeEnable = true,
                isNightModeEnable = false,
                isAdFree = false,
                appResumeCount = 0,
                bottomTabState = MainActivityBottomTabState(
                    bottomTabContainerId = R.id.bottom_tabs_main_container,
                    isVisibleBottomNav = true,
                    bottomNavTabs = listOf(
                        BottomTab.Home, BottomTab.Ask, BottomTab.Games,
                        BottomTab.Print
                    ),
                    currentTab = BottomTab.Home
                ),
                topAppBarState = MainActivityTopAppBarState(
                    toolBarState = ToolBarUIState(
                        isPrivateMode = true,
                        inResponseScreen = true,
                        isUserLoggedIn = true,
                        showTooltip = true,
                        canShareChat = true,
                        showScreenShotToolTip = true
                    ),
                    showBackButton = true,
                    showAlertBadge = true,
                    sectionTitle = "sectionTitle",
                    conversationHistory = listOf()
                ),
                bottomBarsState = MainActivityBottomBarsState(
                    shouldShowPersistentPlayer = true
                ),
                fallbackState = MainActivityFallbackState(
                    failoverState = FailoverState(),
                )
            ),
            navController = rememberNavController(),
            onUIEvent = {}
        )
    }
}