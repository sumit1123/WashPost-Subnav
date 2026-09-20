package com.wapo.android.commons.config.sec.model

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WapoSecData(
    @SerializedName("wapoData")
    @Json(name = "wapoData")
    val wapoData: WapoData? = null,
)

@JsonClass(generateAdapter = true)
data class Airship(
    @SerializedName("devKey")
    @Json(name = "devKey")
    val devKey: String? = null,
    @SerializedName("devSecret")
    @Json(name = "devSecret")
    val devSecret: String? = null,
    @SerializedName("betaKey")
    @Json(name = "betaKey")
    val betaKey: String? = null,
    @SerializedName("betaSecret")
    @Json(name = "betaSecret")
    val betaSecret: String? = null,
    @SerializedName("prodKey")
    @Json(name = "prodKey")
    val prodKey: String? = null,
    @SerializedName("prodSecret")
    @Json(name = "prodSecret")
    val prodSecret: String? = null,
)

@JsonClass(generateAdapter = true)
data class Logging(
    @SerializedName("splunkToken")
    @Json(name = "splunkToken")
    val splunkToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class WapoData(
    @SerializedName("zendesk")
    @Json(name = "zendesk")
    val zendesk: Zendesk? = null,
    @SerializedName("airshipUnified")
    @Json(name = "airshipUnified")
    val airshipUnified: Airship? = null,
    @SerializedName("airshipClassic")
    @Json(name = "airshipClassic")
    val airshipClassic: Airship? = null,
    @SerializedName("logging")
    @Json(name = "logging")
    val logging: Logging? = null,
    @SerializedName("signInClassic")
    @Json(name = "signInClassic")
    val signInClassic: SignInClassic? = null,
    @SerializedName("oneTrust")
    @Json(name = "oneTrust")
    val oneTrust: OneTrust? = null,
    @SerializedName("signInUnified")
    @Json(name = "signInUnified")
    val signInUnified: SignInUnified? = null,
    @SerializedName("appsFlyer")
    @Json(name = "appsFlyer")
    val appsFlyer: AppsFlyer? = null,
    @SerializedName("permutive")
    @Json(name = "permutive")
    val permutive: PermutiveConfig? = null,
    @Json(name = "iterable")
    val iterable: IterableConfig? = null,
    @SerializedName("articles")
    @Json(name = "articles")
    val articles: Articles? = null,
    @SerializedName("nimbus")
    @Json(name = "nimbus")
    val nimbus: NimbusConfig? = null,
    @SerializedName("comscore")
    @Json(name = "comscore")
    val comscore: ComscoreConfig? = null,
    @SerializedName("preferences")
    @Json(name = "preferences")
    val preferences: PreferencesConfig? = null,
)

@JsonClass(generateAdapter = true)
data class OneTrust(
    @SerializedName("domainIdentifier")
    @Json(name = "domainIdentifier")
    val domainIdentifier: String? = null,
    @SerializedName("stageDomainIdentifier")
    @Json(name = "stageDomainIdentifier")
    val stageDomainIdentifier: String? = null,
)

@JsonClass(generateAdapter = true)
data class Zendesk(
    @SerializedName("prodClientId")
    @Json(name = "prodClientId")
    val prodClientId: String? = null,
    @SerializedName("prodApplicationId")
    @Json(name = "prodApplicationId")
    val prodApplicationId: String? = null,
    @SerializedName("devClientId")
    @Json(name = "devClientId")
    val devClientId: String? = null,
    @SerializedName("devApplicationId")
    @Json(name = "devApplicationId")
    val devApplicationId: String? = null,
)

@JsonClass(generateAdapter = true)
data class SignInClassic(
    @SerializedName("clientSecretStage")
    @Json(name = "clientSecretStage")
    val clientSecretStage: String? = null,
    @SerializedName("clientIdProd")
    @Json(name = "clientIdProd")
    val clientIdProd: String? = null,
    @SerializedName("clientSecretProd")
    @Json(name = "clientSecretProd")
    val clientSecretProd: String? = null,
    @SerializedName("clientIdStage")
    @Json(name = "clientIdStage")
    val clientIdStage: String? = null,
    @SerializedName("jwtSecret")
    @Json(name = "jwtSecret")
    val jwtSecret: String? = null,
)

@JsonClass(generateAdapter = true)
data class SignInUnified(
    @SerializedName("clientSecretStage")
    @Json(name = "clientSecretStage")
    val clientSecretStage: String? = null,
    @SerializedName("clientIdProd")
    @Json(name = "clientIdProd")
    val clientIdProd: String? = null,
    @SerializedName("clientSecretProd")
    @Json(name = "clientSecretProd")
    val clientSecretProd: String? = null,
    @SerializedName("clientIdStage")
    @Json(name = "clientIdStage")
    val clientIdStage: String? = null,
)

@JsonClass(generateAdapter = true)
data class AppsFlyer(
    @SerializedName("prodKey")
    @Json(name = "prodKey")
    val prodKey: String? = null,
)

@JsonClass(generateAdapter = true)
data class PermutiveConfig(
    @SerializedName("workspaceId")
    @Json(name = "workspaceId")
    val workspaceId: String? = null,
    @SerializedName("apiKey")
    @Json(name = "apiKey")
    val apiKey: String? = null,
)

@JsonClass(generateAdapter = true)
data class IterableConfig(
    @SerializedName("devKey")
    @Json(name = "devKey")
    val devKey: String? = null,
    @SerializedName("prodKey")
    @Json(name = "prodKey")
    val prodKey: String? = null,
)

@JsonClass(generateAdapter = true)
data class Articles(
    @SerializedName("encryptionKey")
    @Json(name = "encryptionKey")
    val encryptionKey: String? = null,
)

@JsonClass(generateAdapter = true)
data class NimbusConfig(
    @SerializedName("publisherKeyDev")
    @Json(name = "publisherKeyDev")
    val publisherKeyDev: String? = null,
    @SerializedName("apiKeyDev")
    @Json(name = "apiKeyDev")
    val apiKeyDev: String? = null,
    @SerializedName("publisherKeyProd")
    @Json(name = "publisherKeyProd")
    val publisherKeyProd: String? = null,
    @SerializedName("apiKeyProd")
    @Json(name = "apiKeyProd")
    val apiKeyProd: String? = null,
)

@JsonClass(generateAdapter = true)
data class ComscoreConfig(
    @SerializedName("c2")
    @Json(name = "c2")
    val c2: String? = null,
    @SerializedName("secretCode")
    @Json(name = "secretCode")
    val secretCode: String? = null,
)


@JsonClass(generateAdapter = true)
data class PreferencesConfig(
    @SerializedName("publicKeyStage")
    @Json(name = "publicKeyStage")
    val publicKeyStage: String? = null,
    @SerializedName("publicKeyProd")
    @Json(name = "publicKeyProd")
    val publicKeyProd: String? = null,
)