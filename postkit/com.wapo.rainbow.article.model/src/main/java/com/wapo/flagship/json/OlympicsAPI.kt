package com.wapo.flagship.json

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class OlympicsMedals(
    @SerializedName("title") val title: String?,
    @SerializedName("data") val data: List<OlympicsMedalsEntry>?,
    @SerializedName("cta") val cta: OlympicsCta?,
): Serializable

class OlympicsMedalsEntry(
    @SerializedName("rank") val rank: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("bronze") val bronze: String?,
    @SerializedName("silver") val silver: String?,
    @SerializedName("gold") val gold: String?,
    @SerializedName("total") val total: String?,
): Serializable

class OlympicsSchedule(
    @SerializedName("title") val title: String?,
    @SerializedName("data") val data: List<OlympicsScheduleEntry>?,
    @SerializedName("cta") val cta: OlympicsCta?,
): Serializable

class OlympicsScheduleEntry(
    @SerializedName("start") val start: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("icon") val icon: String?,
): Serializable

class OlympicsCta(
    @SerializedName("title") val title: String?,
    @SerializedName("link") val link: OlympicsLink?,
): Serializable

class OlympicsLink(
    @SerializedName("url") val url: String?,
): Serializable