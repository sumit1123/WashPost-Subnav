package com.wapo.flagship.features.conversations.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.wpds.theme.wpdsColorsLight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.washingtonpost.android.R

/**
 * This composable displays a floating action button that allows the user to post a comment.
 * The button's text is animated to expand and collapse based on whether the user is scrolling.
 */
@Composable
fun CommentButton(
    onClick: () -> Unit,
    isScrolling: Boolean = false,
    text: String = "Comment",
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.border(1.dp, wpdsColorsLight.outline, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        containerColor = wpdsColorsLight.surface,
        contentColor = wpdsColorsLight.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = !isScrolling,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row {
                    Text(text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                }
            }
            Icon(painterResource(id = R.drawable.ic_comment), contentDescription = "Comment")
        }
    }
}