import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
}

android {
    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")
    }

    packaging {
        resources.excludes.add("libs/amazon-device-messaging-1.0.1.jar")
    }
    namespace = "com.wapo.android.push"
}

dependencies {
    implementation(project(":android-commons"))
    implementation(project(":android-config"))

    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Room.runtime)
    implementation(Dependencies.Support.appCompat)
    ksp(Dependencies.Room.compiler)
    implementation(Dependencies.Lifecycle.viewmodelKtx)
}