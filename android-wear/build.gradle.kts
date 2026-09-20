import androidx.fragment.app.replace
import com.washingtonpost.convention.Dependencies
import com.washingtonpost.convention.ConfigData
import kotlin.text.isNotBlank

plugins {
    id("com.washingtonpost.dependencies")
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize") // or id("org.jetbrains.kotlin.plugin.parcelize")
}

val env = java.lang.System.getenv()
val signApk = env["STORE_PASS"] != null && env["KEY_PASS"] != null && env["KEY_ALIAS"] != null && env["KEY_STORE"] != null
val appVersion = env["WEAR_VERSION_NAME"]
val appVersionCode = env["WEAR_VERSION_CODE"]
val buildNumber = env["BUILD_NUMBER"]
val branchVersionSuffix = env["GIT_BRANCH"]?.replace(Regex("^(tags/)|(origin/)"), "-") ?: ""
val getAppVersionName: (String) -> String = { flavor ->
    (if (buildNumber != null) "build-$buildNumber-" else "") + flavor + branchVersionSuffix
}
val shouldMinify = env["SHOULD_MINIFY"]?.toBoolean() ?: false

android {
    compileSdk = ConfigData.wearTargetSdk

    defaultConfig {
        applicationId = "com.washingtonpost.android"
        minSdk = ConfigData.wearMinSdk
        targetSdk = ConfigData.wearTargetSdk
        versionCode = 1
        versionName = "1.0"
    }

    lint {
        // checkReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError = false
    }

    signingConfigs {
        create("debug") {
            storeFile = file("devwapo.keystore")
            storePassword = "androidtest"
            keyAlias = "washingtonpost android dev"
            keyPassword = "androidtest"
        }
        if (signApk) {
            create("wapo") {
                storeFile = file(env["KEY_STORE"]!!)
                storePassword = env["STORE_PASS"]!!
                keyAlias = env["KEY_ALIAS"]!!
                keyPassword = env["KEY_PASS"]!!
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signApk) signingConfig = signingConfigs.getByName("wapo")
        }
        getByName("debug") {
            isMinifyEnabled = shouldMinify
            isShrinkResources = shouldMinify
            isMultidexEnabled = true
            // if (signApk) signingConfig = signingConfigs.getByName("wapo")
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "-dev"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro", "proguard-rules-test.pro")
            // isTestCoverageEnabled = true
            // alwaysUpdateBuildId = false
        }
    }

    flavorDimensions += "default"

    productFlavors {
        create("playstore") {
            dimension = "default"
            if (appVersion != null && appVersion.isNotBlank()) {
                versionName = appVersion
            } else {
                versionName = getAppVersionName("wear")
            }
            if (appVersionCode != null && appVersionCode.isNotBlank()) {
                versionCode = appVersionCode.toInt()
            }
            buildConfigField("String", "STORE_TYPE", "\"playstore\"")
        }
    }

    dependencies {
        implementation(files("libs/in-app-purchasing-2.0.61.jar"))
        implementation(project(":articles"))
        implementation(project(":com.wapo.rainbow.article.model"))
        implementation(project(":sections"))
        implementation(project(":wapotext"))
        implementation(project(":wapoviews"))
        implementation(project(":android-airship"))
        implementation(project(":android-commons"))
        implementation(project(":android-audio"))
        implementation(project(":android-push"))
        "playstoreImplementation"(project(path = ":android-push-playstore"))
        implementation(platform(Dependencies.Firebase.bom))
        implementation(Dependencies.Firebase.crashlytics)
        implementation(Dependencies.Firebase.analytics)
        implementation(Dependencies.Wearable.core)
        "compileOnly"(Dependencies.Wearable.runtime)
        implementation(Dependencies.Wearable.complications)
        implementation(Dependencies.PlayServices.wearable)
        implementation(Dependencies.Deps.gson)
        implementation(Dependencies.Deps.picasso)
        implementation(Dependencies.DaggerHilt.core)
        ksp(Dependencies.DaggerHilt.compiler)
        implementation(Dependencies.CoroutinesWear.core)
        implementation(Dependencies.CoroutinesWear.android)
        implementation(Dependencies.Support.appCompat)
        implementation(Dependencies.Support.coreKtx)
        implementation(Dependencies.Lifecycle.runtimeKtx)
        implementation(Dependencies.Lifecycle.viewmodelKtx)
        implementation(Dependencies.Lifecycle.java8)
        implementation(Dependencies.Deps.activityKtx)
        implementation(Dependencies.Deps.fragmentKtx)
        implementation(Dependencies.Retrofit.runtime)
        implementation(Dependencies.Retrofit.gson)
        implementation(Dependencies.Retrofit.moshi)
        implementation(Dependencies.Moshi.adapters)
        implementation(Dependencies.Moshi.kotlin)
        ksp(Dependencies.Moshi.codegen)
        implementation(Dependencies.Deps.okHttp)
        implementation(Dependencies.Retrofit.okHttpLoggingInterceptor)
        implementation(Dependencies.Room.runtime)
        implementation(Dependencies.Room.ktx)
        ksp(Dependencies.Room.compiler)
        implementation(Dependencies.Deps.viewPager)
        implementation(Dependencies.Support.design)
        implementation(Dependencies.Deps.preferenceKtx)
        implementation(Dependencies.Deps.rxjava)
    }

    namespace = "com.washingtonpost.android"
}