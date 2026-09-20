import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.wapo.rainbow.article.model"
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-commons"))

    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.ApacheCommons.io)
    implementation(Dependencies.Support.annotations)
    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Mockito.core)
    testImplementation(Dependencies.Deps.robolectric)
    androidTestImplementation(Dependencies.Support.annotations)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)
}
