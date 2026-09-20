import com.washingtonpost.convention.ConfigData
import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.washingtonpost.compose")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.washingtonpost.android.paywall"
    testNamespace = "com.washingtonpost.android.paywall.test"

    defaultConfig {
        minSdk = ConfigData.minSdk
        // targetSdkVersion ConfigData.targetSdk
        testInstrumentationRunner = "paywall.test.utils.CustomTestRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("debug") {
            // Note: This is for running tests. Otherwise, it should be defined at the app level.
            manifestPlaceholders["appAuthRedirectScheme"] = ConfigData.authRedirectUri
        }
    }

    sourceSets {
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":wapotext"))
    implementation(project(":android-commons"))
    implementation(project(":wpds"))
    implementation(project(":android-remotelog"))
    implementation(project(":android-config"))

    implementation(Dependencies.Deps.jwtDecode)
    implementation(Dependencies.Deps.appAuth)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Support.v4)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Support.annotations)
    implementation(Dependencies.Deps.constraintLayout)
    implementation(Dependencies.Deps.threetenabp)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Lifecycle.process)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Retrofit.okHttpLoggingInterceptor)
    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    ksp(Dependencies.Glide.ksp)
    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.ArchCore.testing)
    testImplementation(Dependencies.Coroutines.test)
    testImplementation(Dependencies.Test.core)
    testImplementation(Dependencies.Test.junitKtx)
    testImplementation(Dependencies.Mockito.kotlin)
    androidTestImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)
    androidTestImplementation(Dependencies.Deps.mockWebServer)
    implementation(Dependencies.Deps.jjwt)

    // ViewModel and LiveData
    implementation(Dependencies.Deps.viewPager)

    // Moshi
    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)

    implementation(Dependencies.Retrofit.rxJava)

    //  Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    //Compose
    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.tooling)
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Glide.compose)
}
