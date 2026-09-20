package com.wapo.flagship

import android.app.Application
import com.wapo.android.commons.util.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.config.SiteServiceConfig
import com.wapo.flagship.content.SectionSubtype
import com.wapo.flagship.features.grid.*
import com.wapo.flagship.util.UIUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject

@HiltViewModel
class TestStorageViewModel @Inject constructor(
    private val application: Application,
) : ViewModel() {
    val tag = "TestStorageVM"

    fun refreshMetrics() {
        viewModelScope.launch {
            logMetrics()
        }
    }

    private suspend fun logMetrics() {
        withContext(Dispatchers.IO) {
            val contentManager = FlagshipApplication.getInstance().contentManager
            val cacheManager = FlagshipApplication.getInstance().cacheManager

            contentManager.wapoConfigManager?.let { configManager ->

                val isPhone = UIUtil.isPhone(application)
                val barSiteServiceConfig: SiteServiceConfig = configManager.sectionsBarConfig
                val menuSections =
                    barSiteServiceConfig
                        .getSectionsAsMenuSections(
                            barSiteServiceConfig.sections,
                            isPhone,
                            true,
                            false,
                        )?.toMutableList()

                // val featuredSiteServiceConfig: SiteServiceConfig = configManager.sectionsFeaturedConfig
                // menuSections?.addAll(featuredSiteServiceConfig.getSectionsAsMenuSections(featuredSiteServiceConfig.sections, isPhone, true) as? ArrayList
                //        ?: emptyList())

                val azSiteServiceConfig: SiteServiceConfig = configManager.sectionsAZConfig
                menuSections?.addAll(
                    azSiteServiceConfig.getSectionsAsMenuSections(
                        azSiteServiceConfig.sections,
                        isPhone,
                        true,
                        false,
                    ) as? ArrayList
                        ?: emptyList(),
                )

                var totalTextDataSize: Long = 0
                var totalImageDataSize: Long = 0
                menuSections
                    ?.flatMap { listOf(it) + it.sectionInfo }
                    ?.forEach { menuSection ->
                        contentManager.getPageUrlObs(menuSection.bundleName).toBlocking().first().let { url ->
                            val json =
                                String(
                                    cacheManager.get(url)?.data
                                        ?: byteArrayOf(),
                                    Charset.defaultCharset(),
                                )
                            var textDataSize: Long = 0
                            var imageDataSize: Long = 0
                            when (contentManager.getSectionSubtype(menuSection.bundleName)) {
                                SectionSubtype.FUSION -> {
                                    val page =
                                        FusionMapper.gson.fromJson(
                                            json,
                                            GridEntity::class.java,
                                        )
                                    if (page != null) {
                                        getFusionArticleUrls(page).forEach { articleUrl ->
                                            val size =
                                                cacheManager.get(articleUrl)?.data?.size ?: 0
                                            textDataSize += size
                                            // LogUtil.i(tag, "Article - articleTextSize=$size, section=${menuSection.bundleName}(Fusion), url=$articleUrl")
                                        }
                                        getFusionImageUrls(page).forEach { imageUrl ->
                                            cacheManager.get(imageUrl)?.data?.let { data ->
                                                val size = File(String(data)).length()
                                                imageDataSize += size
                                                // LogUtil.i(tag, "Article - imagesSize=$size, section=${menuSection.bundleName}(Fusion), url=$imageUrl")
                                            }
                                        }
                                    }
                                    Logger.d(
                                        tag,
                                        "Section(Fusion) - sectionTextSize=${
                                            cacheManager.get(url)?.data?.size
                                        }, articlesTextSize=$textDataSize, imageDataSize=$imageDataSize, section=${menuSection.bundleName}, url=$url",
                                    )
                                }
                                else -> {
                                        /*
                                            Do nothing
                                         */
                                }
                            }
                            totalTextDataSize += cacheManager.get(url)?.data?.size ?: 0
                            totalTextDataSize += textDataSize
                            totalImageDataSize += imageDataSize
                        }
                    }
                Logger.d(
                    tag,
                    "totalTextDataSize=$totalTextDataSize, totalImageDataSize=$totalImageDataSize, totalDataSize=${totalTextDataSize + totalImageDataSize}",
                )
            }
        }
    }

    private fun getFusionArticleUrls(fusionPage: GridEntity): ArrayList<String> {
        val articleUrls = ArrayList<String>()

        fusionPage.regions
            .flatMap { it.items }
            .filterIsInstance<ChainEntity>()
            .flatMap { it.items }
            .filterNotNull()
            .flatMap { it.items }
            .filterIsInstance<HomepageStoryEntity>()
            .forEach { feature ->
                val link = feature.link
                val fallbackLink = feature.offlineLink
                if (isLinkDownloadable(fallbackLink)) {
                    fallbackLink?.url?.let { articleUrls.add(it) }
                } else if (isLinkDownloadable(link)) {
                    link?.url?.let { articleUrls.add(it) }
                }
            }
        return articleUrls
    }

    private fun getFusionImageUrls(fusionPage: GridEntity): ArrayList<String> {
        val imageUrls = ArrayList<String>()
        fusionPage.regions
            .flatMap { it.items }
            .filterIsInstance<ChainEntity>()
            .flatMap { it.items }
            .filterNotNull()
            .flatMap { it.items }
            .filterIsInstance<HomepageStoryEntity>()
            .forEach {
                val url = it.media?.url
                if (url != null) {
                    imageUrls.add(url)
                }
            }

        return imageUrls
    }

    private fun isLinkDownloadable(link: LinkEntity?): Boolean =
        link?.url != null &&
            (link.type == LinkTypeEntity.ARTICLE)
}
