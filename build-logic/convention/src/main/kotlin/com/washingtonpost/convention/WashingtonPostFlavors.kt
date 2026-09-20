package com.washingtonpost.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project

enum class FlavorDimensions {
    default
}

enum class WashingtonPostFlavors(
    val appId: String, 
    val changeLogKeys: VersionKeys, 
    val envKeys: VersionKeys,
    val buildConfigFields: Array<BuildConfigField>
) {
    amazon(
        "com.washingtonpost.rainbow",
        VersionKeys(Keys.AMAZON_VERSION_CODE_CHANGE_LOG, Keys.AMAZON_VERSION_NAME_CHANGE_LOG),
        VersionKeys(Keys.AMAZON_VERSION_CODE, Keys.AMAZON_VERSION_NAME),
        arrayOf(
            BuildConfigField("String", "STORE_TYPE", "\"amazon\""), 
            BuildConfigField("String", "BASE_URL", "\"https://rainbowapi-stage.wpdigital.net/rainbow-data-service/rainbow/\""),
            BuildConfigField("String", "SUBS_BASE_URL", "\"https://subscribe.washingtonpost.com/\""),
            BuildConfigField("String", "SEARCH_BASE_URL", "\"https://tabletapi.washingtonpost.com/apps-data-service/\""),
            BuildConfigField("String", "RECIPE_BASE_URL", "\"https://washpost-apps-origin-dev.ext.nile.works/recipe-search/api/v0/\"")

        )
    ),
    playstore(
        "com.washingtonpost.android",
        VersionKeys(Keys.PLAY_STORE_VERSION_CODE_CHANGE_LOG, Keys.PLAY_STORE_VERSION_NAME_CHANGE_LOG),
        VersionKeys(Keys.PLAY_STORE_VERSION_CODE, Keys.PLAY_STORE_VERSION_NAME),
        arrayOf(
            BuildConfigField("String", "STORE_TYPE", "\"playstore\""),
            BuildConfigField("String", "BASE_URL", "\"https://rainbowapi-stage.wpdigital.net/rainbow-data-service/rainbow/\""),
            BuildConfigField("String", "SUBS_BASE_URL", "\"https://subscribe.washingtonpost.com/\""),
            BuildConfigField("String", "SEARCH_BASE_URL", "\"https://tabletapi.washingtonpost.com/apps-data-service/\""),
            BuildConfigField("String", "RECIPE_BASE_URL", "\"https://tabletapi.washingtonpost.com/apps-data-service/recipe-search.json\"")
        )
    )
}

data class VersionKeys(
    val versionCode: String,
    val versionName: String
)

data class BuildConfigField(
    val type: String,
    val name: String,
    val value: String
)


fun Project.configureFlavors(applicationExtension: ApplicationExtension) {
    with(applicationExtension) {
        FlavorDimensions.values().forEach {
            flavorDimensions += it.name
        }

        WashingtonPostFlavors.values().forEach {
            productFlavors.create(it.name) {
                val versionConfig = getVersionData(it)
                versionCode = versionConfig.versionCode
                versionName = versionConfig.versionName
                applicationId = it.appId

                it.buildConfigFields.forEach { field ->
                    with(field) {
                        buildConfigField(type, name, value)
                    }
                }
            }
        }
    }
}

fun configureFlavors(libraryExtension: LibraryExtension) {
    with(libraryExtension) {
        FlavorDimensions.values().forEach {
            flavorDimensions += it.name
        }

        WashingtonPostFlavors.values().forEach {
            productFlavors.create(it.name) {
                dimension = FlavorDimensions.default.name
            }
        }
    }
}

fun configureVariants(libraryExtension: LibraryExtension) {
    libraryExtension.buildTypes {
        create("beta") {
            initWith(getByName("release"))
        }
    }
}
