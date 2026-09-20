package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentFlagReason
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wpds.theme.wpdsColors

private val FLAG_REASONS = listOf(
    CommentFlagReason.OFFENSIVE,
    CommentFlagReason.ABUSIVE,
    CommentFlagReason.MISINFORMATION,
    CommentFlagReason.SPAM
)

@Composable
fun FlagReportScreen(
    comment: CommentItem,
    onBack: () -> Unit,
    onAction: (CommentAction) -> Unit
) {
    var selectedReason by remember { mutableStateOf<CommentFlagReason?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Action bar (top bar with back and close buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(contentColor = wpdsColors.onSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(contentColor = wpdsColors.onSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }

        HorizontalDivider()

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Report this comment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = wpdsColors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )
            Text(
                text = "Why are you reporting this comment?",
                style = MaterialTheme.typography.bodyLarge,
                color = wpdsColors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Start
            )

            FLAG_REASONS.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReason = reason }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = wpdsColors.cta,
                            unselectedColor = wpdsColors.onSurfaceSubtle
                        )
                    )
                    Text(
                        text = reason.message,
                        color = wpdsColors.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Button(
                onClick = {
                    selectedReason?.let {
                        onAction(CommentAction.CommentFlag(comment, it))
                    }
                    onBack()
                },
                enabled = selectedReason != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = wpdsColors.cta,
                    contentColor = wpdsColors.onCta,
                    disabledContainerColor = wpdsColors.onSurfaceSubtle.copy(alpha = 0.38f),
                    disabledContentColor = wpdsColors.onSurface.copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Submit")
            }
        }
    }
}
