package com.wapo.zendesk

import android.content.ContentResolver
import com.washingtonpost.android.config.domain.models.config.ZendeskConfig
import com.washingtonpost.android.volley.RequestQueue
import java.io.File

interface ZendeskProvider {
    val requestQueue: RequestQueue
    val metadataCustomField: String
    val config: ZendeskConfig
    val cacheDir: File
    val contentResolver: ContentResolver
    val email: String
    val name: String
    val isBeta: Boolean
}