import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.washingtonpost.convention"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    compileOnly("com.android.tools.build:gradle:9.1.1")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    implementation("org.yaml:snakeyaml:2.5")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.washingtonpost.application"
            implementationClass = "ApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.washingtonpost.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "com.washingtonpost.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("dependencies") {
            id = "com.washingtonpost.dependencies"
            implementationClass = "com.washingtonpost.convention.Dependencies"
        }
        register("flavors") {
            id = "com.washingtonpost.flavors"
            implementationClass = "FlavorsConventionPlugin"
        }
    }
}
