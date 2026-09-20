package com.wapo.flagship.features.wpvideos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.android.commons.util.truncatedString
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@Composable
fun VerticalVideoCommentButton(
    modifier: Modifier = Modifier,
    commentCount: Int? = null,
    showTooltip: Boolean = false,
    onClick: () -> Unit,
    onTooltipClosed: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (showTooltip) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Tooltip(
                    onClose = onTooltipClosed
                )
            }
        }
        Column(
            modifier = modifier.clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.50f))
                    .clickable { onClick() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_comment),
                    contentDescription = "Comments",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
            if ((commentCount ?: 0) > 0) {
                Text(
                    text = commentCount?.truncatedString().orEmpty(),
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Tooltip(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val tooltipColor = colorResource(com.wpds.wpds.R.color.gray80)
    Row(
        modifier = modifier.padding(top = 20.dp, start = 50.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = tooltipColor
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("New:")
                            }
                            append(" Add your thoughts and join the conversation on any video.")
                        },
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                        }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tooltip_close),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onClose() }
                    )
                }
            }
        }
        Canvas(modifier = Modifier.size(10.dp).padding(top = 6.dp)) {
            val path = Path().apply {
                moveTo(0f, size.height)
                lineTo(size.width, 0f)
                lineTo(0f, 0f)
                close()
            }
            drawPath(path, color = tooltipColor)
        }
    }
}

@Preview
@Composable
private fun VerticalVideoCommentButtonPreview() {
    AndroidClassicTheme {
        VerticalVideoCommentButton(
            commentCount = 10,
            showTooltip = true,
            onClick = {},
            onTooltipClosed = {}
        )
    }
}