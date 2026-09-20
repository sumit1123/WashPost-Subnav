import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.wapo.text"
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":android-commons"))
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    androidTestImplementation(Dependencies.Espresso.core)
    implementation(Dependencies.Support.v4)
    testImplementation(Dependencies.Deps.jUnit)
    implementation(Dependencies.Kotlin.stdlib)
}
