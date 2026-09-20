import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.washingtonpost.android.volley"
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    useLibrary("org.apache.http.legacy")
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-commons"))

    implementation(Dependencies.Support.annotations)
    implementation(Dependencies.Support.design)
    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Support.annotations)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)
    testImplementation(Dependencies.Deps.robolectric)

    // https://youtrack.jetbrains.com/issue/KT-55297/kotlin-stdlib-should-declare-constraints-on-kotlin-stdlib-jdk8-and-kotlin-stdlib-jdk7
    implementation(platform(Dependencies.Kotlin.bom))
}
