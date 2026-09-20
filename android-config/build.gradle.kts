import com.washingtonpost.convention.ConfigData
import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.washingtonpost.android.config"
    testNamespace = "com.washingtonpost.android.config.test"

    defaultConfig {
        minSdk = ConfigData.minSdk
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":android-commons"))

    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Coroutines.core)
    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.okHttp)
    implementation(Dependencies.Deps.datastorePreferences)

    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Deps.jUnit)

    implementation(Dependencies.KMP.kmpShared)
}