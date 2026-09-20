package com.washingtonpost.foryou

import com.washingtonpost.foryou.data.ConsumedArticles

interface ConsumedListProvider {
    suspend fun getReadList(): List<ConsumedArticles>

}