import com.washingtonpost.convention.Dependencies
import com.washingtonpost.convention.Versions
import org.jetbrains.kotlin.gradle.utils.IMPLEMENTATION
import kotlin.text.lowercase

plugins {
    id("com.washingtonpost.library")
    id("com.washingtonpost.compose")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.wapo.flagship.features.audio"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin", "src/androidTest/kotlin")
        }
    }
}

dependencies {
    implementation(project(":android-userhistory"))
    implementation(project(":wapotext"))
    implementation(project(":wapoviews"))
    implementation(project(":android-commons"))
    implementation(project(":wpvolley"))
    implementation(project(":nightmode"))
    implementation(project(":wpds"))
    implementation(project(":android-feedback"))
    implementation(project(":android-config"))
    implementation(project(":android-remotelog"))

    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Deps.mockk)
    testImplementation(Dependencies.Deps.mockitoCore)
    testImplementation(Dependencies.Deps.robolectric)
    testImplementation(Dependencies.Deps.coreTesting)
    testImplementation(Dependencies.Deps.robolectric)
    testImplementation(Dependencies.Coroutines.test)
    androidTestImplementation(Dependencies.Espresso.core)
    implementation(Dependencies.Kotlin.stdlib)

    // media 3
    implementation(Dependencies.Media3.exoplayer)
    implementation(Dependencies.Media3.dash)
    implementation(Dependencies.Media3.hls)
    implementation(Dependencies.Media3.ima)
    implementation(Dependencies.Media3.ui)
    implementation(Dependencies.Media3.session)
    implementation(Dependencies.Media3.cast)

    implementation(Dependencies.Concurrent.futures)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Deps.preference)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Deps.constraintLayout)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Deps.rxAndroid)

    // Glide dependencies
    implementation(Dependencies.Glide.core)
    ksp(Dependencies.Glide.ksp)
    implementation(Dependencies.Glide.annotations)
    implementation(Dependencies.Glide.compose)

    implementation(Dependencies.Coroutines.core)
    implementation(Dependencies.Coroutines.android)

    // Lifecycle and Viewmodel
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Lifecycle.livedataKtx)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Deps.flexbox)

    // Cast
    implementation(Dependencies.PlayServices.castFramework)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    // Moshi
    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)

    // Room
    implementation(Dependencies.Room.runtime)
    implementation(Dependencies.Room.ktx)
    ksp(Dependencies.Room.compiler)

    //Compose
    implementation(Dependencies.Compose.livedata)
    implementation(Dependencies.Compose.material3)

    //Retrofit
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.converterScalars)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Retrofit.rxJava)
    constraints {
        implementation(Dependencies.Deps.okHttp) {
            version {
                strictly(Versions.okHttpVersion)
            }
        }
    }
    constraints {
        implementation(Dependencies.Retrofit.okHttpLoggingInterceptor) {
            version {
                strictly(Versions.okHttpVersion)
            }
        }
    }

    //OkHTTP
    implementation(Dependencies.Deps.okHttp)
    implementation(Dependencies.Deps.okHttpServerEvents)

    implementation(Dependencies.Deps.threetenabp)
}
