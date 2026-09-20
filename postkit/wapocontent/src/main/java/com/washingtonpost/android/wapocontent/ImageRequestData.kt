package com.washingtonpost.android.wapocontent

open class ImageRequestData
@JvmOverloads constructor(url: String, val width: Int, val height: Int, val key: String? = null) : RequestData(url, null)