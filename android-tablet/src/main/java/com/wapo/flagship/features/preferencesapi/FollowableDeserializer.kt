// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.wapo.android.commons.serialization.fromJsonToList
import com.wapo.flagship.features.preferencesapi.models.Followable
import java.lang.reflect.Type

class FollowableDeserializer : JsonDeserializer<Followable> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): Followable? {
        context ?: throw IllegalStateException("Json serialization context is missing")
        val jObj = json as? JsonObject ?: return null
        return Followable(
            disabled =
                if (jObj.get(DISABLED) is JsonNull) {
                    false
                } else {
                    jObj
                        .getAsJsonPrimitive(
                            DISABLED,
                        ).asBoolean
                },
            heading =
                if (jObj.get(HEADING) is JsonNull) {
                    return null
                } else {
                    jObj
                        .getAsJsonPrimitive(
                            HEADING,
                        ).asString
                },
            registrationHeading =
                if (jObj.get(REGISTRATION_HEADING) is JsonNull) {
                    return null
                } else {
                    jObj
                        .getAsJsonPrimitive(
                            REGISTRATION_HEADING,
                        ).asString
                },
            unfollowedAffordanceTitle =
                if (jObj.get(UNFOLLOWED_AFFORDANCE_TITLE) is JsonNull) {
                    return null
                } else {
                    jObj
                        .getAsJsonPrimitive(
                            UNFOLLOWED_AFFORDANCE_TITLE,
                        ).asString
                },
            followedAffordanceTitle =
                if (jObj.get(FOLLOWED_AFFORDANCE_TITLE) is JsonNull) {
                    return null
                } else {
                    jObj
                        .getAsJsonPrimitive(
                            FOLLOWED_AFFORDANCE_TITLE,
                        ).asString
                },
            image = if (jObj.get(IMAGE) is JsonNull) null else jObj.getAsJsonPrimitive(IMAGE).asString,
            unfollowedPrompt =
                if (jObj.get(UNFOLLOWED_PROMPT) is JsonNull) {
                    return null
                } else {
                    jObj
                        .getAsJsonPrimitive(
                            UNFOLLOWED_PROMPT,
                        ).asString
                },
            followedPrompt =
                if (jObj.get(FOLLOWED_PROMPT) is JsonNull) {
                    return null
                } else {
                    jObj
                        .getAsJsonPrimitive(
                            FOLLOWED_PROMPT,
                        ).asString
                },
            newsletters =
                if (jObj.get(NEWSLETTERS) is JsonNull) {
                    null
                } else {
                    gson.fromJsonToList(
                        jObj.get(NEWSLETTERS).asString,
                    )
                },
            notifications =
                if (jObj.get(NOTIFICATIONS) is JsonNull) {
                    null
                } else {
                    gson.fromJsonToList(
                        jObj.get(NOTIFICATIONS).asString,
                    )
                },
            bullets =
                if (jObj.get(BULLETS) is JsonNull) {
                    null
                } else {
                    gson.fromJsonToList(
                        jObj.get(NEWSLETTERS).asString,
                    )
                },
        )
    }

    companion object {
        @JvmField val DISABLED = "disabled"

        @JvmField val HEADING = "heading"

        @JvmField val REGISTRATION_HEADING = "registration_heading"

        @JvmField val UNFOLLOWED_AFFORDANCE_TITLE = "unfollowed_affordance_title"

        @JvmField val FOLLOWED_AFFORDANCE_TITLE = "followed_affordance_title"

        @JvmField val IMAGE = "image"

        @JvmField val UNFOLLOWED_PROMPT = "unfollowed_prompt"

        @JvmField val FOLLOWED_PROMPT = "followed_prompt"

        @JvmField val NEWSLETTERS = "newsletters"

        @JvmField val NOTIFICATIONS = "notifications"

        @JvmField val BULLETS = "bullets"

        private val gson = Gson()
    }
}
