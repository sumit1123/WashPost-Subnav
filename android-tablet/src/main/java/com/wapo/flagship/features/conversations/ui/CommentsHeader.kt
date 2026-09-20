package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.washingtonpost.android.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.wapo.android.commons.util.truncatedString
import com.wapo.flagship.features.nightmode.NightModeController
import com.wpds.theme.wpdsColors
import com.wpds.theme.wpdsColorsDark
import com.wpds.theme.wpdsColorsLight

/**
 * This composable displays the header for the comments section. It shows the total
 * number of comments and a "My Profile" button that the user can tap to view their
 * own comments.
 */
@Composable
fun CommentsHeader(
    commentCount: Int,
    onMyProfileClick: (() -> Unit)? = null
) {
    val useDarkTheme =
        (LocalContext.current.applicationContext as? NightModeController)?.isNightModeEnabled()
        ?: isSystemInDarkTheme()
    val textColor = if (useDarkTheme) wpdsColorsDark.onSurface else wpdsColorsLight.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(
                        pluralStringResource(
                            id = R.plurals.comments_count,
                            count = commentCount,
                            commentCount.truncatedString() ?: "0"
                        )
                    )
                }
            },
            color = textColor
        )

        if (onMyProfileClick != null) {
            Surface(
                onClick = onMyProfileClick,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, wpdsColors.outline),
                color = wpdsColors.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.my_post_shortcut_icon),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.labelMedium,
                        color = wpdsColors.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
