import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.washingtonpost.android.logger"
}

dependencies {
    implementation(Dependencies.Kotlin.stdlib)
}
