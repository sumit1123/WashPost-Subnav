package com.wapo.android.commons.config.sec.helper

import javax.net.ssl.SSLSocketFactory

object SSLSocketFactoryProvider {
    @Volatile
    var sslSocketFactory: SSLSocketFactory? = null
}