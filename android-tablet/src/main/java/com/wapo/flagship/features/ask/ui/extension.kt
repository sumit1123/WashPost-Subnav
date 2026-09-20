package com.wapo.flagship.features.ask.ui

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.wapo.android.commons.util.Logger
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeParseException
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.composed


fun Modifier.dottedBorder(
    color: Color,
    strokeWidth: Dp,
    dashLength: Dp,
    gapLength: Dp,
    shape: RoundedCornerShape
) = this.composed {
    val density = LocalDensity.current
    val stroke = Stroke(
        width = with(density) { strokeWidth.toPx() },
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(
                with(density) { dashLength.toPx() },
                with(density) { gapLength.toPx() }
            ),
            0f
        )
    )
    val offset = with(density) { 1.dp.toPx() }

    Modifier.drawWithContent {
        drawContent()

        val path = Path().apply {
            val cornerRadius = shape.topStart.toPx(size, density) + 12

            // Top-left corner
            arcTo(
                rect = Rect(
                    left = offset,
                    top = offset,
                    right = (cornerRadius * 2) + offset,
                    bottom = (cornerRadius * 2) + offset
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Top side
            lineTo(x = size.width - cornerRadius - offset, y = offset)

            // Top-right corner
            arcTo(
                rect = Rect(
                    left = size.width - (cornerRadius * 2) - offset,
                    top = offset,
                    right = size.width - offset,
                    bottom = (cornerRadius * 2) + offset
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Right side
            lineTo(x = size.width - offset, y = size.height - cornerRadius - offset)

            // Bottom-right corner
            arcTo(
                rect = Rect(
                    left = size.width - (cornerRadius * 2) - offset,
                    top = size.height - (cornerRadius * 2) - offset,
                    right = size.width - offset,
                    bottom = size.height - offset
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Bottom side
            lineTo(x = cornerRadius + offset, y = size.height - offset)

            // Bottom-left corner
            arcTo(
                rect = Rect(
                    left = offset,
                    top = size.height - (cornerRadius * 2) - offset,
                    right = (cornerRadius * 2) + offset,
                    bottom = size.height - offset
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Left side
            lineTo(x = offset, y = cornerRadius + offset)
        }
        drawPath(
            path = path,
            color = color,
            style = stroke
        )
    }
}

fun String?.toDateLong(): Long? {
    if (this.isNullOrBlank()) {
        return null
    }

    return try {
        OffsetDateTime.parse(this).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        Logger.e("DateParsing", "Failed to parse date string: '$this'", e)
        null
    }
}

fun String.toAnnotatedString(): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)
    return buildAnnotatedString {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                }
                is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
        }
    }
}
