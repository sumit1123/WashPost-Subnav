import com.washingtonpost.convention.Dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.washingtonpost.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wapo.networkutils.interceptors"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("11")
        }
    }
}

dependencies {
    implementation(project(":android-paywall"))
    implementation(project(":wpvolley"))
    implementation(project(":android-commons"))

    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Support.appCompat)
    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Deps.okHttp)
    implementation(Dependencies.Glide.okhttp)
}
