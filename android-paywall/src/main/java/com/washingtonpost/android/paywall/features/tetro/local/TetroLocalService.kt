/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.paywall.features.tetro.local

import com.washingtonpost.android.paywall.features.tetro.TetroResponse
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import okhttp3.Headers

interface TetroLocalService {
    fun updateMeterData(response: TetroResponse?)
    fun storeCookies(headers: Headers)
    fun getBufferedReadList() : List<ArticleStub>?
    fun getStateCookie() : String?
}