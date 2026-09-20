import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-parcelize")
}

android {
    sourceSets {
        getByName("main"){
            java.srcDirs("src/main/kotlin")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
         }
    }
    namespace = "com.wapo.flagship.features.posttv"
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-commons"))
    implementation(project(":android-config"))

    androidTestImplementation(Dependencies.Espresso.core)
    implementation(Dependencies.Support.appCompat)
    testImplementation(Dependencies.Deps.jUnit)
    // media 3
    implementation(Dependencies.Media3.exoplayer)
    implementation(Dependencies.Media3.dash)
    implementation(Dependencies.Media3.hls)
    implementation(Dependencies.Media3.ima)
    implementation(Dependencies.Media3.ui)
    implementation(Dependencies.Media3.session)
    implementation(Dependencies.Media3.cast)
    implementation(Dependencies.Deps.youtube)
    implementation(Dependencies.Deps.interactiveMedia)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Deps.okHttp)
    implementation(Dependencies.Retrofit.okHttpLoggingInterceptor)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Deps.constraintLayout)
    implementation(Dependencies.Lifecycle.process)
}
