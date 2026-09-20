package com.wapo.flagship.features.articles3.views

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.FollowState
import com.wapo.flagship.features.articles2.models.InlineTopicFollowItem
import com.wapo.flagship.features.articles3.models.ui.InlineAlertToggleUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

@Composable
fun InlineAlertToggleView(
    uiModel: InlineAlertToggleUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    val context = LocalContext.current
    val initialEnabled = uiModel.isEnabled
    var isEnabled by remember { mutableStateOf(initialEnabled) }

    Column {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(wpdsColors.gray400)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    if (isEnabled) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(uiModel.topicDisplayName)
                        }
                        append(" alerts are now on")
                    } else {
                        append("Turn on ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(uiModel.topicDisplayName)
                        }
                        append(" alerts")
                    }
                },
                color = wpdsColors.primary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isEnabled,
                onCheckedChange = { newStatus: Boolean ->
                    if (newStatus && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                        showNotificationsBlockedDialog(context)
                    } else {
                        isEnabled = newStatus
                        articlesInteractionHelper.onEventFired(
                            ArticleInteractionEvent.AlertToggled(uiModel.topicKey, uiModel.topicDisplayName, newStatus)
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = wpdsColors.toggleCheckedThumb,
                    checkedTrackColor = wpdsColors.toggleCheckedTrack,
                    uncheckedThumbColor = wpdsColors.toggleUncheckedThumb,
                    uncheckedTrackColor = wpdsColors.toggleUncheckedTrack
                )
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(wpdsColors.gray400)
        )
    }
}

private fun showNotificationsBlockedDialog(context: Context) {
    AlertDialog.Builder(context).create().apply {
        setTitle(context.resources.getString(com.washingtonpost.android.notifications.R.string.notifications_blocked_title))
        setMessage(context.resources.getString(com.washingtonpost.android.notifications.R.string.notifications_blocked_message))
        setButton(
            AlertDialog.BUTTON_NEUTRAL,
            context.resources.getString(com.washingtonpost.android.notifications.R.string.go_settings_message),
        ) { _, _ ->
            launchSystemAppSettings(context)
        }
        setButton(
            AlertDialog.BUTTON_NEGATIVE,
            context.resources.getString(com.washingtonpost.android.notifications.R.string.cancelLabel),
        ) { _, _ ->
            cancel()
        }
        show()
    }
}

private fun launchSystemAppSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    } else {
        context.startActivity(
            Intent()
                .setClassName(
                    "com.android.settings",
                    "com.android.settings.Settings\$AppNotificationSettingsActivity",
                ).putExtra("app_package", context.packageName)
                .putExtra("app_uid", context.applicationInfo.uid)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
        )
    }
}

enum class InlineAlertToggleUiStyle {
    DEFAULT
}

@Preview
@Composable
private fun InlineAlertToggleViewOn() {
    AndroidClassicTheme {
        Surface(modifier = Modifier.background(wpdsColors.secondary)) {
            InlineAlertToggleView(
                uiModel = InlineAlertToggleUiModel(
                    topicKey = "some-topic",
                    topicDisplayName = "Some Topic",
                    isEnabled = true,
                    uiStyle = InlineAlertToggleUiStyle.DEFAULT
                ),
                articlesInteractionHelper = dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview
@Composable
private fun InlineAlertToggleViewOff() {
    AndroidClassicTheme {
        Surface(modifier = Modifier.background(wpdsColors.secondary)) {
            InlineAlertToggleView(
                uiModel = InlineAlertToggleUiModel(
                    topicKey = "some-topic",
                    topicDisplayName = "Some Topic",
                    isEnabled = false,
                    uiStyle = InlineAlertToggleUiStyle.DEFAULT
                ),
                articlesInteractionHelper = dummyArticlesInteractionHelper
            )
        }
    }
}
