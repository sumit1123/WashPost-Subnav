package com.wapo.android.commons.data.repository

import com.wapo.android.commons.domain.AppContextUtilsRepo
import com.wapo.android.commons.util.AppContextUtils

class AppContextUtilsRepoImpl: AppContextUtilsRepo {

    override fun isDebuggableBuild(): Boolean {
        return AppContextUtils.isDebuggableBuild()
    }

    override fun isBetaBuild(): Boolean =
        AppContextUtils.isBetaBuild()
}
