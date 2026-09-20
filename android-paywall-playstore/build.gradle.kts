import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-android")
}

android {
    namespace = "com.washingtonpost.android.paywall.playstore"

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(project(":android-paywall"))
    implementation(project(":android-commons"))
    implementation(project(":android-config"))

    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.PlayBilling.core)
    implementation(Dependencies.PlayBilling.ktx)
    implementation(Dependencies.Coroutines.core)
    implementation(Dependencies.Coroutines.android)
    testImplementation(Dependencies.Coroutines.test)
    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Test.runner)
    testImplementation(Dependencies.Test.rules)
    testImplementation(Dependencies.Mockito.kotlin)
}

repositories {
    mavenCentral()
}
