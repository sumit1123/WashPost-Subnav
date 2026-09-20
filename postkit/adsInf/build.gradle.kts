import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.wapo.adsinf"

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-remotelog"))
    implementation(project(":wpds"))
    implementation(project(":android-commons"))
    implementation(project(":android-config"))
    implementation(project(":android-paywall"))

    androidTestImplementation(Dependencies.Espresso.core)
    implementation(Dependencies.Support.appCompat)
    testImplementation(Dependencies.Deps.jUnit)

    // Google Ads dependencies
    implementation(Dependencies.PlayServices.ads)
    implementation(Dependencies.PlayServices.adsIdentifier)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Kotlin.stdlib)

    //  Nimbus
    implementation(Dependencies.Deps.nimbus)
    implementation(Dependencies.Deps.nimbusAps)
    implementation(Dependencies.Deps.nimbusAdMob)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    implementation(Dependencies.PlayBilling.core)
    implementation(Dependencies.PlayBilling.ktx)
}
