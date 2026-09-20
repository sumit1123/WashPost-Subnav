import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.washingtonpost.userhistory"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":android-commons"))
    implementation(project(":android-remotelog"))
    implementation(project(":android-live-views"))

    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Test.ext)
    androidTestImplementation(Dependencies.Espresso.core)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    implementation(Dependencies.Lifecycle.viewmodelKtx)

    implementation(Dependencies.Deps.gson)

    // Retrofit
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Retrofit.rxJava)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.converterScalars)

    // Moshi
    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)
}
