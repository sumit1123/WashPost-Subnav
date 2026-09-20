package com.washingtonpost.android.save

interface SaveActivity {
    fun onAllReadingListArticlesDeleted()

    fun getSaveProvider() : SaveProvider

    fun openSectionByUrl(url: String, defaultToWeb: Boolean): Boolean
}