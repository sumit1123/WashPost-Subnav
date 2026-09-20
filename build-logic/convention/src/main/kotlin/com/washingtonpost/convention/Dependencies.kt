package com.washingtonpost.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

object ConfigData {
    const val minSdk = 21
    const val compileSdk = 37
    const val targetSdk = 37

    // wearable
    const val wearTargetSdk = 34
    const val wearMinSdk = 26

    // ndk
    const val ndkVversion = "25.1.8937393"

    const val authScheme = "com.washingtonpost.classic"
    const val authRedirectPath = "oauth-callback"
    const val authRedirectUri = "$authScheme://$authRedirectPath"
    const val universalLinkHost = "washingtonpost.com"
    const val iterableLinkHost = "links.washingtonpost.com"
}

object Versions {
    const val annotationsVersion = "1.9.1"      //  https://mvnrepository.com/artifact/androidx.annotation/annotation
    const val apacheCommonsVersion = "2.4"      //  https://mvnrepository.com/artifact/commons-io/commons-io - newer versions require Java Streams API on device
    const val archCoreVersion = "2.2.0"         //  https://mvnrepository.com/artifact/androidx.arch.core/core-common
    const val constraintLayoutVersion = "2.2.1" //  https://mvnrepository.com/artifact/androidx.constraintlayout/constraintlayout
    const val espressoVersion = "3.7.0"         //  https://mvnrepository.com/artifact/androidx.test.espresso/espresso-core
    const val fragmentVersion = "1.8.9"         //  https://mvnrepository.com/artifact/androidx.fragment/fragment
    const val jUnitVersion = "4.13.2"           //  https://mvnrepository.com/artifact/junit/junit
    const val kotlinVersion = "2.3.21"          //  https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
    const val lifecycleVersion = "2.8.7"        //  https://mvnrepository.com/artifact/androidx.lifecycle/lifecycle-runtime - 2.10.0 requires minSdkVersion>=23, 2.9.4 runtime error
    const val mockitoVersion = "5.21.0"         //  https://mvnrepository.com/artifact/org.mockito/mockito-core
    const val mockitoKotlinVersion = "6.1.0"
    const val mockkVersion = "1.13.10"
    const val mockwebserver = "5.3.2"           //  https://mvnrepository.com/artifact/com.squareup.okhttp3/mockwebserver
    const val powermockVersion = "2.0.9"        //  https://mvnrepository.com/artifact/org.powermock/powermock-core
    const val okHttpVersion = "5.2.1"          //  https://mvnrepository.com/artifact/com.squareup.okhttp3/logging-interceptor - 5.3.2 Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.2.0, expected version is 2.0.0.
    const val pagingVersion = "3.3.6"           //  https://mvnrepository.com/artifact/androidx.paging/paging-runtime
    const val retrofitVersion = "3.0.0"         //  https://mvnrepository.com/artifact/com.squareup.retrofit2/retrofit
    const val roomVersion = "2.7.2"             //  https://mvnrepository.com/artifact/androidx.room/room-runtime - 2.8.4 requires minSdkVersion>=23
    const val rxAndroidVersion = "1.2.1"        //  https://mvnrepository.com/artifact/io.reactivex/rxandroid
    const val rxJavaVersion = "1.3.8"           //  https://mvnrepository.com/artifact/io.reactivex/rxjava
    const val legacySupportVersion = "1.0.0"    //  https://mvnrepository.com/artifact/androidx.legacy/legacy-support-v4
    const val swipeRefreshLayoutVersion = "1.2.0"   //  https://mvnrepository.com/artifact/androidx.swiperefreshlayout/swiperefreshlayout
    const val appCompatVersion = "1.7.1"        //  https://mvnrepository.com/artifact/androidx.appcompat/appcompat
    const val materialVersion = "1.13.0"        //  https://mvnrepository.com/artifact/com.google.android.material/material
    const val coroutinesVersion = "1.10.2"      //  https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    const val gsonVersion = "2.13.2"            //  https://mvnrepository.com/artifact/com.google.code.gson/gson
    const val testVersion = "1.7.0"             //  https://mvnrepository.com/artifact/androidx.test/runner
    const val testExtJunitVersion = "1.3.0"     //  https://mvnrepository.com/artifact/androidx.test.ext/junit
    const val picassoVersion = "2.8"            //  https://mvnrepository.com/artifact/com.squareup.picasso/picasso
    const val media3Version = "1.8.0"           //  https://mvnrepository.com/artifact/androidx.media3/media3-exoplayer - 1.9.0+ requires minSdkVersion>=23
    const val jwtDecodeVersion = "2.0.2"        //  https://mvnrepository.com/artifact/com.auth0.android/jwtdecode
    const val appAuthVersion = "0.7.1"          //  https://mvnrepository.com/artifact/net.openid/appauth - 0.11.1 Error authenticating user: Invalid ID Token: Issues at time is more than 10 minutes before of after the current...
    const val wearableVersion = "2.8.1"         //  https://mvnrepository.com/artifact/com.google.android.support/wearable
    const val playServicesAdsVersion = "23.6.0" //  https://mvnrepository.com/artifact/com.google.android.gms/play-services-ads - 24.0.0+ requires minSdkVersion>=23
    const val playServicesAdsIdentifierVersion = "18.2.0"   //  https://mvnrepository.com/artifact/com.google.android.gms/play-services-ads-identifier
    const val nimbusAdsVersion = "2.37.1"       //  https://docs.adsbynimbus.com/docs/sdk/android/changelog
    const val playServicesWearableVersion = "18.2.0"        //  https://mvnrepository.com/artifact/com.google.android.gms/play-services-wearable
    const val playServicesTagManagerVersion = "18.1.0"      //  https://mvnrepository.com/artifact/com.google.android.gms/play-services-tagmanager
    const val playServicesAnalyticsVersion = "18.1.0"       //  https://mvnrepository.com/artifact/com.google.android.gms/play-services-analytics
    const val playServicesCastFrameworkVersion = "22.0.0"   //  https://mvnrepository.com/artifact/com.google.android.gms/play-services-cast-framework - 22.2.0 requires a minSdkVersion>=23
    const val firebaseBomVersion = "32.8.1"     //  https://mvnrepository.com/artifact/com.google.firebase/firebase-bom - 34.0.0+ requires a minSdkVersion>=23
    const val youtubeVersion = "1.2.3"          //  https://github.com/tommus/youtube-android-player-api/releases
    const val interactiveMediaVersion = "3.37.0"    //  https://mvnrepository.com/artifact/com.google.ads.interactivemedia.v3/interactivemedia - 3.38.0 requires minSdkVersion>=23
    const val robolectricVersion = "4.17"       //  https://mvnrepository.com/artifact/org.robolectric/robolectric
    const val materialRippleVersion = "1.0.2"   //  https://mvnrepository.com/artifact/com.balysv/material-ripple
    const val flexboxVersion = "3.0.0"          //  https://mvnrepository.com/artifact/com.google.android.flexbox/flexbox
    const val bountyCastleVersion = "1.46"      //  https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk16
    const val preferenceVersion = "1.2.1"       //  https://mvnrepository.com/artifact/androidx.preference/preference
    const val percentLayoutVersion = "1.0.0"    //  https://mvnrepository.com/artifact/androidx.percentlayout/percentlayout
    const val hamcrestVersion = "3.0"           //  https://mvnrepository.com/artifact/org.hamcrest/hamcrest-library
    const val circularRevealVersion = "2.0.1"   //  https://github.com/ozodrukh/CircularReveal/releases/tag/2.1.0
    const val materialCalendarVersion = "2.0.1" //  https://github.com/prolificinteractive/material-calendarview/releases
    const val pollexorVersion = "3.0.0"         //  https://mvnrepository.com/artifact/com.squareup/pollexor
    const val wearVersion = "1.0.0"             //  https://mvnrepository.com/artifact/androidx.wear/wear - Latest version is 1.3.0 - As this isn't used right now, will be managed separately
    const val wearWatchfaceVersion = "1.0.0"    //  https://mvnrepository.com/artifact/androidx.wear.watchface/watchface-complications-data-source-ktx - Latest version is 1.2.1 - As this isn't used right now, will be managed separately
    const val relinkerVersion = "1.4.5"         //  https://mvnrepository.com/artifact/com.getkeepsafe.relinker/relinker
    const val browserVersion = "1.9.0"          //  https://mvnrepository.com/artifact/androidx.browser/browser
    const val threetenabpVersion = "1.4.9"      //  https://mvnrepository.com/artifact/com.jakewharton.threetenabp/threetenabp
    const val appsflyerVersion = "6.17.5"       //  https://mvnrepository.com/artifact/com.appsflyer/af-android-sdk
    const val installreferrerVersion = "2.2"    //  https://mvnrepository.com/artifact/com.android.installreferrer/installreferrer
    const val workVersionVersion = "2.10.0"     //  https://mvnrepository.com/artifact/androidx.work/work-runtime - 2.11.0 requires minSdkVersion>=23
    const val chartbeatVersion = "1.7.4"        //  https://mvnrepository.com/artifact/com.github.chartbeat/android_sdk
    const val activityKtxVersion = "1.11.0"     //  https://mvnrepository.com/artifact/androidx.activity/activity-ktx - 1.12.0+ requires minSdkVersion>=23
    const val fragmentKtxVersion = "1.8.9"      //  https://mvnrepository.com/artifact/androidx.fragment/fragment-ktx
    const val coreKtxVersion = "1.17.0"         //  https://mvnrepository.com/artifact/androidx.core/core-ktx
    const val recyclerViewVersion = "1.4.0"     //  https://mvnrepository.com/artifact/androidx.recyclerview/recyclerview
    const val recyclerViewSelectionVersion = "1.2.0"    //  https://mvnrepository.com/artifact/androidx.recyclerview/recyclerview-selection
    const val playBillingVersion = "8.0.0"      //  https://mvnrepository.com/artifact/com.android.billingclient/billing - 8.1.0+ requires minSdkVersion>=23
    const val airshipVersion = "17.8.1"         //  https://mvnrepository.com/artifact/com.urbanairship.android/urbanairship-core - 20.0.6 requires minSdkVersion>=23; 20.0.0 is compiled with kotlin 2.2.0 but current kotlin version is 2.0.21; 18.7.2+ requires several updates to AirshipAutopilot, AirshipPrivacyManager, AirshipProvider, AirshipPushManager, AirshipInAppMessageExtender
    const val jjwtVersion = "0.13.0"            //  https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt
    const val viewPager2Version = "1.1.0"       //  https://mvnrepository.com/artifact/androidx.viewpager2/viewpager2
    const val moshiVersion = "1.15.2"           //  https://mvnrepository.com/artifact/com.squareup.moshi/moshi-kotlin
    const val daggerHiltVersion = "2.58"        //  https://mvnrepository.com/artifact/com.google.dagger/hilt-android
    const val navigationVersion = "2.9.6"       //  https://mvnrepository.com/artifact/androidx.navigation/navigation-fragment-ktx
    const val glideVersion = "5.0.5"            //  https://mvnrepository.com/artifact/com.github.bumptech.glide/glide
    const val zendeskVersion = "5.5.2"          //  https://developer.zendesk.com/documentation/classic-web-widget-sdks/support-sdk/android/release_notes/
    const val onetrustVersion = "202604.2.0.0"  //  https://mvnrepository.com/artifact/com.onetrust.cmp/native-sdk - OneTrust versions must consist of 4 numbers to work
    const val apsSdkVersion = "10.1.1"          //  https://mvnrepository.com/artifact/com.amazon.android/aps-sdk - 11.0.4+ requires minSdkVersion>=23
    const val asyncLayoutInflaterVersion = "1.1.0"      //  https://mvnrepository.com/artifact/androidx.asynclayoutinflater/asynclayoutinflater
    const val appSearchVersion = "1.1.0"        //  https://mvnrepository.com/artifact/androidx.appsearch/appsearch
    const val concurrentVersion = "1.3.0"       //  https://mvnrepository.com/artifact/androidx.concurrent/concurrent-futures-ktx
    const val permutiveCoreVersion = "1.10.0"   //  https://mvnrepository.com/artifact/com.permutive.android/core
    const val permutiveGoogleAdsVersion = "2.0.0"       //  https://mvnrepository.com/artifact/com.permutive.android/google-ads - 2.1.0+ requires minSdkVersion>=23
    const val composeVersion = "2025.10.00"     //  https://mvnrepository.com/artifact/androidx.compose/compose-bom - 2025.10.01+ requires minSdkVersion>=23 (because of androidx.compose.material3.adaptive:adaptive:1.2.0)
    const val composeCompilerVersion = "1.5.15" //  https://developer.android.com/jetpack/androidx/releases/compose-kotlin
    const val activityComposeVersion = "1.11.0" //  https://mvnrepository.com/artifact/androidx.activity/activity-compose - 1.12.2 requires minSdkVersion>=23
    const val accompanistVersion = "0.36.0"     //  https://mvnrepository.com/artifact/com.google.accompanist/accompanist-themeadapter-material
    const val snakeYamlVersion = "2.5"          //  https://mvnrepository.com/artifact/org.yaml/snakeyaml
    const val leakCanaryVersion = "2.14"        //  https://mvnrepository.com/artifact/com.squareup.leakcanary/leakcanary-android
    const val splashVersion = "1.2.0"           //  https://mvnrepository.com/artifact/androidx.core/core-splashscreen
    const val glideComposeVersion = "1.0.0-beta01"      //  https://mvnrepository.com/artifact/com.github.bumptech.glide/compose
    const val glideTransformationsVersion ="4.3.0"      //  https://mvnrepository.com/artifact/jp.wasabeef/glide-transformations
    const val iabtcfDecoderVersion = "2.0.10"   //  https://mvnrepository.com/artifact/com.iabtcf/iabtcf-decoder
    const val composeMaterial3AndroidVersion = "1.4.0"  //  https://mvnrepository.com/artifact/androidx.compose.material3/material3-android
    const val composeMaterial3Adaptive = "1.1.0"        //  https://mvnrepository.com/artifact/androidx.compose.material3.adaptive/adaptive - 1.2.0 requires minSdkVersion>=23
    const val composeLivedataVersion = "1.9.5"         //  https://mvnrepository.com/artifact/androidx.compose.runtime/runtime-livedata - 1.10.0 requires minSdkVersion>=23
    const val datastoreVersion = "1.1.7"        //  https://mvnrepository.com/artifact/androidx.datastore/datastore-preferences - 1.2.0 requires minSdkVersion>=23
    const val coilComposeVersion = "2.7.0"      //  https://mvnrepository.com/artifact/io.coil-kt/coil-compose
    const val composeUI ="1.9.5"                //  https://mvnrepository.com/artifact/androidx.compose.ui/ui-android - 1.10.0 requires minSdkVersion>=23
    const val composeConstraintLayout ="1.1.1"  //  https://mvnrepository.com/artifact/androidx.constraintlayout/constraintlayout-compose
    const val detekt ="1.23.8"                  //  https://mvnrepository.com/artifact/io.gitlab.arturbosch.detekt/detekt-core
    const val iterable = "3.6.4"                //  https://mvnrepository.com/artifact/com.iterable/iterableapi
    const val glance ="1.1.1"                   //  https://mvnrepository.com/artifact/androidx.glance/glance
    const val desugarJdkVersion = "2.1.5"       //  https://mvnrepository.com/artifact/com.android.tools/desugar_jdk_libs
    const val ageSignals="0.0.4"                //  https://mvnrepository.com/artifact/com.google.android.play/age-signals
    const val jetBrainsMarkdown = "0.7.3"
    const val kotlinxHtmlVersion = "0.11.0"
    const val htmlParserVersion = "1.23.1"
    const val carAppVersion = "1.7.0"
    const val mediaCompatVersion = "1.7.0"
}

class Dependencies: Plugin<Project> {

    object Support {
        const val annotations = "androidx.annotation:annotation:${Versions.annotationsVersion}"
        const val appCompat = "androidx.appcompat:appcompat:${Versions.appCompatVersion}"
        const val recyclerview = "androidx.recyclerview:recyclerview:${Versions.recyclerViewVersion}"
        const val recyclerViewSelection = "androidx.recyclerview:recyclerview-selection:${Versions.recyclerViewSelectionVersion}"
        const val cardview = "androidx.cardview:cardview:${Versions.legacySupportVersion}"
        const val design = "com.google.android.material:material:${Versions.materialVersion}"
        const val v4 = "androidx.legacy:legacy-support-v4:${Versions.legacySupportVersion}"
        const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtxVersion}"
        const val fragmentRuntime = "androidx.fragment:fragment:${Versions.fragmentVersion}"
        const val fragmentTesting = "androidx.fragment:fragment-testing:${Versions.fragmentVersion}"
        const val swipeRefreshLayout = "androidx.swiperefreshlayout:swiperefreshlayout:${Versions.swipeRefreshLayoutVersion}"
        const val splash = "androidx.core:core-splashscreen:${Versions.splashVersion}"
    }

    object Room {
        const val runtime = "androidx.room:room-runtime:${Versions.roomVersion}"
        const val compiler = "androidx.room:room-compiler:${Versions.roomVersion}"
        const val ktx = "androidx.room:room-ktx:${Versions.roomVersion}"
    }

    object Lifecycle {
        const val runtime = "androidx.lifecycle:lifecycle-runtime:${Versions.lifecycleVersion}"
        const val runtimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycleVersion}"
        const val java8 = "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycleVersion}"
        const val compiler = "androidx.lifecycle:lifecycle-compiler:${Versions.lifecycleVersion}"
        const val viewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycleVersion}"
        const val livedataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycleVersion}"
        const val reactiveStreams = "androidx.lifecycle:lifecycle-reactivestreams:${Versions.lifecycleVersion}"
        const val commons = "androidx.lifecycle:lifecycle-common:${Versions.lifecycleVersion}"
        const val process = "androidx.lifecycle:lifecycle-process:${Versions.lifecycleVersion}"
    }

    object ArchCore {
        const val runtime = "androidx.arch.core:core-runtime:${Versions.archCoreVersion}"
        const val testing = "androidx.arch.core:core-testing:${Versions.archCoreVersion}"
    }

    object Retrofit {
        const val runtime = "com.squareup.retrofit2:retrofit:${Versions.retrofitVersion}"
        const val gson = "com.squareup.retrofit2:converter-gson:${Versions.retrofitVersion}"
        const val rxJava = "com.squareup.retrofit2:adapter-rxjava:${Versions.retrofitVersion}"
        const val moshi = "com.squareup.retrofit2:converter-moshi:${Versions.retrofitVersion}"
        const val converterScalars = "com.squareup.retrofit2:converter-scalars:${Versions.retrofitVersion}"
        const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttpVersion}"
    }

    object Espresso {
        const val core = "androidx.test.espresso:espresso-core:${Versions.espressoVersion}"
        const val contrib = "androidx.test.espresso:espresso-contrib:${Versions.espressoVersion}"
        const val intents = "androidx.test.espresso:espresso-intents:${Versions.espressoVersion}"
    }

    object Mockito {
        const val core = "org.mockito:mockito-core:${Versions.mockitoVersion}"
        const val android = "org.mockito:mockito-android:${Versions.mockitoVersion}"
        const val kotlin = "org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlinVersion}"
    }

    object Kotlin {
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlinVersion}"
        const val test = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlinVersion}"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlinVersion}"
        const val bom = "org.jetbrains.kotlin:kotlin-bom:${Versions.kotlinVersion}"
    }

    object Coroutines {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutinesVersion}"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}"
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutinesVersion}"
    }

    object Concurrent {
        const val futures = "androidx.concurrent:concurrent-futures-ktx:${Versions.concurrentVersion}"
    }

    object Media3 {
        const val core = "androidx.media3:media3-exoplayer-core:${Versions.media3Version}"
        const val ui = "androidx.media3:media3-ui:${Versions.media3Version}"
        const val common = "androidx.media3:media3-common:${Versions.media3Version}"
        const val hls = "androidx.media3:media3-exoplayer-hls:${Versions.media3Version}"
        const val ima = "androidx.media3:media3-exoplayer-ima:${Versions.media3Version}"
        const val session = "androidx.media3:media3-session:${Versions.media3Version}"
        const val cast = "androidx.media3:media3-cast:${Versions.media3Version}"
        const val exoplayer = "androidx.media3:media3-exoplayer:${Versions.media3Version}"
        const val dash = "androidx.media3:media3-exoplayer-dash:${Versions.media3Version}"
    }

    object Test {
        const val runner = "androidx.test:runner:${Versions.testVersion}"
        const val rules = "androidx.test:rules:${Versions.testVersion}"
        const val core = "androidx.test:core:${Versions.testVersion}"
        const val ext = "androidx.test.ext:junit:${Versions.testExtJunitVersion}"
        const val junitKtx = "androidx.test.ext:junit-ktx:${Versions.testExtJunitVersion}"
        const val json = "org.json:json:20210307"
        const val mockk = "io.mockk:mockk:${Versions.mockkVersion}"
    }

    object Wearable {
        const val core = "androidx.wear:wear:${Versions.wearVersion}"
        const val complications = "androidx.wear.watchface:watchface-complications-data-source-ktx:${Versions.wearWatchfaceVersion}"
        const val runtime = "com.google.android.wearable:wearable:${Versions.wearableVersion}"
        const val support = "com.google.android.support:wearable:${Versions.wearableVersion}"
    }

    object PlayServices {
        const val ads = "com.google.android.gms:play-services-ads:${Versions.playServicesAdsVersion}"
        const val adsIdentifier = "com.google.android.gms:play-services-ads-identifier:${Versions.playServicesAdsIdentifierVersion}"
        const val wearable = "com.google.android.gms:play-services-wearable:${Versions.playServicesWearableVersion}"
        const val tagmanager = "com.google.android.gms:play-services-tagmanager:${Versions.playServicesTagManagerVersion}"
        const val analytics = "com.google.android.gms:play-services-analytics:${Versions.playServicesAnalyticsVersion}"
        const val castFramework = "com.google.android.gms:play-services-cast-framework:${Versions.playServicesCastFrameworkVersion}"
    }

    object Firebase {
        const val bom = "com.google.firebase:firebase-bom:${Versions.firebaseBomVersion}"
        const val messaging = "com.google.firebase:firebase-messaging"
        const val perf = "com.google.firebase:firebase-perf"
        const val analytics = "com.google.firebase:firebase-analytics"
        const val remoteconfig = "com.google.firebase:firebase-config"
        const val crashlytics = "com.google.firebase:firebase-crashlytics"
        const val installations = "com.google.firebase:firebase-installations"
    }

    object ApacheCommons {
        const val io = "commons-io:commons-io:${Versions.apacheCommonsVersion}"
    }

    object Powermock {
        const val core = "org.powermock:powermock-core:${Versions.powermockVersion}"
        const val junit = "org.powermock:powermock-module-junit4:${Versions.powermockVersion}"
        const val mockito = "org.powermock:powermock-api-mockito2:${Versions.powermockVersion}"
    }

    object PlayBilling {
        const val core = "com.android.billingclient:billing:${Versions.playBillingVersion}"
        const val ktx = "com.android.billingclient:billing-ktx:${Versions.playBillingVersion}"
    }

    object Moshi {
        const val kotlin = "com.squareup.moshi:moshi-kotlin:${Versions.moshiVersion}"
        const val adapters = "com.squareup.moshi:moshi-adapters:${Versions.moshiVersion}"
        const val codegen = "com.squareup.moshi:moshi-kotlin-codegen:${Versions.moshiVersion}"
    }

    object Navigation {
        const val fragment = "androidx.navigation:navigation-fragment-ktx:${Versions.navigationVersion}"
        const val ui = "androidx.navigation:navigation-ui-ktx:${Versions.navigationVersion}"
        const val gradle = "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigationVersion}"
    }

    object Glide {
        const val core = "com.github.bumptech.glide:glide:${Versions.glideVersion}"
        const val annotations = "com.github.bumptech.glide:annotations:${Versions.glideVersion}"
        const val compiler = "com.github.bumptech.glide:compiler:${Versions.glideVersion}"
        const val ksp = "com.github.bumptech.glide:ksp:${Versions.glideVersion}"
        const val okhttp = "com.github.bumptech.glide:okhttp3-integration:${Versions.glideVersion}"
        const val compose = "com.github.bumptech.glide:compose:${Versions.glideComposeVersion}"
        const val transformations = "jp.wasabeef:glide-transformations:${Versions.glideTransformationsVersion}"
    }

    object DaggerHilt {
        const val core = "com.google.dagger:hilt-android:${Versions.daggerHiltVersion}"
        const val compiler = "com.google.dagger:hilt-compiler:${Versions.daggerHiltVersion}"
        const val plugin = "com.google.dagger:hilt-android-gradle-plugin:${Versions.daggerHiltVersion}"
    }

    object AppSearch {
        const val core = "androidx.appsearch:appsearch:${Versions.appSearchVersion}"
        const val compiler = "androidx.appsearch:appsearch-compiler:${Versions.appSearchVersion}"
        const val storage = "androidx.appsearch:appsearch-local-storage:${Versions.appSearchVersion}"
    }

    object Permutive {
        const val core = "com.permutive.android:core:${Versions.permutiveCoreVersion}"
        const val googleAds = "com.permutive.android:google-ads:${Versions.permutiveGoogleAdsVersion}"
    }

    object Iterable {
        const val api = "com.iterable:iterableapi:${Versions.iterable}"
        const val ui = "com.iterable:iterableapi-ui:${Versions.iterable}"
    }

    object KMP {
        const val kmpShared = "com.wapo.kmpshared:KMPShared"
    }

    object Compose {
        const val ui = "androidx.compose.ui:ui"
        const val graphics = "androidx.compose.ui:ui-graphics"
        const val tooling = "androidx.compose.ui:ui-tooling"
        const val material = "androidx.compose.material:material"
        const val material3 = "androidx.compose.material3:material3-android:${Versions.composeMaterial3AndroidVersion}"
        const val material3Adaptive = "androidx.compose.material3.adaptive:adaptive:${Versions.composeMaterial3Adaptive}"
        const val testManifest = "androidx.compose.ui:ui-test-manifest"
        const val junit = "androidx.compose.ui:ui-test-junit4"
        const val bom = "androidx.compose:compose-bom:${Versions.composeVersion}"
        const val runtimeLivedata = "androidx.compose.runtime:runtime-livedata"
        const val compiler = Versions.composeCompilerVersion
        const val navigation = "androidx.navigation:navigation-compose:${Versions.navigationVersion}"
        const val interop = "androidx.compose.ui:ui-viewbinding"
        const val livedata = "androidx.compose.runtime:runtime-livedata:${Versions.composeLivedataVersion}"
        const val coil = "io.coil-kt:coil-compose:${Versions.coilComposeVersion}"
        const val composeUI = "androidx.compose.ui:ui-android:${Versions.composeUI}"
        const val composeConstraintLayout = "androidx.constraintlayout:constraintlayout-compose:${Versions.composeConstraintLayout}"
        const val composeMaterialIcons = "androidx.compose.material:material-icons-core"
    }

    object AgeRestrictions {
        const val signals = "com.google.android.play:age-signals:${Versions.ageSignals}"
    }

    object Glance {
        const val glance = ("androidx.glance:glance:${Versions.glance}")
        const val appWidgets = ("androidx.glance:glance-appwidget:${Versions.glance}")
    }

    object Deps {
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}"
        const val jUnit = "junit:junit:${Versions.jUnitVersion}"
        const val mockWebServer = "com.squareup.okhttp3:mockwebserver:${Versions.mockwebserver}"
        const val rxjava = "io.reactivex:rxjava:${Versions.rxJavaVersion}"
        const val rxAndroid = "io.reactivex:rxandroid:${Versions.rxAndroidVersion}"
        const val pagingKtx = "androidx.paging:paging-runtime-ktx:${Versions.pagingVersion}"
        const val gson = "com.google.code.gson:gson:${Versions.gsonVersion}"
        const val picasso = "com.squareup.picasso:picasso:${Versions.picassoVersion}"
        const val jwtDecode = "com.auth0.android:jwtdecode:${Versions.jwtDecodeVersion}"
        const val appAuth = "net.openid:appauth:${Versions.appAuthVersion}"
        const val youtube = "com.github.tommus:youtube-android-player-api:${Versions.youtubeVersion}"
        const val interactiveMedia = "com.google.ads.interactivemedia.v3:interactivemedia:${Versions.interactiveMediaVersion}"
        const val robolectric = "org.robolectric:robolectric:${Versions.robolectricVersion}"
        const val materialRipple = "com.balysv:material-ripple:${Versions.materialRippleVersion}"
        const val flexbox = "com.google.android.flexbox:flexbox:${Versions.flexboxVersion}"
        const val bountyCastle = "org.bouncycastle:bcprov-jdk16:${Versions.bountyCastleVersion}"
        const val preference = "androidx.preference:preference:${Versions.preferenceVersion}"
        const val preferenceKtx = "androidx.preference:preference-ktx:${Versions.preferenceVersion}"
        const val percentLayout = "androidx.percentlayout:percentlayout:${Versions.percentLayoutVersion}"
        const val hamcrest = "org.hamcrest:hamcrest-library:${Versions.hamcrestVersion}"
        const val circularReveal = "com.github.ozodrukh:CircularReveal:${Versions.circularRevealVersion}"
        const val materialCalendar = "com.github.prolificinteractive:material-calendarview:${Versions.materialCalendarVersion}"
        const val pollexor = "com.squareup:pollexor:${Versions.pollexorVersion}"
        const val relinker = "com.getkeepsafe.relinker:relinker:${Versions.relinkerVersion}"
        const val browser = "androidx.browser:browser:${Versions.browserVersion}"
        const val threetenabp = "com.jakewharton.threetenabp:threetenabp:${Versions.threetenabpVersion}"
        const val appsflyer = "com.appsflyer:af-android-sdk:${Versions.appsflyerVersion}"
        const val installreferrer = "com.android.installreferrer:installreferrer:${Versions.installreferrerVersion}"
        const val work = "androidx.work:work-runtime:${Versions.workVersionVersion}"
        const val chartbeat = "com.github.chartbeat:android_sdk:${Versions.chartbeatVersion}"
        const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttpVersion}"
        const val okHttpServerEvents = "com.squareup.okhttp3:okhttp-sse:${Versions.okHttpVersion}"
        const val activityKtx = "androidx.activity:activity-ktx:${Versions.activityKtxVersion}"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:${Versions.fragmentKtxVersion}"
        const val airshipCore = "com.urbanairship.android:urbanairship-core:${Versions.airshipVersion}"
        const val airshipFcm = "com.urbanairship.android:urbanairship-fcm:${Versions.airshipVersion}"
        const val airshipAdm = "com.urbanairship.android:urbanairship-adm:${Versions.airshipVersion}"
        const val airshipAutomation = "com.urbanairship.android:urbanairship-automation:${Versions.airshipVersion}"
        const val jjwt = "io.jsonwebtoken:jjwt:${Versions.jjwtVersion}"
        const val viewPager = "androidx.viewpager2:viewpager2:${Versions.viewPager2Version}"
        const val zendesk = "com.zendesk:support:${Versions.zendeskVersion}"
        const val onetrust = "com.onetrust.cmp:native-sdk:${Versions.onetrustVersion}"
        const val apsSdk = "com.amazon.android:aps-sdk:${Versions.apsSdkVersion}"
        const val asyncLayoutInflater = "androidx.asynclayoutinflater:asynclayoutinflater:${Versions.asyncLayoutInflaterVersion}"
        const val activityCompose = "androidx.activity:activity-compose:${Versions.activityComposeVersion}"
        const val accompanistMaterial = "com.google.accompanist:accompanist-themeadapter-material:${Versions.accompanistVersion}"
        const val snakeYaml = "org.yaml:snakeyaml:${Versions.snakeYamlVersion}"
        const val leakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanaryVersion}"
        const val iabtcf = "com.iabtcf:iabtcf-decoder:${Versions.iabtcfDecoderVersion}"
        const val datastorePreferences = "androidx.datastore:datastore-preferences:${Versions.datastoreVersion}"
        const val detekt = "io.gitlab.arturbosch.detekt${Versions.detekt}"
        const val nimbus = "com.adsbynimbus.android:nimbus:${Versions.nimbusAdsVersion}"
        const val nimbusAps = "com.adsbynimbus.android:extension-aps:${Versions.nimbusAdsVersion}"
        const val nimbusAdMob = "com.adsbynimbus.android:extension-admob:${Versions.nimbusAdsVersion}"
        const val coreDesugaringLibs = "com.android.tools:desugar_jdk_libs:${Versions.desugarJdkVersion}"
        const val mockk = "io.mockk:mockk:${Versions.mockkVersion}"
        const val coreTesting = "androidx.arch.core:core-testing:${Versions.archCoreVersion}"
        const val mockitoCore = "org.mockito:mockito-core:${Versions.mockitoVersion}"
        const val kotlinxHtml = "org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtmlVersion}"
        const val htmlParser = "org.jsoup:jsoup:${Versions.htmlParserVersion}"
        const val carApp = "androidx.car.app:app:${Versions.carAppVersion}"
        const val carAppProjected = "androidx.car.app:app-projected:${Versions.carAppVersion}"
        const val mediaCompat = "androidx.media:media:${Versions.mediaCompatVersion}"
    }

    object JetBrains {
        const val markdown = "org.jetbrains:markdown:${Versions.jetBrainsMarkdown}"
    }
    override fun apply(target: Project) {
        // Empty Intentionally
    }
}
