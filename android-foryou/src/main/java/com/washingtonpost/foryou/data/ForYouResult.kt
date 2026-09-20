package com.washingtonpost.foryou.data

sealed class ForYouResult {
    class Success(val item: ForYouResponse) : ForYouResult()
    object Error : ForYouResult()
}