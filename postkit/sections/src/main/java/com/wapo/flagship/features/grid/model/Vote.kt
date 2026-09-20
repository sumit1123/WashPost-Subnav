package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.VoteEntity

class Vote : Item()

fun getVote(voteEntity: VoteEntity) : Vote {
    return Vote().apply {
        layoutAttributes = PageModelMapper.getLayoutAttributes(voteEntity.layoutAttributes)
    }
}