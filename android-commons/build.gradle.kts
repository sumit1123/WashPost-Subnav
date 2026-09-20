import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.wapo.android.commons.logger"
    testNamespace = "com.wapo.android.commons"

    useLibrary("org.apache.http.legacy")

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin", "src/androidTest/kotlin")
        }
    }
}

dependencies {
    implementation(Dependencies.Support.annotations)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Deps.rxjava)
    testImplementation(Dependencies.Deps.jUnit)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Support.coreKtx)
    androidTestImplementation(Dependencies.Espresso.core)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Espresso.contrib)
    androidTestImplementation(Dependencies.Test.core)
    androidTestImplementation(Dependencies.Test.ext)
    androidTestImplementation(Dependencies.Test.rules)
    testImplementation(Dependencies.Mockito.kotlin)
    implementation(Dependencies.Lifecycle.livedataKtx)
    implementation(Dependencies.Support.recyclerview)

    //  Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)

    implementation(Dependencies.Deps.threetenabp)

    implementation(Dependencies.Lifecycle.process)

    implementation(Dependencies.AgeRestrictions.signals)

    testImplementation(Dependencies.Deps.robolectric)
}
