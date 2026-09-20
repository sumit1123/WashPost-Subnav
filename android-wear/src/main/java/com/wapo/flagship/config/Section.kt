/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.config

import com.wapo.flagship.features.section.models.SectionUrl
import com.wapo.flagship.features.section.models.PageType
import org.json.JSONException
import org.json.JSONObject

data class Section(val name: String, val sectionUrl: SectionUrl) {

    companion object {
        @Throws(JSONException::class)
        fun fromJSONObject(config: JSONObject): Section {
            val name =
                if (config.has("name")) config.getString("name") else ""
            // prioritize fusion
            val sectionUrl =
                if (config.has("fusionEndpoint")) {
                    SectionUrl(config.getString("fusionEndpoint"), PageType.FUSION)
                } else {
                    if (config.has("pageBuilderEndpoint")) {
                        SectionUrl(
                            config.getString("pageBuilderEndpoint"),
                            PageType.PAGE_BUILDER
                        )
                    } else {
                        SectionUrl("")
                    }
                }

            return Section(name, sectionUrl)
        }
    }

}