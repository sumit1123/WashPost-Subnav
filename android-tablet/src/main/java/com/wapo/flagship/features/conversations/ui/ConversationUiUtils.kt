package com.wapo.flagship.features.conversations.ui

import com.wapo.kmpshared.features.conversations.domain.models.CommentItem
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentBodyNode
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentBodyNodeType
import kotlinx.html.FlowContent
import kotlinx.html.blockQuote
import kotlinx.html.div
import kotlinx.html.p
import kotlinx.html.stream.createHTML


internal fun List<CommentBodyNode>.toRawHtml(): String {
    if (isEmpty()) return ""
    return createHTML(prettyPrint = false).div {
        renderNodes(this@toRawHtml)
    }.undoDoubleEscapedEntities()
}

private fun FlowContent.renderNodes(nodes: List<CommentBodyNode>?) {
    nodes?.forEach { node ->
        when (node.type) {
            CommentBodyNodeType.PARAGRAPH -> p {
                node.content?.let { +it }
                renderNodes(node.children)
            }
            CommentBodyNodeType.BLOCKQUOTE -> blockQuote {
                node.content?.let { +it }
                renderNodes(node.children)
            }
        }
    }
}

/**
 * kotlinx.html's `+` operator escapes `&` to `&amp;`, which double-escapes
 * entities already present in the API content (e.g. `&quot;` → `&amp;quot;`).
 * A single replace of `&amp;` → `&` undoes that one level of escaping for all
 * entities, so HtmlCompat.fromHtml decodes them correctly.
 */
private fun String.undoDoubleEscapedEntities(): String =
    replace("&amp;", "&")

internal val CommentItem.roleText: String?
    get() = when {
        isStaff() -> "Staff"
        isSource() -> "Source"
        else -> null
    }
