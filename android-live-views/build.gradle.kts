import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.washingtonpost.android.androidlive"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {

    implementation(project(":android-commons"))

    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.recyclerview)

    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    testImplementation(Dependencies.Deps.jUnit)

    androidTestImplementation(Dependencies.Espresso.core)
}
