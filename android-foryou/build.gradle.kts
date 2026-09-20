import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.washingtonpost.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.washingtonpost.foryou"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":wapoviews"))
    implementation(project(":android-commons"))
    implementation(project(":android-remotelog"))
    implementation(project(":android-paywall"))
    implementation(project(":wpds"))
    implementation(project(":android-audio"))
    implementation(project(":android-userhistory"))
    implementation(project(":sections"))
    implementation(project(":android-audio"))
    implementation(project(":wapotext"))
    implementation(project(":wapocontent"))
    implementation(project(":android-posttv"))
    implementation(project(":android-recirculation"))

    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Lifecycle.livedataKtx)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
    implementation(Dependencies.Deps.constraintLayout)
    implementation(Dependencies.Support.cardview)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    ksp(Dependencies.Glide.ksp)
    implementation(Dependencies.Support.swipeRefreshLayout)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)

    // Compose
    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Deps.accompanistMaterial)
    implementation(Dependencies.Compose.tooling)
    debugImplementation(Dependencies.Compose.tooling)
    debugImplementation(Dependencies.Compose.testManifest)
    implementation(Dependencies.Glide.compose)
    implementation(Dependencies.Glide.transformations)

    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Support.annotations)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)

    implementation(Dependencies.Media3.exoplayer)
    implementation(Dependencies.Media3.ui)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)
}