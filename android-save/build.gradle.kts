import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    testOptions {
        useLibrary("org.apache.http.legacy")
    }

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/java")
    }
    namespace = "com.washingtonpost.android.save"
    testNamespace =  "com.washingtonpost.android.save.test"
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":wapoviews"))
    implementation(project(":wpvolley"))
    implementation(project(":wapotext"))
    implementation(project(":wpds"))
    implementation(project(":android-commons"))
    implementation(project(":android-follow"))
    implementation(project(":android-recirculation"))
    implementation(project(":android-foryou"))
    implementation(project(":networkutils"))

    implementation(Dependencies.Support.appCompat)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Espresso.core)
    androidTestImplementation(Dependencies.ArchCore.testing)
    androidTestImplementation(Dependencies.Coroutines.test)
    testImplementation(Dependencies.Kotlin.test)
    testImplementation(Dependencies.Mockito.core)
    testImplementation(Dependencies.Powermock.core)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Coroutines.core)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Lifecycle.commons)
    implementation(Dependencies.Lifecycle.reactiveStreams)
    implementation(Dependencies.Support.swipeRefreshLayout)
    implementation(Dependencies.Room.runtime)
    implementation(Dependencies.Room.ktx)
    implementation(Dependencies.Deps.pagingKtx)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.rxJava)
    implementation(Dependencies.Retrofit.okHttpLoggingInterceptor)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    ksp(Dependencies.Room.compiler)
    // Lifecycle and Viewmodel
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Lifecycle.livedataKtx)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)
    // Glide
    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    ksp(Dependencies.Glide.ksp)
    //Compose
    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.material3)
    implementation(Dependencies.Compose.tooling)
}

repositories {
    mavenCentral()
}
