package com.wapo.adsinf.models

import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.mediation.MediationAdapter

data class NetworkExtras(
    val adapterClass: Class<out MediationAdapter>? = AdMobAdapter::class.java,
    val networkExtrasBundle: Bundle?
) {
    constructor(networkExtrasBundle: Bundle?) : this(AdMobAdapter::class.java, networkExtrasBundle)
}