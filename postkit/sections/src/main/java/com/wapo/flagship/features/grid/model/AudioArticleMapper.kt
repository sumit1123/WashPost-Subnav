package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.*

object AudioArticleMapper {

    fun getAudioArticle(
        audioArticleEntity: AudioArticleEntity?,
        feed: String? = null,
        pageConfig: PageConfig
    ): AudioArticle? {
        audioArticleEntity ?: return null
        return audioArticleEntity.run {
            AudioArticle(
                contentUrl,
                displayDate,
                HomepageStoryMapper.getLabel(label),
                titlePrefix,
                titleSeparator ?: "|",
                title,
                getVoices(voices),
                getHumanVoice(humanVoice),
                preferredVoice,
                getAudioArticleTracking(tracking),
                feed,
                MediaMapper.getMedia(playerMedia, pageConfig),
                InlinePlayerMapper.getInlinePlayer(inlinePlayer),
                caption,
                language = language,
                adConfig = AudioAdMapper.getAudioAdConfig(audioArticleEntity.adConfig),
            )
        }
    }

    private fun getVoices(voicesEntities: List<VoiceEntity>?): List<Voice>? {
        voicesEntities ?: return null
        return voicesEntities.map {
            Voice(
                it.voiceId,
                it.duration,
                it.label,
                it.rawUrl,
                it.adsUrl
            )
        }
    }

    private fun getHumanVoice(humanVoiceEntity: HumanVoiceEntity?): HumanVoice? {
        humanVoiceEntity ?: return null
        return humanVoiceEntity.run {
            HumanVoice(
                sourceFileId,
                caption,
                duration,
                rawUrl,
                adsUrl
            )
        }
    }

    private fun getAudioArticleTracking(audioArticleTrackingEntity: AudioArticleTrackingEntity?): AudioArticleTracking? {
        audioArticleTrackingEntity ?: return null
        return audioArticleTrackingEntity.run {
            AudioArticleTracking(
                arcId,
                authorId,
                authorName,
                authorDesk,
                authorSubdesk,
                trackingTags,
                commercialNode,
                contentCategory,
                contentType,
                pageName,
                firstPublishDate,
                author,
                headline,
                hierarchy,
                section,
                subsection,
                source
            )
        }
    }
}