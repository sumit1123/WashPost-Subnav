/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.android.domain.repository

import android.os.Bundle
import com.wapo.android.commons.logs.EventLog

interface RemoteLogRepo {

    fun d(eventLog: EventLog?)

    fun d(eventLog: EventLog?, process: String?)

    fun d(eventLog: EventLog?, process: String?, paywallInfo: String?)

    fun e(eventLog: EventLog?)

    fun e(eventLog: EventLog?, process: String?)

    fun e(eventLog: EventLog?, process: String?, paywallInfo: String?)

    fun w(eventLog: EventLog?)

    fun p(eventLog: EventLog?)

    fun p(eventLog: EventLog?, process: String?)

    fun p(eventLog: EventLog?, process: String?, paywallInfo: String?)

    fun v(eventLog: EventLog?)

    fun v(eventLog: EventLog?, process: String?)

    fun v(eventLog: EventLog?, process: String?, paywallInfo: String?)

    fun m(eventLog: EventLog?)

    fun m(eventLog: EventLog?, process: String?)

    fun m(eventLog: EventLog?, process: String?, paywallInfo: String?)

    fun uploadLogFiles(bundle: Bundle?)
}
