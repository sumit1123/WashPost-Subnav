package com.arc.logger

open class Payload {

    open var tag: String? = TAG

    open var error: Throwable? = null

    open fun withTag(tag: String?): Payload {
        this.tag = tag
        return this
    }

    open fun withError(e: Throwable?): Payload {
        this.error = e
        return this
    }

    companion object {
        const val TAG = "Logger"

        @JvmOverloads
        @JvmStatic
        fun extra(tag: String? = null, e: Throwable? = null): Payload {
            return Payload()
                    .withError(e)
                    .withTag(tag)
        }
    }
}
