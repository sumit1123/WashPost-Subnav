package com.wapo.flagship.common

fun String?.isTrulyEmpty(): Boolean = isNullOrEmpty() || equals("null", ignoreCase = true)
