import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.wapo.flagship.features.aixp"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    // Internal dependencies
    implementation(project(":android-commons"))
    implementation(project(":android-remotelog"))
    implementation(project(":wapotext"))
    implementation(project(":wapoviews"))
    implementation(project(":sections"))
    implementation(project(":android-audio"))
    implementation(project(":android-foryou"))
    implementation(project(":android-articles"))
    implementation(project(":android-zendesk"))
    implementation(project(":wpds"))
    implementation(project(":android-paywall"))
    implementation(project(":android-feedback"))

    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Test.ext)
    androidTestImplementation(Dependencies.Espresso.core)

    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
    implementation(Dependencies.Room.runtime)
    ksp(Dependencies.Room.compiler)
    implementation(Dependencies.Room.ktx)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Kotlin.reflect)
    implementation(Dependencies.Navigation.fragment)
    implementation(Dependencies.Navigation.ui)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    // Retrofit
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Retrofit.rxJava)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.converterScalars)

    // Moshi
    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)
    implementation(Dependencies.Retrofit.moshi)

    // Glide
    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    ksp(Dependencies.Glide.ksp)

    // Swipe
    implementation(Dependencies.Support.swipeRefreshLayout)
}
