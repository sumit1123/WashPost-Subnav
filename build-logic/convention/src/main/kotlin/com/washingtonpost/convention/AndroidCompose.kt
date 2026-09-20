package com.washingtonpost.convention

import com.android.build.api.dsl.CommonExtension

fun configureCompose(commonExtension: CommonExtension) {
    commonExtension.buildFeatures.compose = true
    commonExtension.composeOptions.kotlinCompilerExtensionVersion = Dependencies.Compose.compiler
}
