package com.wapo.flagship.features.posttv.players

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.posttv.R
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.posttv.listeners.VideoListener
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.posttv.vimeo.OnVimeoExtractionListener
import com.wapo.flagship.features.posttv.vimeo.VimeoExtractor
import com.wapo.flagship.features.posttv.vimeo.VimeoVideo

class VimeoPlayerImpl(val context: Context, val listener: VideoListener): PostTvPlayerImpl(
    context,
    listener
) {
    override fun playVideo(video: Video) {
        mVideo = video
        listener.setIsLoading(true)
        VimeoExtractor.getInstance()?.fetchVideoWithURL(video.id, null, object : OnVimeoExtractionListener {
                override fun onSuccess(vimeoVideo: VimeoVideo?) {
                    val streamUrl = getStream(vimeoVideo)
                    if (streamUrl != null) {
                        runOnUiThread {
                            super@VimeoPlayerImpl.playVideo(createVimeoVideo(streamUrl, video))
                            listener.setIsLoading(false)
                        }
                    } else {
                        onFailure(IllegalStateException("Stream not found for ${video.id}"))
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                    Logger.e(TAG, "An error occurred", throwable)
                    runOnUiThread {
                        listener.onError(context.getString(R.string.unknown_error))
                        listener.setIsLoading(false)
                    }
                }
            })
    }

    private fun getStream(vimeoVideo: VimeoVideo?): String? {
        return vimeoVideo?.streams?.get("1080p")
            ?: vimeoVideo?.streams?.get("720p")
            ?: vimeoVideo?.streams?.get("480p")
            ?: vimeoVideo?.streams?.get("360p")
            ?: vimeoVideo?.streams?.get("270p")
    }

    private fun createVimeoVideo(videoUrl: String?, video: Video): Video {
        return Video.Builder()
            .setId(videoUrl)
            .setContentUrl(video.contentUrl)
            .setShareUrl(video.shareUrl)
            .setHeadline(video.headline)
            .setDuration(video.duration)
            .setIsLive(video.isLive)
            .setPageName(video.pageName)
            .setVideoName(video.videoName)
            .setVideoSection(video.videoSection)
            .setVideoSource(video.videoSource)
            .setVideoCategory(video.videoCategory)
            .setShouldPlayAds(video.shouldPlayAds)
            .setContentId(video.contentId)
            .setSubtitleUrl(video.subtitleUrl)
            .setAdTagUrl(video.adTagUrl)
            .setFallbackUrl(video.fallbackUrl)
            .setSource(video.source)
            .build()
    }

    private fun runOnUiThread(impl: () -> Unit) {
        val activity = (context as PostTvApplication).currentActivity
        if (activity != null && !activity.isFinishing) {
            activity.runOnUiThread {
                impl()
            }
        }
    }

    companion object {
        private val TAG: String = VimeoPlayerImpl::class.java.simpleName
    }
}