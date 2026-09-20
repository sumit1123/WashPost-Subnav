/*
 * Copyright (c) 2018. The Washington Post
 */
import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {

    buildTypes {
        debug {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    namespace = "com.washingtonpost.android.recirculation"
}

dependencies {
    implementation(project(":wapoviews"))
    implementation(project(":android-audio"))
    implementation(project(":android-follow"))
    implementation(project(":wpvolley"))
    implementation(project(":wapotext"))
    implementation(project(":android-commons"))
    implementation(project(":wpds"))

    implementation(Dependencies.Support.design)
    implementation(Dependencies.Support.cardview)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.fragmentRuntime)
    implementation(Dependencies.Deps.constraintLayout)
    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Mockito.core)
    androidTestImplementation(Dependencies.Mockito.android)
    androidTestImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Support.annotations)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Deps.rxjava)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)
}
repositories {
    mavenCentral()
}
