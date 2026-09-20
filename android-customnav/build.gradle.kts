import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.washingtonpost.customnav"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":sections"))
    implementation(project(":android-commons"))
    implementation(project(":wapoviews"))

    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Deps.flexbox)

    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Support.annotations)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)
}
