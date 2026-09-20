/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.models

import com.wapo.flagship.features.section.models.ArticleMeta

sealed class UserEvent {

    data class SaveForLater(val articleMeta: ArticleMeta): UserEvent()
    object ListenToArticle: UserEvent()
    object Next: UserEvent()

}