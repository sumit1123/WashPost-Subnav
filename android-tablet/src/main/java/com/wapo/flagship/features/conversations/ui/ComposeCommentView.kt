package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.wpds.theme.wpdsColors
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.wapo.flagship.features.conversations.model.CommentAction
import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.ContextSummaryResponse

/**
 * A view that allows the user to compose a comment.
 *
 * @param articleTitle The title of the article being commented on.
 * @param parentComment The parent comment, if the user is replying to a comment.
 * @param contextSummary The summary of the context.
 * @param onBack A callback to be invoked when the user taps the back button.
 * @param onClose A callback to be invoked when the user taps the close button.
 * @param onAction A callback to be invoked when the user performs an action.
 */
@Composable
fun ComposeCommentView(
    articleTitle: String,
    parentComment: CommentItem?,
    contextSummary: ContextSummaryResponse?,
    communityGuidelinesUrl: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onAction: (CommentAction) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val minChar = contextSummary?.data?.settings?.charCount?.min ?: 1
    val maxChar = contextSummary?.data?.settings?.charCount?.max ?: 2000
    val commentsDisabled = contextSummary?.data?.stream?.commentsDisabled ?: false
    val isUnderMin = text.isNotEmpty() && text.length < minChar
    val isOverMax = text.length > maxChar
    val isValid = text.isNotBlank() && !isUnderMin && !isOverMax && !commentsDisabled

    Column(Modifier.fillMaxSize()) {
        // Action bar: Back (left), title area (center), Close (right)
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
            Text(
                text = "Comment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = wpdsColors.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(contentColor = wpdsColors.onSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
        HorizontalDivider()

        // Article title
        if (articleTitle.isNotBlank()) {
            Text(
                text = articleTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = wpdsColors.onSurface,
                modifier = Modifier.padding(16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider()
        }

        // Reply banner (if replying)
        if (parentComment != null) {
            Text(
                text = "Replying to ${parentComment.author?.username}",
                color = wpdsColors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Guidelines message
        val uriHandler = LocalUriHandler.current
        val guidelinesText = buildAnnotatedString {
            append("Keep comments respectful and follow our ")
            pushStringAnnotation(
                tag = "URL",
                annotation = communityGuidelinesUrl
            )
            pushStyle(
                SpanStyle(
                    color = wpdsColors.cta,
                    textDecoration = TextDecoration.Underline
                )
            )
            append("guidelines")
            pop()
            pop()
            append(".")
        }
        ClickableText(
            text = guidelinesText,
            style = MaterialTheme.typography.bodySmall.copy(
                color = wpdsColors.onSurfaceSubtle
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .semantics {
                    contentDescription = "Guidelines"
                    role = Role.Button
                    onClick("Open community guidelines") {
                        uriHandler.openUri(communityGuidelinesUrl)
                        true
                    }
                },
            onClick = { offset ->
                guidelinesText.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    uriHandler.openUri(annotation.item)
                }
            }
        )

        // Comment box (multi-line)
        OutlinedTextField(
            value = text,
            onValueChange = { if (!commentsDisabled && it.length <= maxChar) text = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 120.dp),
            placeholder = { Text("Share your thoughts") },
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = wpdsColors.onSurface,
                unfocusedTextColor = wpdsColors.onSurface,
                cursorColor = wpdsColors.cta,
                focusedBorderColor = wpdsColors.outline,
                unfocusedBorderColor = wpdsColors.outline,
                focusedContainerColor = wpdsColors.surface,
                unfocusedContainerColor = wpdsColors.surface,
                unfocusedPlaceholderColor = wpdsColors.onSurfaceSubtle,
                focusedPlaceholderColor = wpdsColors.onSurfaceSubtle
            )
        )

        // Character count
        Text(
            text = "${text.length} / $maxChar",
            color = wpdsColors.onSurfaceSubtle,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Send button
        Button(
            onClick = {
                if (isValid) {
                    onAction(CommentAction.SendComment(text, parentComment))
                    text = ""
                    onAction(CommentAction.CancelReply)
                    onClose()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = wpdsColors.cta,
                contentColor = wpdsColors.onCta,
                disabledContainerColor = wpdsColors.gray200,
                disabledContentColor = wpdsColors.onCta.copy(alpha = 0.6f)
            )
        ) {
            Text("Send")
        }
    }
}
