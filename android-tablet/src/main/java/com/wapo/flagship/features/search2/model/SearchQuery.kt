package com.wapo.flagship.features.search2.model

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema

@Document
data class SearchQuery(
    /** Namespace for Note */
    @Document.Namespace
    val namespace: String = NAME_SPACE,

    /** Id for Note */
    @Document.Id
    val id: String,

    /** Field for text that that user inputs */
    @Document.StringProperty(
        indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES
    )
    val text: String
)

const val NAME_SPACE = "wapo_app"
