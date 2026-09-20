package com.wpds.utils

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun MarkdownText(
    source: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    maxLines: Int = Integer.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val rootNode = remember(source) {
        parseMarkdown(source.text)
    }
    val markdownAnnotatedString = remember(rootNode) {
        styleMarkdown(rootNode, source.text, style)
    }
    val annotatedString = remember(markdownAnnotatedString) {
        applySourceStringAnnotations(markdownAnnotatedString, source)
    }
    Text(
        text = annotatedString,
        style = style,
        modifier = modifier,
        inlineContent = inlineContent,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout
    )
}

private fun parseMarkdown(text: String): ASTNode {
    val flavour = CommonMarkFlavourDescriptor()
    return MarkdownParser(flavour).buildMarkdownTreeFromString(text)
}

private fun styleMarkdown(node: ASTNode, text: String, style: TextStyle, listType: ListType = ListType.NONE): AnnotatedString {
    return buildAnnotatedString {
        var orderedListNum = 1
        node.children.forEach { child ->
            when (child.type) {
                MarkdownElementTypes.PARAGRAPH -> {
                    append(styleMarkdown(child, text, style))
                }
                MarkdownElementTypes.EMPH -> {
                    withStyle(style = style.copy(fontStyle = FontStyle.Italic).toSpanStyle()) {
                        append(styleMarkdown(child, text, style))
                    }
                }
                MarkdownElementTypes.STRONG -> {
                    withStyle(style = style.copy(fontWeight = FontWeight.Bold).toSpanStyle()) {
                        append(styleMarkdown(child, text, style))
                    }
                }
                MarkdownElementTypes.ORDERED_LIST -> {
                    append(styleMarkdown(child, text, style, ListType.OL))
                }
                MarkdownElementTypes.UNORDERED_LIST -> {
                    append(styleMarkdown(child, text, style, ListType.UL))
                }
                MarkdownElementTypes.LIST_ITEM -> {
                    withStyle(
                        ParagraphStyle(
                            textIndent = TextIndent(
                                firstLine = 20.sp,
                                restLine = 20.sp
                            )
                        )
                    ) {
                        when (listType) {
                            ListType.OL -> append("${orderedListNum++}. ")
                            ListType.UL -> append("• ")
                            ListType.NONE -> {/* No-op */}
                        }
                        append(styleMarkdown(child, text, style, listType))
                    }
                }
                MarkdownTokenTypes.TEXT,
                MarkdownTokenTypes.WHITE_SPACE -> {
                    append(text.substring(child.startOffset, child.endOffset))
                }
                MarkdownTokenTypes.EOL -> {
                    if (listType == ListType.NONE) append("\n")
                }
                else -> {
                    if (child.children.isNotEmpty()) {
                        append(styleMarkdown(child, text, style, listType))
                    } else {
                        val raw = text.substring(child.startOffset, child.endOffset)
                        if (raw in whiteListedSymbols) {
                            append(raw)
                        }
                    }
                }
            }
        }
    }
}

private val whiteListedSymbols = listOf("'", "\u2019", "[", "]")

enum class ListType { UL, OL, NONE }

private fun applySourceStringAnnotations(
    markdownAnnotatedString: AnnotatedString,
    source: AnnotatedString
): AnnotatedString {
    val renderedText = markdownAnnotatedString.text
    val sourceAnnotations = source
        .getStringAnnotations(0, source.length)
        .distinctBy { "${it.tag}|${it.item}|${it.start}|${it.end}" }
        .sortedBy { it.start }

    var searchFrom = 0

    return buildAnnotatedString {
        append(renderedText)

        markdownAnnotatedString.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
        markdownAnnotatedString.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }

        sourceAnnotations.forEach { ann ->
            val token = source.text.substring(ann.start, ann.end)
            val foundAt = renderedText.indexOf(token, searchFrom)
            if (foundAt >= 0) {
                addStringAnnotation(
                    tag = ann.tag,
                    annotation = ann.item,
                    start = foundAt,
                    end = foundAt + token.length
                )
                searchFrom = foundAt + token.length
            }
        }
    }
}

@Preview
@Composable
private fun MarkdownTextPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            MarkdownText(
                source = AnnotatedString(
                    """
                        President Trump's handling of the **Iran war** has led to a spike in oil prices [1], with the price per barrel reaching nearly $120 at one point, which could translate to gas prices surpassing a national average of $4 per gallon.
                        * **Gas prices:** have increased by 47 cents in the past week, averaging $3.48 for a gallon of regular.
                        * **Diesel prices:** are surging even faster, hitting $4.78 a gallon, up nearly 90 cents from a week ago.
                        * **Impact on economy:** the conflict is disrupting global supply chains [2], leading to warnings of price shocks for pharmaceuticals, semiconductors, and everything made from oil.
                
                        **And now here is a list of *gas prices*:**
                        1. $1.00 per gallon
                        2. $2.00 per gallon is pretty *cheap*
                        3. $3.00 per gallon
                        4. $4.00 per gallon
                        5. $5.00 per gallon
                        6. $6.00 per gallon is a lot of money for gas! So much that this text bleeds onto another line!
                        7. $7.00 per liter?
                        8. $8.00 per gallon
                        9. $9.00 per gallon
                        10. $9.99 9/10 per gallon. See how they try trick you with a fraction? There's no _tenth of a cent!_
                
                        If you have a follow-up question, feel free to let me know.
                    """.trimIndent()
                ),
                style = TextStyle(
                    color = wpdsColors.primary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FranklinItcStandardFontFamily,
                ),
                inlineContent = mapOf(),
            )
        }
    }
}
