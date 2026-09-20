import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-android")
    id("androidx.navigation.safeargs")
    id("kotlin-parcelize")
}

android {
    namespace = "com.washingtonpost.android.gdpr"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":wapotext"))
    implementation(project(":android-commons"))
    implementation(project(":android-config"))

    implementation(Dependencies.Support.v4)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.coreKtx)
    androidTestImplementation(Dependencies.Support.annotations)

    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)

    implementation(Dependencies.Kotlin.stdlib)
    
    testImplementation(Dependencies.Deps.jUnit)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Deps.gson)
}
