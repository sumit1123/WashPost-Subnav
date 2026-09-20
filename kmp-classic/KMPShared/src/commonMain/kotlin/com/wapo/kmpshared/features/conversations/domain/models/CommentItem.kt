package com.wapo.kmpshared.features.conversations.domain.models

import com.wapo.kmpshared.features.conversations.domain.models.response.CommentAuthor
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentBodyNode
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentDomainActionCounts
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentStatus
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentTag
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentTagCode
import com.wapo.kmpshared.features.conversations.domain.models.response.CommentVideoItem
import com.wapo.kmpshared.util.KMPDate

data class CommentItem(
    val id: String,
    val parentId: String?,
    val revisionId: String?,
    val depth: Int,
    val createdAt: KMPDate,
    val author: CommentAuthor?,
    val bodyNodes: List<CommentBodyNode>,
    val videoItem: CommentVideoItem?,
    val status: CommentStatus?,
    val tags: List<CommentTag>,
    val actionCounts: CommentDomainActionCounts,
    val viewerAction: CommentViewerActionState,
    val replyCount: Int,
) {
    fun isStaff(): Boolean = tags.any { it.code == CommentTagCode.STAFF }

    fun isSource(): Boolean = tags.any { it.code == CommentTagCode.SOURCE_REPLY }
}
