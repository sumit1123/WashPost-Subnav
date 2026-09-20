gradle.startParameter.excludedTaskNames.addAll(
    listOf(
        ":build-logic:convention:testClasses",
        ":build-logic:convention:clean"
    )
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("kmp-classic/gradle/libs.versions.toml"))
        }
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        // Version should match with AGP version
        id("com.android.settings") version "9.1.1" apply false
    }
}
plugins {
    id("com.android.settings")
}
android {
    execution {
        //Ref: https://developer.android.com/build/r8-execution-profiles
        profiles {
            create("default") {
                r8 {
                    // AGP 9 runs R8 more efficiently in the Gradle process. This avoids
                    // competing Gradle/R8 heaps and worker-startup failures on CI.
                    runInSeparateProcess = false
                }
            }
            defaultProfile = "default"
        }
    }
}

include(":android-tablet")
include(":android-paywall")
include(":android-paywall-playstore")
include(":android-paywall-amazon")
include(":android-comics")
include(":android-push")
include(":android-push-playstore")
include(":android-push-amazon")
include(":android-gdpr")
include(":android-zendesk")
include(":android-audio")
include(":android-auto")
include(":android-save")
include(":android-articles")
include(":android-recirculation")
include(":android-follow")
include(":android-airship")
include(":android-live-views")
include(":android-commons")
include(":android-posttv")
include(":android-foryou")
include(":android-customnav")
include(":wpds")


include(":android-remotelog")
include(":android-remotelog-splunk")

project(":android-remotelog").projectDir = File("android-remotelog-library/android-remotelog")
project(":android-remotelog-splunk").projectDir = File("android-remotelog-library/android-remotelog-splunk")

val postKit = listOf(
    "sections", "articles", "nightmode", "wapotext", "androidext",
    "wapocontent", "wapoviews", "wpvolley", "notifications", "adsInf",
    "com.wapo.rainbow.article.model")

postKit.forEach {
    include(":$it")
}

rootProject.children.filter { it.name in postKit }.forEach { project ->
    project.projectDir = File("postkit/${project.name}")
}

include(":android-commons:appsflyer")
project(":android-push-playstore").projectDir = File("android-push/android-push-playstore")
project(":android-push-amazon").projectDir = File("android-push/android-push-amazon")
project(":android-airship").projectDir = File("android-push/android-airship")
include(":wpds")
include(":android-aixp")
include(":android-userhistory")
include(":networkutils")
include(":android-config")
include(":android-feedback")
includeBuild("kmp-classic")
