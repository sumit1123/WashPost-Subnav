import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.wapo.flagship.features.nightmode"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-commons"))

    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Support.appCompat)
}
