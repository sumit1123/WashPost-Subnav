import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.washingtonpost.android.articles"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }

    packaging {
        resources.excludes.addAll(
            listOf(
                "third_party/java_src/error_prone/project/annotations/Annotations.gwt.xml",
                "third_party/java_src/error_prone/project/annotations/Google_internal.gwt.xml",
                "error_prone/Annotations.gwt.xml"
            )
        )
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":android-commons"))
    implementation(project(":wapocontent"))
    implementation(project(":nightmode"))
    implementation(project(":wapoviews"))
    implementation(project(":wapotext"))
    implementation(project(":android-posttv"))
    implementation(project(":wpvolley"))
    implementation(project(":adsInf"))
    implementation(project(":com.wapo.rainbow.article.model"))
    implementation(project(":android-follow"))

    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.design)
    implementation(Dependencies.Deps.gson)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Deps.rxAndroid)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Support.coreKtx)
    implementation(Dependencies.Deps.constraintLayout)
    testImplementation(Dependencies.Mockito.core)
    testImplementation(Dependencies.Deps.jUnit)
    testImplementation(Dependencies.Deps.robolectric)
    androidTestImplementation(Dependencies.Test.runner)
    androidTestImplementation(Dependencies.Test.rules)
}
