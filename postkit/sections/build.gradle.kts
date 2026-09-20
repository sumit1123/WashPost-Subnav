import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.washingtonpost.compose")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.washingtonpost.android.sections"

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":androidext"))
    implementation(project(":wpvolley"))
    implementation(project(":android-commons"))
    implementation(project(":android-config"))
    implementation(project(":nightmode"))
    implementation(project(":android-live-views"))
    implementation(project(":wapoviews"))
    implementation(project(":wapotext"))
    implementation(project(":wapocontent"))
    implementation(project(":android-posttv"))
    implementation(project(":android-recirculation"))
    implementation(project(":wpds"))
    implementation(project(":adsInf"))
    implementation(project(":com.wapo.rainbow.article.model"))
    implementation(project(":android-userhistory"))
    implementation(project(":android-audio"))
    implementation(project(":android-remotelog"))
    implementation(project(":android-paywall"))

    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.v4)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Support.cardview)
    implementation(Dependencies.Deps.materialRipple)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Deps.flexbox)
    implementation(Dependencies.Deps.picasso)
    implementation(Dependencies.Deps.constraintLayout)
    implementation(Dependencies.Kotlin.reflect)
    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    implementation(Dependencies.Deps.fragmentKtx)


    implementation(Dependencies.Compose.material3)
    implementation(Dependencies.Compose.composeConstraintLayout)
    implementation(Dependencies.Glide.compose)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    //Compose
    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Compose.livedata)
    implementation(Dependencies.Compose.tooling)
    implementation(Dependencies.Compose.coil)

    // media 3
    implementation(Dependencies.Media3.exoplayer)
    implementation(Dependencies.Media3.dash)
    implementation(Dependencies.Media3.hls)
    implementation(Dependencies.Media3.ima)
    implementation(Dependencies.Media3.ui)
    implementation(Dependencies.Media3.session)
    implementation(Dependencies.Media3.cast)

    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Mockito.core)
    //https://github.com/robolectric/robolectric/issues/3288
    testImplementation(Dependencies.Deps.bountyCastle)
    testImplementation(Dependencies.Deps.robolectric)
    testImplementation(Dependencies.Test.core)
    androidTestImplementation(Dependencies.Support.annotations)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)
}
