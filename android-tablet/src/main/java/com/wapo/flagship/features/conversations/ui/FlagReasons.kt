package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wpds.theme.wpdsColors
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentFlagReason
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem


private val FLAG_REASONS = listOf(
    CommentFlagReason.OFFENSIVE,
    CommentFlagReason.ABUSIVE,
    CommentFlagReason.MISINFORMATION,
    CommentFlagReason.SPAM
)

/**
 * This composable displays a dialog that allows the user to report a comment.
 * It presents a list of reasons for reporting, and when the user selects one, it
 * triggers an action to flag the comment and dismisses the dialog.
 */
@Composable
fun FlagReportBottomSheet(
    comment: CommentItem,
    onAction: (CommentAction) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf<CommentFlagReason?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Why are you flagging this?") },
        text = {
            Column {
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
                            onClick = { selectedReason = reason }
                        )
                        Text(text = reason.message, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedReason?.let {
                        onAction(CommentAction.CommentFlag(comment, it))
                        onDismiss()
                    }
                },
                enabled = selectedReason != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = wpdsColors.cta,
                    contentColor = wpdsColors.onCta
                )
            ) {
                Text("Flag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}