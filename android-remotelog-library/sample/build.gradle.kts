import com.washingtonpost.convention.ConfigData
import com.washingtonpost.convention.Dependencies

plugins {
    id("com.android.application")
    id("com.washingtonpost.dependencies")
}

android {
    namespace = "example.sample"

    compileSdk = ConfigData.compileSdk

    defaultConfig {
        applicationId = ""
        minSdk = ConfigData.minSdk
        targetSdk = ConfigData.targetSdk
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(Dependencies.Support.appCompat)
    implementation(project(":android-remotelog"))
}
