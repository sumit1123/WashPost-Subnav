import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.wapo.flagship.auto"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":android-audio"))
    implementation(project(":wpds"))

    implementation(Dependencies.Deps.carApp)
    implementation(Dependencies.Deps.carAppProjected)
    implementation(Dependencies.Deps.mediaCompat)
    implementation(Dependencies.Media3.common)
    implementation(Dependencies.Media3.session)
    implementation(Dependencies.Concurrent.futures)
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Lifecycle.runtimeKtx)
    implementation(Dependencies.Coroutines.core)
    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Glide.core)

    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Deps.robolectric)
    testImplementation(Dependencies.Coroutines.test)
}
