import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.washingtonpost.android.notifications"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-commons"))
    implementation(project(":wapotext"))
    implementation(project(":wapoviews"))
    implementation(project(":wpvolley"))
    implementation(project(":nightmode"))
    implementation(project(":android-remotelog"))

    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.v4)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Deps.materialRipple)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Deps.gson)
    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)
}
