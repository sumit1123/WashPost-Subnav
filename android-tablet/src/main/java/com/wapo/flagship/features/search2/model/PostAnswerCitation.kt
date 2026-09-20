package com.wapo.flagship.features.search2.model

import androidx.compose.ui.text.AnnotatedString

data class ParsedCitation(
    val number: Int,
    val tag: String
)

data class ParsedTextResult(
    val annotated: AnnotatedString,
    val citations: List<ParsedCitation>
)