import com.washingtonpost.convention.Dependencies

plugins {
    id("com.washingtonpost.library")
    id("com.washingtonpost.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.wapo.view"

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))
    implementation(project(":wapotext"))
    implementation(project(":wpvolley"))
    implementation(project(":android-commons"))
    implementation(project(":android-remotelog"))
    implementation(project(":wpds"))
    implementation(project(":wapocontent"))
    implementation(project(":com.wapo.rainbow.article.model"))

    //Compose
    implementation(platform(Dependencies.Compose.bom))
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.graphics)
    implementation(Dependencies.Compose.material)
    implementation(Dependencies.Deps.accompanistMaterial)
    implementation(Dependencies.Compose.tooling)
    debugImplementation(Dependencies.Compose.tooling)
    debugImplementation(Dependencies.Compose.testManifest)
    implementation(Dependencies.Glide.compose)

    androidTestImplementation(Dependencies.Espresso.core)
    implementation(Dependencies.Support.appCompat)
    implementation(Dependencies.Support.recyclerview)
    implementation(Dependencies.Support.design)
    testImplementation(Dependencies.Deps.jUnit)
    implementation(Dependencies.Kotlin.stdlib)
    implementation(Dependencies.Deps.materialRipple)
    implementation(Dependencies.Support.cardview)
    implementation(Dependencies.Deps.flexbox)
    implementation(Dependencies.Deps.constraintLayout)
    implementation(Dependencies.Glide.core)
    implementation(Dependencies.Glide.annotations)
    ksp(Dependencies.Glide.ksp)
    implementation(Dependencies.Deps.asyncLayoutInflater)
    implementation(Dependencies.Deps.rxjava)
    implementation(Dependencies.Compose.composeUI)
    implementation(Dependencies.Compose.composeConstraintLayout)
    implementation(Dependencies.Compose.composeMaterialIcons)
}
