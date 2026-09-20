package com.wapo.flagship.features.posttv.vimeo

internal object VimeoUtils {
    fun isDigitsOnly(str: CharSequence): Boolean {
        val len = str.length
        for (i in 0 until len) {
            if (!Character.isDigit(str[i])) {
                return false
            }
        }
        return true
    }

    fun isEmpty(str: CharSequence?): Boolean {
        return str == null || str.length == 0
    }
}