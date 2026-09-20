import com.washingtonpost.convention.Dependencies
import com.washingtonpost.convention.Versions
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.washingtonpost.convention.ConfigData
import org.yaml.snakeyaml.Yaml
import java.util.Base64

plugins {
    id("com.washingtonpost.application")
    id("com.washingtonpost.compose")
    id("com.washingtonpost.flavors")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.getkeepsafe.dexcount")
    id("com.google.firebase.firebase-perf")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("com.google.dagger.hilt.android")
}
val changeLog = Yaml().load<Map<String, Any>>(File("android-tablet/changelog.yml").inputStream())

val env = System.getenv()
val p12Content = env["P12_KEYSTORE_CONTENT"]
val signApk = env["STORE_PASS"] != null && env["KEY_PASS"] != null && env["KEY_ALIAS"] != null && (env["KEY_STORE"] != null || p12Content != null)
var appVersion = env["PLAYSTORE_VERSION_NAME"]
var appVersionCode = env["PLAYSTORE_VERSION_CODE"]
var appVersionAmazon = env["AMAZON_VERSION_NAME"]
var appVersionCodeAmazon = env["AMAZON_VERSION_CODE"]
val buildNumber = env["BUILD_NUMBER"]

val branchVersionSuffix = env["GIT_BRANCH"]
    ?.replace(Regex("^(tags/)|(origin/)"), "-") ?: ""

fun getAppVersionName(flavor: String): String {
    return (if (buildNumber != null) "build-$buildNumber-" else "") + flavor + branchVersionSuffix
}
fun versionCodeToInt(appVersionCode: String?): Int {
    return if (appVersionCode?.trim()?.isNotEmpty() == true) appVersionCode.toInt() else 1
}
val shouldMinify = env["SHOULD_MINIFY"]?.toBoolean() ?: false
if (env["GIT_BRANCH"] == "develop") {
    val buildNumberSuffix = if (buildNumber != null) " ($buildNumber)" else ""

    if (appVersion?.trim().isNullOrEmpty()) {
        appVersion = changeLog["playstore_version_name"]?.toString() + buildNumberSuffix
    }
    if (appVersionCode?.trim().isNullOrEmpty()) {
        appVersionCode = changeLog["playstore_version_code"]?.toString()
    }
    if (appVersionAmazon?.trim().isNullOrEmpty()) {
        appVersionAmazon = changeLog["amazon_version_name"]?.toString() + buildNumberSuffix
    }
    if (appVersionCodeAmazon?.trim().isNullOrEmpty()) {
        appVersionCodeAmazon = changeLog["amazon_version_code"]?.toString()
    }
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

detekt {
    toolVersion = Dependencies.Deps.detekt
    config.setFrom(files("$rootDir/config/detekt/default-detekt-config.yml"))
    buildUponDefaultConfig =true
    allRules =false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}

//noinspection GroovyAssignabilityCheck
android {
    lint {
//        checkReleaseBuilds false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError = false
        lintConfig = file("./qa-check/lint.xml")
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

        packaging {
            resources {
                pickFirsts += listOf(
                    "META-INF/LICENSE.txt",
                    "META-INF/NOTICE.txt"
                )
                excludes += listOf(
                    "LICENSE.txt",
                    "assets/databases/dillonDb.db",
                    "res/drawable-hdpi-v4/ic_launcher.png",
                    "res/drawable-mdpi-v4/ic_launcher.png",
                    "res/drawable-xhdpi-v4/ic_launcher.png",
                    "res/drawable-xxhdpi-v4/ic_launcher.png",
                    "jsr305_annotations/Jsr305_annotations.gwt.xml",
                    "third_party/java_src/error_prone/project/annotations/Annotations.gwt.xml",
                    "third_party/java_src/error_prone/project/annotations/Google_internal.gwt.xml",
                    "error_prone/Annotations.gwt.xml",
                    "META-INF/LICENSE"
                )
            }
        }

    signingConfigs {
        if (signApk) {
            create("wapo") {
                // Sign apk using p12Content if provided
                // otherwise use store file from the KEY_STORE location.
                storeFile = if (!p12Content.isNullOrBlank()) {
                    // Create a temporary file to store the P12 content and
                    // set the storeFile to the temporary file
                    val tempP12File = File.createTempFile("temp_keystore", ".p12")
                    tempP12File.writeBytes(p12Content.decodeBase64Bytes())
                    // way to ensure the file is deleted after the build is finished or when the Gradle process terminates.
                    tempP12File.deleteOnExit()
                    tempP12File
                } else {
                    file(env["KEY_STORE"] ?: "")
                }
                storePassword = env["STORE_PASS"]
                keyAlias = env["KEY_ALIAS"]
                keyPassword = env["KEY_PASS"]
            }
        }

        getByName("debug") {
            storeFile = file("devwapo.keystore")
            storePassword = "androidtest"
            keyAlias = "washingtonpost android dev"
            keyPassword = "androidtest"
        }

    }

    productFlavors {
        named("amazon") {
            applicationId = "com.washingtonpost.rainbow"

            versionName = if (!appVersionAmazon.isNullOrBlank()) {
                appVersionAmazon
            } else {
                getAppVersionName("amazon")
            }

            versionCodeToInt(appVersionCodeAmazon).let { versionCode = it }

            buildConfigField("String", "STORE_TYPE", "\"amazon\"")
            buildConfigField("String", "BASE_URL", "\"https://rainbowapi-stage.wpdigital.net/rainbow-data-service/rainbow/\"")
            buildConfigField("String", "SUBS_BASE_URL", "\"https://subscribe.washingtonpost.com/\"")
            buildConfigField("String", "SEARCH_BASE_URL", "\"https://tabletapi.washingtonpost.com/apps-data-service/\"")
            buildConfigField("String", "RECIPE_BASE_URL", "\"https://washpost-apps-origin-dev.ext.nile.works/recipe-search/api/v0/\"")
        }

        named("playstore") {
            applicationId = "com.washingtonpost.android"

            versionName = if (!appVersion.isNullOrBlank()) {
                appVersion
            } else {
                getAppVersionName("playstore")
            }

            versionCodeToInt(appVersionCode).let { versionCode = it }

            buildConfigField("String", "STORE_TYPE", "\"playstore\"")
            buildConfigField("String", "BASE_URL", "\"https://rainbowapi-stage.wpdigital.net/rainbow-data-service/rainbow/\"")
            buildConfigField("String", "SUBS_BASE_URL", "\"https://subscribe.washingtonpost.com/\"")
            buildConfigField("String", "SEARCH_BASE_URL", "\"https://tabletapi.washingtonpost.com/apps-data-service/\"")
            buildConfigField("String", "RECIPE_BASE_URL", "\"https://tabletapi.washingtonpost.com/apps-data-service/recipe-search.json\"")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            // Enable Crashlytics mapping file upload
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
            proguardFiles("proguard-rules.pro", getDefaultProguardFile("proguard-android-optimize.txt"))
            if (signApk) signingConfig = signingConfigs["wapo"]
        }
        create("beta") {
            initWith(buildTypes["release"])
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".beta"

            val authScheme = "com.washingtonpost.beta"
            val universalLinkHost = "beta.washingtonpost.com"
            val authRedirectUri = "$authScheme://${ConfigData.authRedirectPath}"
            buildConfigField("String", "AUTH_SCHEME", "\"$authScheme\"")
            buildConfigField("String", "APP_AUTH_REDIRECT_URI", "\"$authRedirectUri\"")
            manifestPlaceholders.putAll(
                mapOf(
                    "authScheme" to authScheme,
                    "appAuthRedirectScheme" to authRedirectUri,
                    "universalLinkHost" to universalLinkHost
                )
            )
        }
        getByName("debug") {
            isMinifyEnabled = shouldMinify
            isShrinkResources = shouldMinify

            // Crashlytics mapping file upload
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = shouldMinify
            }
            // if (signApk) signingConfig signingConfigs.wapo
            signingConfig = signingConfigs["debug"]
            versionNameSuffix = "-dev"
            proguardFiles("proguard-rules.pro", "proguard-rules-test.pro", getDefaultProguardFile("proguard-android-optimize.txt"))
            // testCoverageEnabled true
            // ext.alwaysUpdateBuildId = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }

    sourceSets {
        getByName("amazon").java.srcDir("src/amazon/java")
        getByName("beta") {
            //  Add debug config overrides to beta builds
            assets.srcDirs("../android-config/src/debug/assets")
        }
    }

    useLibrary("org.apache.http.legacy")
    defaultConfig {
        applicationId = "com.washingtonpost.android"
        vectorDrawables.useSupportLibrary = true
    }
    ndkVersion = "25.1.8937393"
    composeOptions {
        kotlinCompilerExtensionVersion = Dependencies.Compose.compiler
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    namespace = "com.washingtonpost.android"
    testNamespace = "com.wapo.flagship"
}

//noinspection GroovyAssignabilityCheck
dependencies {
    coreLibraryDesugaring(Dependencies.Deps.coreDesugaringLibs)
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar", "*.aar")))
    implementation(files("libs/comscore.3.1508.28.jar"))
    implementation(project(":android-userhistory"))
    implementation(project(":android-commons"))
    implementation(project(":android-commons:appsflyer"))
    implementation(project(":android-comics"))
    implementation(project(":wpvolley"))
    implementation(project(":nightmode"))
    implementation(project(":androidext"))
    implementation(project(":android-feedback"))
    implementation(project(":wapotext"))
    implementation(project(":sections"))
    implementation(project(":articles"))
    implementation(project(":com.wapo.rainbow.article.model"))
    implementation(project(":android-posttv"))
    implementation(project(":android-audio"))
    implementation(project(":android-articles"))
    implementation(project(":wapocontent"))
    implementation(project(":wapoviews"))
    implementation(project(":wpds"))
    implementation(project(":notifications"))
    implementation(project(":android-config"))
    implementation(project(":android-paywall"))
    playstoreImplementation(project(":android-paywall-playstore"))
    playstoreImplementation(project(":android-auto"))
    amazonImplementation(project(":android-paywall-amazon"))
    implementation(project(path = ":adsInf"))
    implementation(project(path = ":android-push"))
    playstoreImplementation(project(path = ":android-push-playstore"))
    amazonImplementation(project(path = ":android-push-amazon"))
    implementation(project(":android-airship"))
    implementation(project(":android-gdpr"))
    implementation(project(":android-zendesk"))
    implementation(project(":android-remotelog"))
    implementation(project(":android-remotelog-splunk"))
    implementation(project(":android-save"))
    implementation(project(":android-recirculation"))
    implementation(project(":android-foryou"))
    implementation(project(":android-customnav"))
    implementation(project(":android-follow"))
    implementation(project(":networkutils"))
    implementation(project(":android-aixp"))

    implementation(Dependencies.Support.v4)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Deps.preference)
    implementation(Dependencies.Lifecycle.runtimeKtx)
    implementation(Dependencies.Deps.activityCompose)
    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.interop)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.tooling)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Compose.material3)
    implementation(Dependencies.Compose.material3Adaptive)
    implementation(Dependencies.Compose.runtimeLivedata)
    implementation(Dependencies.Compose.navigation)
    implementation(Dependencies.Deps.accompanistMaterial)
    implementation(Dependencies.Compose.composeConstraintLayout)
    androidTestImplementation(platform(Dependencies.Compose.bom))
    androidTestImplementation(Dependencies.Compose.junit)
    debugImplementation(Dependencies.Compose.tooling)
    debugImplementation(Dependencies.Compose.testManifest)
    playstoreImplementation(Dependencies.Deps.percentLayout)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    implementation(Dependencies.Deps.materialRipple)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Deps.constraintLayout)
    implementation(platform(Dependencies.Firebase.bom))
    implementation(Dependencies.Firebase.installations)
    implementation(Dependencies.Firebase.perf)
    implementation(Dependencies.PlayServices.ads)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.converterScalars)
    constraints {
        implementation(Dependencies.Deps.okHttp) {
            version {
                strictly(Versions.okHttpVersion)
            }
        }
    }
    constraints {
        implementation(Dependencies.Retrofit.okHttpLoggingInterceptor) {
            version {
                strictly(Versions.okHttpVersion)
            }
        }
    }

    implementation(Dependencies.ApacheCommons.io)
    androidTestImplementation(Dependencies.Support.annotations)
    androidTestImplementation(Dependencies.Espresso.core)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Espresso.contrib) {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    androidTestImplementation(Dependencies.Test.rules)
    androidTestImplementation(Dependencies.Deps.hamcrest)
    androidTestImplementation(Dependencies.Deps.jUnit)
    androidTestImplementation(Dependencies.ArchCore.testing)
    androidTestImplementation(Dependencies.Test.core)
    androidTestImplementation(Dependencies.Test.ext)
    androidTestImplementation(Dependencies.Espresso.intents)
    androidTestImplementation(Dependencies.Support.fragmentTesting)
    androidTestImplementation(Dependencies.Media3.ui)
    androidTestImplementation(Dependencies.Media3.exoplayer)
    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.ArchCore.testing)
    testImplementation(Dependencies.Coroutines.test)
    testImplementation(Dependencies.Mockito.kotlin)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Retrofit.runtime)
    implementation(Dependencies.Retrofit.gson)
    implementation(Dependencies.Retrofit.moshi)
    implementation(Dependencies.Retrofit.rxJava)
    implementation(Dependencies.Deps.picasso)
    implementation(Dependencies.Deps.circularReveal)
    implementation(Dependencies.Deps.materialCalendar)
    implementation(Dependencies.Deps.pollexor)
    implementation(Dependencies.Coroutines.android)
    implementation(Dependencies.Coroutines.core)
    implementation(Dependencies.Concurrent.futures)
    implementation(Dependencies.Room.runtime)
    implementation(Dependencies.Room.ktx)
    ksp(Dependencies.Room.compiler)
    implementation(Dependencies.Deps.youtube)
    implementation(Dependencies.Deps.browser)
    implementation(Dependencies.Deps.threetenabp)
    implementation(Dependencies.Firebase.crashlytics)
    implementation(Dependencies.Firebase.analytics)
    implementation(Dependencies.Firebase.remoteconfig)
    implementation(Dependencies.PlayServices.tagmanager)
    implementation(Dependencies.Deps.chartbeat)
    implementation(Dependencies.Deps.pagingKtx)

    implementation(Dependencies.Deps.okHttp)
    implementation(Dependencies.Deps.okHttpServerEvents)
    implementation(Dependencies.Retrofit.okHttpLoggingInterceptor)

    implementation(Dependencies.Deps.work)
    // GA
    implementation(Dependencies.PlayServices.analytics)
    implementation(Dependencies.Deps.activityKtx)
    implementation(Dependencies.Deps.fragmentKtx)
    implementation(Dependencies.Lifecycle.reactiveStreams)

    // Hilt
    implementation(Dependencies.DaggerHilt.core)
    ksp(Dependencies.DaggerHilt.compiler)

    implementation(Dependencies.Moshi.kotlin)
    implementation(Dependencies.Moshi.adapters)
    ksp(Dependencies.Moshi.codegen)
    implementation(Dependencies.Navigation.fragment)
    implementation(Dependencies.Navigation.ui)
    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    implementation(Dependencies.Glide.compose)
    ksp(Dependencies.Glide.ksp)
    implementation(Dependencies.Deps.onetrust)

    //App Search
    implementation(Dependencies.AppSearch.core)
    kapt(Dependencies.AppSearch.compiler)
    implementation(Dependencies.AppSearch.storage)

    // media 3
    implementation(Dependencies.Media3.exoplayer)
    implementation(Dependencies.Media3.dash)
    implementation(Dependencies.Media3.hls)
    implementation(Dependencies.Media3.ima)
    implementation(Dependencies.Media3.ui)
    implementation(Dependencies.Media3.session)
    implementation(Dependencies.Media3.cast)

    implementation(Dependencies.Deps.apsSdk)
    implementation(Dependencies.Deps.relinker)

    // Permutive
    implementation(Dependencies.Permutive.core) {
        exclude(group = "org.threeten", module = "threetenbp")
    }
    implementation(Dependencies.Permutive.googleAds)
    implementation(Dependencies.Deps.snakeYaml)

    implementation(Dependencies.Support.splash)


    debugImplementation(Dependencies.Deps.leakCanary)


    //needed for A9 to work
    implementation(Dependencies.Deps.iabtcf)

    // DataStore
    implementation(Dependencies.Deps.datastorePreferences)

    // Iterable
    implementation(Dependencies.Iterable.api)
    implementation(Dependencies.Iterable.ui)

    // For Jetpack Glance
    implementation(Dependencies.Glance.glance)
    implementation(Dependencies.Glance.appWidgets)
    implementation(Dependencies.Compose.coil)
    implementation(Dependencies.Compose.composeMaterialIcons)

    testImplementation(Dependencies.Test.mockk)
    implementation(Dependencies.KMP.kmpShared)

    implementation(Dependencies.Deps.kotlinxHtml)
    implementation(Dependencies.Deps.htmlParser)
}

apply {
    from("qa-check.gradle.kts")
}

fun String.decodeBase64Bytes(): ByteArray {
    return Base64.getDecoder().decode(this)
}