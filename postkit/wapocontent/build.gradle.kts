import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.washingtonpost.android.wapocontent"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Kotlin.stdlib)
}
