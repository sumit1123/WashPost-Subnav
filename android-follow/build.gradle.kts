import com.washingtonpost.convention.ConfigData
import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.washingtonpost.android.follow"

    defaultConfig {
        minSdk = ConfigData.minSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables.useSupportLibrary = true
    }

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
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
        }
    }
}

dependencies {

    implementation(project(":wapoviews"))
    implementation(project(":wapotext"))
    implementation(project(":wpvolley"))
    implementation(project(":nightmode"))
    implementation(project(":android-commons"))
    implementation(project(":android-paywall"))

    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Support.swipeRefreshLayout)
    implementation(Dependencies.Support.cardview)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.recyclerViewSelection)

    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Kotlin.reflect)

    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Deps.pagingKtx)
    implementation(Dependencies.Deps.constraintLayout)

    implementation(Dependencies.Lifecycle.livedataKtx)
    implementation(Dependencies.Lifecycle.viewmodelKtx)

    implementation(Dependencies.Room.runtime)
    ksp(Dependencies.Room.compiler)
    implementation(Dependencies.Room.ktx)

    implementation(Dependencies.Retrofit.okHttpLoggingInterceptor)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.rxJava)

    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    ksp(Dependencies.Glide.ksp)

    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Test.ext)
    androidTestImplementation(Dependencies.Espresso.core)
}
