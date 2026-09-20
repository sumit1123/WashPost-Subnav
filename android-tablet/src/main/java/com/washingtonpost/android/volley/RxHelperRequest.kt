package com.washingtonpost.android.volley

private val CACHE_HIT = "cache-hit"
private val NOT_MODIFIED = ""

abstract class RxHelperRequest<T>(
    method: Int,
    url: String,
    errorListener: Response.ErrorListener?,
) : Request<T>(method, url, errorListener) {
    var isCacheHit: Boolean = false
        get() = field
        private set(value) {
            field = value
        }

    var isNotModified: Boolean = false
        get() = field
        private set(value) {
            field = value
        }

    override fun finish(tag: String?) {
        isNotModified = NOT_MODIFIED == tag
        super.finish(tag)
        onComplete()
    }

    protected abstract fun onComplete()

    override fun addMarker(tag: String?) {
        if (CACHE_HIT == tag) {
            isCacheHit = true
        }

        super.addMarker(tag)
    }
}
