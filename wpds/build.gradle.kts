import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.washingtonpost.compose")
}

android {
    namespace = "com.wpds.wpds"

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
    implementation(project(":nightmode"))

    implementation(project(":android-commons"))

    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Deps.accompanistMaterial)
    implementation(Dependencies.Compose.tooling)
    debugImplementation(Dependencies.Compose.tooling)
    debugImplementation(Dependencies.Compose.testManifest)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Compose.material3)
    implementation(Dependencies.Glide.compose)
    implementation(Dependencies.JetBrains.markdown)
}
