package com.washingtonpost.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension
) {
    commonExtension.compileSdk = ConfigData.compileSdk
    commonExtension.defaultConfig.apply {
        minSdk = ConfigData.minSdk
        manifestPlaceholders.putAll(
            mapOf(
                "targetSdkVersion" to "${ConfigData.targetSdk}",
                "appAuthRedirectScheme" to ConfigData.authRedirectUri,
                "authScheme" to ConfigData.authScheme,
                "authRedirectPath" to ConfigData.authRedirectPath,
                "universalLinkHost" to ConfigData.universalLinkHost,
                "iterableLinkHost" to ConfigData.iterableLinkHost,
            )
        )
        buildConfigField("String", "AUTH_SCHEME", "\"${ConfigData.authScheme}\"")
        buildConfigField("String", "AUTH_REDIRECT_PATH", "\"${ConfigData.authRedirectPath}\"")
        buildConfigField("String", "APP_AUTH_REDIRECT_URI", "\"${ConfigData.authRedirectUri}\"")
        buildConfigField("String", "ITERABLE_LINK_HOST", "\"${ConfigData.iterableLinkHost}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    commonExtension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin()
    configureKapt()
}

private fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}

private fun Project.configureKapt() {
    tasks.withType<KaptGenerateStubs>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}
