import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.washingtonpost.android.comics"
}

dependencies {
    implementation(project(":android-commons"))
    implementation(project(":android-config"))
    implementation(project(":adsInf"))

    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.rxJava)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Deps.rxjava)
    testImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.ArchCore.testing)
    androidTestImplementation(Dependencies.Test.core)
    androidTestImplementation(Dependencies.Test.ext)
    androidTestImplementation(Dependencies.Espresso.intents)
}
