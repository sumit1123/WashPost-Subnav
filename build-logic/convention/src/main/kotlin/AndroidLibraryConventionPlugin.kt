import com.android.build.api.dsl.LibraryExtension
import com.washingtonpost.convention.configureKotlinAndroid
import com.washingtonpost.convention.configureVariants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("com.washingtonpost.dependencies")
                apply("com.washingtonpost.flavors")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)

                packaging {
                    resources {
                        excludes += "META-INF/DEPENDENCIES"
                        excludes += "META-INF/LICENSE"
                        excludes += "META-INF/LICENSE.txt"
                        excludes += "META-INF/license.txt"
                        excludes += "META-INF/NOTICE"
                        excludes += "META-INF/NOTICE.txt"
                        excludes += "META-INF/notice.txt"
                        excludes += "META-INF/ASL2.0"
                        excludes += "META-INF/*.kotlin_module"
                    }
                }

                buildFeatures {
                    viewBinding = true
                    buildConfig = true
                }

                configureVariants(this)

                useLibrary("android.test.base")
                useLibrary("android.test.runner")
            }
        }
    }
}