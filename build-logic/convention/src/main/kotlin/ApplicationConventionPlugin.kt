import com.android.build.api.dsl.ApplicationExtension
import com.washingtonpost.convention.ConfigData
import com.washingtonpost.convention.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ApplicationConventionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("com.washingtonpost.dependencies")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig {
                    targetSdk = ConfigData.targetSdk
                    vectorDrawables.useSupportLibrary = true
                }
                ndkVersion = ConfigData.ndkVversion

                packaging {
                    resources {
                        pickFirsts += "META-INF/LICENSE.txt"
                        pickFirsts += "META-INF/NOTICE.txt"
                        excludes += "META-INF/LICENSE"
                        excludes += "LICENSE.txt"
                        excludes += "assets/databases/dillonDb.db"
                        excludes += "res/drawable-hdpi-v4/ic_launcher.png"
                        excludes += "res/drawable-mdpi-v4/ic_launcher.png"
                        excludes += "res/drawable-xhdpi-v4/ic_launcher.png"
                        excludes += "res/drawable-xxhdpi-v4/ic_launcher.png"
                        excludes += "jsr305_annotations/Jsr305_annotations.gwt.xml"
                        excludes += "third_party/java_src/error_prone/project/annotations/Annotations.gwt.xml"
                        excludes += "third_party/java_src/error_prone/project/annotations/Google_internal.gwt.xml"
                        excludes += "error_prone/Annotations.gwt.xml"
                    }
                }

                buildFeatures {
                    viewBinding = true
                    dataBinding = true
                    buildConfig = true
                }

                useLibrary("org.apache.http.legacy")
            }
        }
    }
}