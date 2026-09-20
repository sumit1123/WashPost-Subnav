package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentSummaryResponse
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

/**
 * This composable displays a card containing a summary of the conversation.
 * The summary is initially truncated to three lines, and a "Show more" button
 * is displayed if the text is longer. The user can tap this button to expand
 * the card and view the full summary.
 */
@Composable
fun SummaryCard(summary: CommentSummaryResponse?) {
    if (summary?.answer.isNullOrBlank()) return

    var expanded by remember { mutableStateOf(false) }
    var isTextTruncated by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = wpdsColors.alpha400
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = com.wpds.wpds.R.drawable.ai_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = stringResource(id = R.string.conversation_summary_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = wpdsColors.onSurface,
                    fontSize = 17.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = summary?.answer ?: "",
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                color = wpdsColors.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.W400,
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 3) {
                        isTextTruncated = true
                    }
                }
            )


            if (isTextTruncated) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (expanded) stringResource(id = R.string.conversation_summary_show_less)
                           else stringResource(id = R.string.conversation_summary_show_more),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = wpdsColors.onSurfaceSubtle,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = !expanded }
                )
            }
        }
    }
}
