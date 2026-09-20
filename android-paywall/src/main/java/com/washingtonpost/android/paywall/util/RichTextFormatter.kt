package com.washingtonpost.android.paywall.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

const val INDEX_RESET = -1
const val BOLD_START = "<b>"
const val BOLD_ENDED = "</b>"
const val ITALIC_START = "<i>"
const val ITALIC_END = "</i>"
const val COLOR_END = "</color>"
const val COLOR_START_TAG_LENGTH = 9
const val COLOR_END_TAG_LENGTH = 8
const val EMPTY_STRING = ""
const val ANY_START_TAG = '<'
const val ANY_END_TAG ='>'
val COLOR_REGEX = Regex(pattern = "<#\\w{6}>", options = setOf(RegexOption.IGNORE_CASE))
val COLOR_HEX_REGEX = Regex(pattern = "#\\w{6}", options = setOf(RegexOption.IGNORE_CASE))

/**
 * Starts the formatting of the encoded string. [context] is needed to access the style resources.
 */
fun getFormattedStringForText(context: Context, encodedString: String, listOfEncodingStyles: List<RichTextEncodingStyle>, useColorEncoding: Boolean = true): SpannableStringBuilder {
    /**
     * Used for storing the string character by character and manipulate it as necessary.
     */
    var builder = StringBuilder()


    /**
     * Removes all of the font styling tags from the original encoded string.
     */
    var styleTagsRemoved = EMPTY_STRING
    listOfEncodingStyles.forEach {
        styleTagsRemoved = if(styleTagsRemoved.isNotEmpty()) {
            styleTagsRemoved.replace(it.startTag, EMPTY_STRING).replace(it.endTag, EMPTY_STRING)
        } else {
            encodedString.replace(it.startTag, EMPTY_STRING).replace(it.endTag, EMPTY_STRING)
        }
    }

    /**
     * Removes the color encoding. [modifiedText] Used as a final plain-text string without any formatting encoded/applied.
     */
    val modifiedText =
        if (styleTagsRemoved.isNotEmpty()) {
            styleTagsRemoved.replace(COLOR_REGEX, EMPTY_STRING).replace(COLOR_END, EMPTY_STRING)
         } else {
            encodedString.replace(COLOR_REGEX, EMPTY_STRING).replace(COLOR_END, EMPTY_STRING)
        }

    /**
     * Used as a final spannable string without any formatting encoded but applied iteratively and is returned as a result.
     */
    val spanString = SpannableStringBuilder(modifiedText)

    /**
     * All indexes are set to -1 to begin with.
     */
    var colorStartIndex = INDEX_RESET

    var colorEndIndex: Int

    var colorString = EMPTY_STRING

    val startIndexes = HashMap<Int, Int>()


    encodedString.forEach { c ->
        builder.append(c)
        listOfEncodingStyles.forEachIndexed { index, richTextEncodingStyle ->
            /*
               any start tag will always appear at the end if the string contains it since the checks are made for every single character
           */
            if (builder.contains(richTextEncodingStyle.startTag)) {
                val startIndex = builder.length - richTextEncodingStyle.startTag.length
                startIndexes[index] = startIndex
                if (startIndex < builder.length) {
                    /*
                        Remove the <b> tag at the end of the string.
                     */
                    val rangeReplacedString =
                        builder.replaceRange(startIndex, builder.length, EMPTY_STRING)
                    builder = StringBuilder(rangeReplacedString)
                }
            }

            /*
                any end tag will always appear at the end if the string contains it since the checks are made for every single character
            */
            if (builder.contains(richTextEncodingStyle.endTag)) {
                val endIndex = builder.length - richTextEncodingStyle.endTag.length
                val startIndex = startIndexes[index]
                if (endIndex < builder.length) {
                    /*
                       Remove the e.g. </b> tag at the end of the string.
                    */
                    val rangeReplacedString =
                        builder.replaceRange(endIndex, builder.length, EMPTY_STRING)
                    builder = StringBuilder(rangeReplacedString)
                }
                /*
                    Performs bolding operation for current e.g. bold tag.
                 */
                if (startIndex != null && startIndex >= 0 && endIndex > 0)
                    spanString.setSpan(
                        TextAppearanceSpan(
                            context,
                            richTextEncodingStyle.style
                        ), startIndex, endIndex, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                /*
                    Reset the indexes for the next e.g. <b></b> tags.
                 */
                startIndexes.remove(index)
            }
        }

        when {

            /*
               <#$$$$$$> tag will always appear at the end if the string contains it since the checks are made for every single character
           */
            useColorEncoding && builder.contains(COLOR_REGEX) -> {
                colorStartIndex = builder.length - COLOR_START_TAG_LENGTH
                colorString = getColorFromString(builder)
                if (colorStartIndex < builder.length) {
                    /*
                        Remove the <b> tag at the end of the string.
                     */
                    val rangeReplacedString =
                        builder.replaceRange(colorStartIndex, builder.length, EMPTY_STRING)
                    builder = StringBuilder(rangeReplacedString)
                }
            }

            /*
                </color> tag will always appear at the end if the string contains it since the checks are made for every single character
            */
            useColorEncoding && builder.contains(COLOR_END) -> {
                colorEndIndex = builder.length - COLOR_END_TAG_LENGTH
                if (colorEndIndex < builder.length) {
                    /*
                       Remove the </i> tag at the end of the string.
                    */
                    val rangeReplacedString =
                        builder.replaceRange(colorEndIndex, builder.length, EMPTY_STRING)
                    builder = StringBuilder(rangeReplacedString)
                }
                /*
                    Performs bolding operation for current bold tag.
                 */
                if (colorString.isNotEmpty() && colorString.matches(COLOR_HEX_REGEX))
                    spanString.setSpan(
                        ForegroundColorSpan(Color.parseColor(colorString)),
                        colorStartIndex,
                        colorEndIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                /*
                    Reset the indexes for the next <b></b> tags.
                 */
                colorStartIndex = INDEX_RESET
                colorEndIndex = INDEX_RESET
                colorString = EMPTY_STRING
            }
        }
    }
    return spanString
}


/**
 * Removes the hex color value from the string provided.
 * provided string should have the color tag e.g. <#f1f1f1> at the end of the string.
 */
private fun getColorFromString(sb: StringBuilder): String {
    val length = sb.length
    val helperSb = StringBuilder()
    var end = length - 1
    /*
        Start from end and then reverse the string to obtain the result.
     */
    while (end >= 0) {
        when (val charAt = sb[end--]) {
            ANY_START_TAG -> break
            ANY_END_TAG -> continue
            else -> helperSb.append(charAt)
        }
    }
    return helperSb.reverse().toString()
}

@Composable
fun rememberHtmlAnnotatedString(html: String): AnnotatedString {
    return remember(html) {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toAnnotatedString()
    }
}

fun Spanned.toAnnotatedString(): AnnotatedString {
    return buildAnnotatedString {
        val text = this@toAnnotatedString.toString()
        append(text)

        getSpans(0, text.length, Any::class.java).forEach { span ->
            val start = getSpanStart(span)
            val end = getSpanEnd(span)

            when (span) {
                is StyleSpan -> {
                    when (span.style) {
                        Typeface.BOLD -> addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold), start, end
                        )

                        Typeface.ITALIC -> addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic), start, end
                        )
                    }
                }

                is UnderlineSpan -> addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline), start, end
                )

                is ForegroundColorSpan -> addStyle(
                    SpanStyle(color = androidx.compose.ui.graphics.Color(span.foregroundColor)), start, end
                )
            }
        }
    }
}