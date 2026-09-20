import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.washpost.airship"
}

dependencies {
    implementation(project(":android-commons"))
    implementation(project(":android-push"))
    implementation(project(":android-remotelog"))
    implementation(project(":android-config"))

    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Test.json)
    api(Dependencies.Deps.airshipCore)
    api(Dependencies.Deps.airshipAutomation)
    implementation(Dependencies.Deps.gson)
}