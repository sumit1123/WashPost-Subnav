package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

/**
 * A row of overlapping reactions.
 *
 * @param reactions The list of reactions to display.
 * @param modifier A modifier to be applied to the row.
 * @param iconSize The size of the reaction icons.
 * @param overlapWidth The width of the overlap between the reaction icons.
 * @param onClick A callback to be invoked when the user clicks on the reactions.
 */
@Composable
fun OverlappingReactions(
    reactions: List<CommentReactionType>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    overlapWidth: Dp = 10.dp,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(-overlapWidth)
    ) {
        reactions.forEach { reaction ->
            val emoji = when (reaction) {
                CommentReactionType.HELPFUL -> R.drawable.sentiment_emoji_helpful
                CommentReactionType.CARE -> R.drawable.sentiment_emoji_care
                CommentReactionType.SURPRISING -> R.drawable.sentiment_emoji_surprised
                CommentReactionType.FUNNY -> R.drawable.sentiment_emoji_funny
                CommentReactionType.FRUSTRATING -> R.drawable.sentiment_emoji_frustrated
            }

            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(wpdsColors.gray600)
                    .border(
                        width = 1.dp,
                        color = wpdsColors.gray0,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = emoji),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}
