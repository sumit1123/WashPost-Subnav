import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-android")
}

android {
    namespace = "com.washingtonpost.android.paywall.amazon"
}

dependencies {
    implementation(files("libs/in-app-purchasing-2.0.61.jar"))

    implementation(project(":android-paywall"))
    implementation(project(":android-commons"))
    implementation(project(":android-config"))

    testImplementation(Dependencies.Deps.jUnit)
    implementation(Dependencies.Deps.gson)
}