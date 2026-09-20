package com.wapo.flagship.features.conversations.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentDomainActionCounts
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentReactionType
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

private val SENTIMENT_REACTIONS = listOf(
    R.drawable.sentiment_emoji_care        to CommentReactionType.CARE,
    R.drawable.sentiment_emoji_frustrated  to CommentReactionType.FRUSTRATING,
    R.drawable.sentiment_emoji_funny       to CommentReactionType.FUNNY,
    R.drawable.sentiment_emoji_helpful     to CommentReactionType.HELPFUL,
    R.drawable.sentiment_emoji_surprised   to CommentReactionType.SURPRISING
)

/**
 * A popup that allows the user to pick a reaction.
 *
 * @param selectedReaction The currently selected reaction.
 * @param actionCounts The counts for each reaction type.
 * @param onSelect A callback to be invoked when the user selects a reaction.
 * @param onDismiss A callback to be invoked when the popup is dismissed.
 */
@Composable
fun ReactionPicker(
    selectedReaction: CommentReactionType? = null,
    actionCounts: CommentDomainActionCounts? = null,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Row(
            modifier = Modifier
                .background(wpdsColors.surface, RoundedCornerShape(24.dp))
                .border(1.dp, wpdsColors.outline, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SENTIMENT_REACTIONS.forEachIndexed { index, (drawableRes, reactionType) ->
                val isSelected = selectedReaction == reactionType
                val reactionLabel = reactionType.value.lowercase().replaceFirstChar { it.uppercase() }
                val count = when (reactionType) {
                    CommentReactionType.CARE -> actionCounts?.care ?: 0
                    CommentReactionType.FRUSTRATING -> actionCounts?.frustrating ?: 0
                    CommentReactionType.FUNNY -> actionCounts?.funny ?: 0
                    CommentReactionType.HELPFUL -> actionCounts?.helpful ?: 0
                    CommentReactionType.SURPRISING -> actionCounts?.surprising ?: 0
                }

                val rotation by rememberInfiniteTransition(label = "shake_${reactionType.value}").animateFloat(
                    initialValue = -3f,
                    targetValue = 3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 120 + (index * 15), easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rotation"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Select $reactionLabel reaction"
                        ) { onSelect(reactionType.value) }
                        .semantics {
                            contentDescription = reactionLabel
                            role = Role.Button
                        }
                        .padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isSelected) wpdsColors.blue100Static.copy(alpha = 0.15f)
                                else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) wpdsColors.blue100Static else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(drawableRes),
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reactionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isSelected) wpdsColors.blue100Static else wpdsColors.onSurfaceSubtle
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = wpdsColors.onSurface
                    )
                }
            }
        }
    }
}
