package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.CommentsActionEntity

object CommentsActionMapper {
    fun getCommentsAction(commentsActionEntity: CommentsActionEntity?): CommentsAction? {
        commentsActionEntity ?: return null
        if (commentsActionEntity.url == null) return null

        return CommentsAction(
            count = commentsActionEntity.count ?: 0,
            url = commentsActionEntity.url,
        )
    }
}