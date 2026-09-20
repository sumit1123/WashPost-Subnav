package com.wapo.flagship.navigation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.lowdata.LowDataModeNotificationConfigurationImpl
import com.wapo.flagship.snackbars.model.SnackBarEvent
import com.wapo.flagship.snackbars.ui.NoConnectionSnackbar
import com.wapo.flagship.snackbars.ui.TurnOnLowDataModeSnackbar
import com.wpds.theme.wpdsColors

@Composable
fun BoxScope.FloatingBottomBarsContainer(
    isPlayerShown: Boolean,
    isNoConnectionShown: Boolean,
    isTurnOnLowDataModeShown: Boolean,
    appBarState: TopBarState,
    lowDataModeNotification: LowDataModeNotificationConfigurationImpl?,
    onClick: (SnackBarEvent) -> Unit = {},
) {
    val animateOffset: Int by animateIntAsState(
        if (appBarState == TopBarState.COLLAPSED) 0 else -56,
        label = "",
    )

    Column(
        modifier =
        Modifier
            .offset(y = animateOffset.dp)
            .align(Alignment.BottomCenter),
    ) {
        PersistentPlayerContainer(isPlayerShown)
        if (isPlayerShown && appBarState == TopBarState.EXPANDED) {
            Box(modifier = Modifier.background(wpdsColors.appBarBg)) {
                HorizontalDivider(
                    modifier = Modifier.widthIn(max = 580.dp),
                    thickness = 1.dp,
                    color = wpdsColors.gray400
                )
            }
        }
        AnimatedVisibility(visible = isNoConnectionShown || isTurnOnLowDataModeShown) {
            if (isNoConnectionShown) {
                NoConnectionSnackbar(onClick)
            }
            if (isTurnOnLowDataModeShown && lowDataModeNotification?.isLowDataModeNotificationEnable() == true) {
                TurnOnLowDataModeSnackbar(onClick)
            }
        }
    }
}
