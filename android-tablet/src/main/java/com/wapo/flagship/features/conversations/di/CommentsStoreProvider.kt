package com.wapo.flagship.features.conversations.di

import com.wapo.kmpshared.features.conversations.presentation.CommentsStore
import com.wapo.kmpshared.util.KMPURL

interface CommentsStoreProvider {
    fun makeCommentsStore(storyURL: KMPURL, arcID: String?): CommentsStore?
}
