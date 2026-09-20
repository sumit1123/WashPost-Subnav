@file:JvmName("VoteGuideApi")
package com.wapo.flagship.features.grid.views.vote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable


class VoteGuide : HashMap<String, VoteState>() {

    fun getStates(): List<String> {
        return entries
                .filter { it.key != NO_STATE }
                .mapNotNull { it.value.stateName }
                .sorted()
    }

    fun getStateInfo(stateName: String?): VoteState? {
        return entries
                .filter { it.key != NO_STATE }
                .find { it.value.stateName == stateName }?.value
    }

    fun getDefaultState(): VoteState? {
        return entries.find { it.key == NO_STATE }?.value
    }

    companion object {
        const val NO_STATE = "NO_STATE"
    }
}

class VoteState(
        @SerializedName("voterRegistration") val voterRegistration: VoterRegistration?,
        @SerializedName("stateName") val stateName: String?,
        @SerializedName("instructions") val instructions: String?,
        @SerializedName("stateAbbrev") val stateAbbrev: String?
) : Serializable

class VoterRegistration(
        @SerializedName("readMore") val readMore: String?
) : Serializable

fun fromResources(context: Context, resId: Int, gson: Gson): VoteGuide {
    return context.resources.openRawResource(resId)
            .use {
                gson.fromJson(it.reader(), VoteGuide::class.java)
            }
}