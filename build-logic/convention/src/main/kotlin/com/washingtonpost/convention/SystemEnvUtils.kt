package com.washingtonpost.convention

import org.gradle.api.Project
import org.yaml.snakeyaml.Yaml

import java.io.File

object Keys {
    const val PLAY_STORE_VERSION_CODE = "PLAYSTORE_VERSION_CODE"
    const val AMAZON_VERSION_CODE = "AMAZON_VERSION_CODE"
    const val PLAY_STORE_VERSION_NAME = "PLAYSTORE_VERSION_NAME"
    const val AMAZON_VERSION_NAME = "AMAZON_VERSION_NAME"
    const val PLAY_STORE_VERSION_CODE_CHANGE_LOG = "playstore_version_code"
    const val AMAZON_VERSION_CODE_CHANGE_LOG = "amazon_version_code"
    const val PLAY_STORE_VERSION_NAME_CHANGE_LOG = "playstore_version_name"
    const val AMAZON_VERSION_NAME_CHANGE_LOG = "amazon_version_name"
    const val GIT_BRANCH = "GIT_BRANCH"
    const val BUILD_NUMBER = "BUILD_NUMBER"
    const val BUILD_TYPE = "BUILD_TYPE"
}

object Constants {
    const val DEVELOPMENT_BRANCH = "develop"
    const val BETA_BUILD_TYPE = "beta"
    const val DEFAULT_VERSION_CODE = 1
}

data class VersionConfig(
    val versionCode: Int,
    val versionName: String
)

fun Project.getVersionData(flavor: WashingtonPostFlavors): VersionConfig {
    val env = System.getenv()
    val changelogStream = File("${rootDir.path}/android-tablet/changelog.yml").inputStream()
    val changelog : Map<String, Any> = Yaml().load(changelogStream)

    val gitBranch = env[Keys.GIT_BRANCH]
    val versionCode: Int = (env[flavor.envKeys.versionCode]?.toIntOrNull() ?: if (gitBranch == Constants.DEVELOPMENT_BRANCH) {
        changelog[flavor.changeLogKeys.versionCode]?.toString()?.toIntOrNull() ?: Constants.DEFAULT_VERSION_CODE
    } else Constants.DEFAULT_VERSION_CODE)

    val envVersionName = env[flavor.envKeys.versionName]
    val versionName: String = if(envVersionName.isNullOrEmpty()) {
        if(gitBranch == Constants.DEVELOPMENT_BRANCH) {
            val isBeta = env[Keys.BUILD_TYPE] == Constants.BETA_BUILD_TYPE
            val buildNumber = env[Keys.BUILD_NUMBER]
            val versionNameSuffix: String = if (isBeta) buildNumber?.let { " ($it)" }.orEmpty() else ""
            (changelog[flavor.changeLogKeys.versionName] as String?)?.plus(versionNameSuffix) ?: getDefaultVersionName(env, flavor)
        } else {
            getDefaultVersionName(env, flavor)
        }
    } else getDefaultVersionName(env, flavor)
    return VersionConfig(versionCode, versionName)
}

fun getDefaultVersionName(env: Map<String, String>, flavor: WashingtonPostFlavors) : String {
    val gitBranch = env[Keys.GIT_BRANCH]
    val branchSuffix: String = gitBranch?.replace("^(tags/)|(origin/)", "-") ?: ""
    val buildNumber = env[Keys.BUILD_NUMBER]
    return "${buildNumber?.let { "build-$it-" }?:""}${flavor.name}$branchSuffix"
}