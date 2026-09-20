import com.washingtonpost.convention.Dependencies

plugins{
    id("com.washingtonpost.library")
}

android {
    namespace = "com.wapo.android.remotelog"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-commons"))
    implementation(project(":android-config"))

    //  Hilt
    implementation(Dependencies.DaggerHilt.core)

    testImplementation(Dependencies.Deps.jUnit)
    implementation(Dependencies.Support.annotations)

    implementation(Dependencies.KMP.kmpShared)
}
