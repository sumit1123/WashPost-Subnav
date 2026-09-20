import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wapo.flagship.features.articles"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
}

dependencies {
    implementation(project(":articles"))
    implementation(project(":android-live-views"))
    implementation(project(":wapotext"))
    implementation(project(":wapoviews"))
    implementation(project(":android-posttv"))
    implementation(project(":wpvolley"))
    implementation(project(":android-recirculation"))

    testImplementation(Dependencies.Deps.jUnit)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.design)
    androidTestImplementation(Dependencies.Espresso.core)
    implementation(Dependencies.Kotlin.stdlib)
}
