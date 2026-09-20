/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */
package com.wapo.flagship.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashWrapper {

    fun init() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }

    fun logExtras(str: String?) {
        FirebaseCrashlytics.getInstance().log(str!!)
    }

    fun sendException(t: Throwable?) {
        FirebaseCrashlytics.getInstance().recordException(t!!)
    }

    fun setUserIdentifier(name: String) {
        FirebaseCrashlytics.getInstance().setUserId("User: $name")
    }

}