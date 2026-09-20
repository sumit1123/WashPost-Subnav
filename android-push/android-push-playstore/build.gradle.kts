import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.wapo.android.push.playstore"
}

dependencies {
    implementation(project(":android-commons"))
    implementation(project(":android-push"))

    implementation(Dependencies.Firebase.messaging)
    api(Dependencies.Deps.airshipFcm)

    // Iterable
    implementation(Dependencies.Iterable.api)
    implementation(Dependencies.Iterable.ui)
}
