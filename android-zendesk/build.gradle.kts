import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        dataBinding = true
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/java")
    }

    configurations.configureEach {
        //Removing the transitive dependencies coming in from zendesk.
        // Flexbox Transitive dependency (coming from zendesk library) only looks for jcenter.
        // To avoid jcenter hits we have added the flexbox in tablet module and it can safely be excluded from here
        exclude(group = "com.google.android", module = "flexbox")
        // Forcing dependencies to use the same version of the app is defined in the versions.gradle file.
        // Build issues in test and androidTest with Zendesk and OneTrust libraries and seems they are
        // using an older version than what app is using.
        resolutionStrategy.force(Dependencies.Retrofit.converterScalars)
    }

    dependencies {
        implementation(project(":wpvolley"))
        implementation(project(":android-commons"))
        implementation(project(":android-config"))

        implementation(Dependencies.Deps.gson)
        testImplementation(Dependencies.Deps.jUnit)
        androidTestImplementation(Dependencies.Support.annotations)
        androidTestImplementation(Dependencies.Test.runner)
        androidTestImplementation(Dependencies.Test.rules)
        implementation(Dependencies.Support.appCompat)
        implementation(Dependencies.Support.design)
        implementation(Dependencies.Kotlin.stdlib)
        implementation(Dependencies.Support.coreKtx)
        implementation(Dependencies.Deps.activityKtx)
        implementation(Dependencies.Deps.fragmentKtx)
        implementation(Dependencies.Deps.constraintLayout)
        implementation(Dependencies.Glide.core)
        implementation(Dependencies.Glide.annotations)
        ksp(Dependencies.Glide.ksp)
        implementation(Dependencies.Deps.zendesk)
        implementation(Dependencies.Navigation.fragment)
        implementation(Dependencies.Navigation.ui)
        implementation(Dependencies.Deps.onetrust)
        testImplementation(Dependencies.Retrofit.converterScalars)
        androidTestImplementation(Dependencies.Retrofit.converterScalars)

        // Hilt
        implementation(Dependencies.DaggerHilt.core)
        ksp(Dependencies.DaggerHilt.compiler)
    }
    namespace = "com.wapo.zendesk"
}

dependencies {
    implementation(Dependencies.Deps.constraintLayout)
}
