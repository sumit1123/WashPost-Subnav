plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.skie)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kmmbridge)
    `maven-publish`
}

group = (findProperty("GROUP")?.toString() ?: "com.wapo.kmpshared").lowercase()
val isCI = System.getenv("GITHUB_ACTIONS") == "true"
val baseVersion = findProperty("LIBRARY_VERSION")?.toString() ?: "0.0.1"
version = if (isCI) baseVersion else "local-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

kotlin {
    val xcframeworkName = "KMPShared"

    androidLibrary {
        namespace = "com.wapo.kmpshared"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = xcframeworkName
            binaryOption("bundleId", "org.washingtonpost.$xcframeworkName")
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.view.model)
            implementation(libs.ktor.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.stately.collections)
            implementation(libs.stately.concurrency)
            api(libs.koin.annotations)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        iosMain.dependencies {
            implementation(libs.ktor.darwin)
        }

        androidMain.dependencies {
            implementation(libs.ktor.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
    }

    // KSP Common sourceSet
    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.concurrent.atomics.ExperimentalAtomicApi")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "kmpshared"
            version = project.version.toString()
            from(components["kotlin"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/WashPost/kmp-classic")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }

        // local dev repo
        maven {
            name = "LocalMaven"
            url = uri("${rootProject.projectDir}/local-repo")
        }
    }
}

skie {
    build {
        produceDistributableFramework()
    }
}

kmmbridge {
    frameworkName.set("KMPShared")
    gitHubReleaseArtifacts()
    spm(swiftToolVersion = "6.2") {
        iOS { v("17") }
    }
}

// KSP Tasks
dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
    add("kspAndroid", libs.koin.ksp.compiler)
    add("kspIosX64", libs.koin.ksp.compiler)
    add("kspIosArm64", libs.koin.ksp.compiler)
    add("kspIosSimulatorArm64", libs.koin.ksp.compiler)
}

project.tasks.configureEach {
    if ((
            name.startsWith("ksp") || (name == "sourcesJar") ||
                (name == "prepareAndroidMainArtProfile")
        ) && name != "kspCommonMainKotlinMetadata"
    ) {
        dependsOn("kspCommonMainKotlinMetadata")
    }

    if (name == "prepareAndroidMainArtProfile") {
        dependsOn("kspAndroidMain")
    }
}
