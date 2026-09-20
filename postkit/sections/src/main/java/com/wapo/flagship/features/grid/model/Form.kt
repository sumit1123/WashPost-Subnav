package com.wapo.flagship.features.grid.model

data class Form(
    val action: String,
    val fields: List<FormField>
)

data class FormField(
    val type: String,
    val param: String,
    val placeHolder: String
)