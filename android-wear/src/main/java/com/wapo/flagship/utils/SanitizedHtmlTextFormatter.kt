/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils


import android.text.Html
import com.wapo.flagship.features.articles2.models.deserialized.SanitizedHtml

object SanitizedHtmlTextFormatter {

    fun format(sanitizedHtml: SanitizedHtml?): CharSequence {
        return when {
            sanitizedHtml?.content != null -> {
                if ("text/plain" == sanitizedHtml.mime) {
                    sanitizedHtml.content
                } else {
                    Html.fromHtml(sanitizedHtml.content, Html.FROM_HTML_MODE_LEGACY)
                }
            }
            else -> {
                ""
            }
        }
    }

}