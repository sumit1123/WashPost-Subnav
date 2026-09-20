/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.models

import com.wapo.flagship.features.audio.viewmodels.ArticleData

sealed class UserClickEvent {
    class ClickArticle(val articleData: ArticleData) : UserClickEvent()
}
