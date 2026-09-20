package com.wapo.android.commons.domain

interface AppContextUtilsRepo {

    fun isDebuggableBuild(): Boolean
    fun isBetaBuild(): Boolean
}
