package com.wapo.flagship.features.support

abstract class DataProvider2 {
    abstract val versionName: String?

    abstract val appName: String?

    abstract val loginId: String?

    abstract val isPaywallTurnedOn: Boolean

    abstract val paywallType: String?

    abstract val paywallSource: String?

    abstract val paywallExpiration: String?

    abstract val paywallPartnerId: String?
}
