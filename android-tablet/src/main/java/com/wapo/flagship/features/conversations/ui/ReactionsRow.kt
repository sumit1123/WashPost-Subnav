package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSentimentType
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

/**
 * This composable displays a row of reactions for a given comment. It includes
 * up- and down-vote buttons, as well as a summary of other reactions.
 * The user can tap the reaction summary to open a picker and add their own reaction.
 */
@Composable
fun ReactionsRow(
    comment: CommentItem,
    isUserLoggedIn: Boolean = true,
    onAction: (CommentAction) -> Unit
) {
    var showReactionPicker by remember { mutableStateOf(false) }

    val sentiment = comment.viewerAction.sentiment
    val isUpLiked = sentiment == CommentSentimentType.UPVOTE
    val isDownLiked = sentiment == CommentSentimentType.DOWNVOTE

    val actionsCounts = comment.actionCounts
    val totalReactions = actionsCounts.reactionsTotal()

    val myReaction: CommentReactionType? = comment.viewerAction.reaction

    Row(verticalAlignment = Alignment.CenterVertically) {

        val isDarkTheme = isSystemInDarkTheme()
        val selectedIndicatorColor = if (isDarkTheme) Color.White else Color.Black

        val netVotes = actionsCounts.likeNet()
        Row(verticalAlignment = Alignment.CenterVertically) {
            val upArrowRes = when {
                isUpLiked && isDarkTheme -> R.drawable.arrow_up_dark_selected
                isUpLiked -> R.drawable.arrow_up_light_selected
                isDarkTheme -> R.drawable.arrow_up_dark
                else -> R.drawable.arrow_up_light
            }
            val downArrowRes = when {
                isDownLiked && isDarkTheme -> R.drawable.arrow_down_dark_selected
                isDownLiked -> R.drawable.arrow_down_light_selected
                isDarkTheme -> R.drawable.arrow_down_dark
                else -> R.drawable.arrow_down_light
            }
            Icon(
                painter = painterResource(upArrowRes),
                contentDescription = "Like",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(enabled = isUserLoggedIn) { onAction(CommentAction.UpVote(comment, !isUpLiked)) }
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = netVotes.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = selectedIndicatorColor
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(downArrowRes),
                contentDescription = "Dislike",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(enabled = isUserLoggedIn) { onAction(CommentAction.DownVote(comment, !isDownLiked)) }
            )
        }

        Spacer(Modifier.width(12.dp))

        val reactions = remember(actionsCounts) {
            listOf(
                CommentReactionType.HELPFUL to actionsCounts.helpful,
                CommentReactionType.CARE to actionsCounts.care,
                CommentReactionType.SURPRISING to actionsCounts.surprising,
                CommentReactionType.FUNNY to actionsCounts.funny,
                CommentReactionType.FRUSTRATING to actionsCounts.frustrating,
            )
                // This is to take to 3 emoji's and ignore the rest
                .filter { (_, count) -> count > 0 }
                .sortedByDescending { (_, count) -> count }
                .take(3)
                .map { (reactionType, _) -> reactionType }
        }

        if (reactions.isNotEmpty()) {
            OverlappingReactions(
                reactions = reactions,
                onClick = { if (isUserLoggedIn) showReactionPicker = true }
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Face,
                contentDescription = "Add Reaction",
                modifier = Modifier.size(18.dp).clickable(enabled = isUserLoggedIn) { showReactionPicker = true },
                tint = if (myReaction != null) selectedIndicatorColor else wpdsColors.gray100
            )
        }

        if (totalReactions > 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                totalReactions.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = wpdsColors.onSurface
            )
        }
    }

    if (showReactionPicker) {
        ReactionPicker(
            selectedReaction = myReaction,
            actionCounts = actionsCounts,
            onSelect = {
                showReactionPicker = false
                onAction(CommentAction.ReactionSelected(comment, it))
            },
            onDismiss = { showReactionPicker = false }
        )
    }
}
