import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.wapo.android.push.amazon"
}

dependencies {
    implementation(project(":android-commons"))
    implementation(project(":android-push"))
    api(Dependencies.Deps.airshipAdm)
    compileOnly(files("libs/amazon-device-messaging-1.1.0.jar"))
}
