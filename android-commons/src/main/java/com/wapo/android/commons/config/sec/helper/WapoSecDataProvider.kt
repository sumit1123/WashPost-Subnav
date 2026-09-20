package com.wapo.android.commons.config.sec.helper

import com.squareup.moshi.Moshi
import com.wapo.android.commons.config.sec.model.WapoSecData
import com.wapo.android.commons.util.Utils

object WapoSecDataProvider {
    private var secData: WapoSecData? = null
    private val moshi = Moshi.Builder().build()
    private val jsonAdapter = moshi.adapter(WapoSecData::class.java)
    private const val AMAZON_FLAVOR = "amazon"
    private const val PLAYSTORE_FLAVOR = "playstore"
    private val TAG = WapoSecDataProvider::class.simpleName

    fun loadWapoSecData(json: String) {
        secData = jsonAdapter.fromJson(json)
    }

    val appsFlyerKey
        get() = secData?.wapoData?.appsFlyer?.prodKey

    val splunkToken
        get() = secData?.wapoData?.logging?.splunkToken ?: ""

    val zendeskProdAppId
        get() = secData?.wapoData?.zendesk?.prodApplicationId ?: ""

    val zendeskProdClientId
        get() = secData?.wapoData?.zendesk?.prodClientId ?: ""

    val zendeskDevAppId
        get() = secData?.wapoData?.zendesk?.devApplicationId ?: ""

    val articleEncryptionKey
        get() = secData?.wapoData?.articles?.encryptionKey ?: ""

    val zendeskDevClientId
        get() = secData?.wapoData?.zendesk?.devClientId ?: ""

    val oneTrustDomainIdProd
        get() = secData?.wapoData?.oneTrust?.domainIdentifier ?: ""

    val oneTrustDomainIdStage
        get() = secData?.wapoData?.oneTrust?.stageDomainIdentifier ?: ""

    val permutive
        get() = secData?.wapoData?.permutive

    fun clientId(
        flavor: String,
        isProd: Boolean = true,
    ): String {
        val clientId =
            when {
                flavor == AMAZON_FLAVOR && isProd -> secData?.wapoData?.signInUnified?.clientIdProd
                flavor == AMAZON_FLAVOR && !isProd -> secData?.wapoData?.signInUnified?.clientIdStage
                flavor == PLAYSTORE_FLAVOR && isProd -> secData?.wapoData?.signInClassic?.clientIdProd
                flavor == PLAYSTORE_FLAVOR && !isProd -> secData?.wapoData?.signInClassic?.clientIdStage
                else -> ""
            } ?: ""
        return clientId
    }

    fun clientSecret(
        flavor: String,
        isProd: Boolean = true,
    ): String {
        val clientSecret =
            when {
                flavor == AMAZON_FLAVOR && isProd -> secData?.wapoData?.signInUnified?.clientSecretProd
                flavor == AMAZON_FLAVOR && !isProd -> secData?.wapoData?.signInUnified?.clientSecretStage
                flavor == PLAYSTORE_FLAVOR && isProd -> secData?.wapoData?.signInClassic?.clientSecretProd
                flavor == PLAYSTORE_FLAVOR && !isProd -> secData?.wapoData?.signInClassic?.clientSecretStage
                else -> ""
            } ?: ""
        return clientSecret
    }

    val jwtSecret
        get() = secData?.wapoData?.signInClassic?.jwtSecret ?: ""

    val airshipKeyDev
        get() =
            if (Utils.isAmazonBuild()) {
                secData?.wapoData?.airshipUnified?.devKey ?: ""
            } else {
                secData?.wapoData?.airshipClassic?.devKey ?: ""
            }

    val airshipSecretDev
        get() =
            if (Utils.isAmazonBuild()) {
                secData?.wapoData?.airshipUnified?.devSecret ?: ""
            } else {
                secData?.wapoData?.airshipClassic?.devSecret ?: ""
            }

    val airshipKeyBeta
        get() =
            if (Utils.isAmazonBuild()) {
            secData?.wapoData?.airshipUnified?.betaKey ?: ""
        } else {
            secData?.wapoData?.airshipClassic?.betaKey ?: ""
        }

    val airshipSecretBeta
        get() =
            if (Utils.isAmazonBuild()) {
                secData?.wapoData?.airshipUnified?.betaSecret ?: ""
            } else {
                secData?.wapoData?.airshipClassic?.betaSecret ?: ""
            }

    val airshipKeyProd
        get() =
            if (Utils.isAmazonBuild()) {
                secData?.wapoData?.airshipUnified?.prodKey ?: ""
            } else {
                secData?.wapoData?.airshipClassic?.prodKey ?: ""
            }

    val airshipSecretProd
        get() =
            if (Utils.isAmazonBuild()) {
                secData?.wapoData?.airshipUnified?.prodSecret ?: ""
            } else {
                secData?.wapoData?.airshipClassic?.prodSecret ?: ""
            }

    val iterableSecretDev
        get() = secData?.wapoData?.iterable?.devKey ?: ""

    val iterableSecretProd
        get() = secData?.wapoData?.iterable?.prodKey ?: ""

    val nimbusPublisherKeyDev
        get() = secData?.wapoData?.nimbus?.publisherKeyDev ?: ""

    val nimbusPublisherKeyProd
        get() = secData?.wapoData?.nimbus?.publisherKeyProd ?: ""

    val nimbusApiKeyDev
        get() = secData?.wapoData?.nimbus?.apiKeyDev ?: ""

    val nimbusApiKeyProd
        get() = secData?.wapoData?.nimbus?.apiKeyProd ?: ""

    val comscoreC2
        get() = secData?.wapoData?.comscore?.c2 ?: ""

    val comscoreSecret
        get() = secData?.wapoData?.comscore?.secretCode ?: ""

    val preferencesPublicKeyStage: String
        get() = secData?.wapoData?.preferences?.publicKeyStage ?: ""

    val preferencesPublicKeyProd: String
        get() = secData?.wapoData?.preferences?.publicKeyProd ?: ""
}
