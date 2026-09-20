import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.wapo.android.commons.appsFlyer"

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
    }
}

dependencies {
    implementation(project(":android-commons"))

    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Deps.appsflyer)
    implementation(Dependencies.Deps.installreferrer)
    implementation(Dependencies.Moshi.kotlin)
    ksp(Dependencies.Moshi.codegen)
}
