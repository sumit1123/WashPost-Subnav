package com.wapo.flagship.features.posttv.vimeo
import java.io.IOException


/**
 * A class used to extract Vimeo video information
 * through an all-digit video identifier or a full video URL.
 *
 * Information includes stream urls, title and duration.
 *
 * See [VimeoVideo] for full information available
 */
class VimeoExtractor private constructor() {
    /**
     * Get Video stream information using its identifier
     * @param identifier Non-null numeric video identifier (e.g. 123456)
     * @param referrer Video referrer URL. Leaving as null provides referrer as video url by default
     * @param listener Callback from extraction
     */
    private fun fetchVideoWithIdentifier(
        identifier: String,
        referrer: String?,
        listener: OnVimeoExtractionListener
    ) {
        //If an invalid identifier is entered, throw an error
        if (identifier.isEmpty()) {
            listener.onFailure(IllegalArgumentException("Video identifier cannot be empty"))
            return
        }
        val manager = VimeoApiManager()
        try {
            manager.extractWithIdentifier(identifier, referrer)
                .enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        listener.onFailure(e)
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        //Check if response is successful
                        if (response.isSuccessful) {
                            //Generate video object from JSON response
                            val vimeoVideo = VimeoVideo(response.body?.string())
                            listener.onSuccess(vimeoVideo)
                        } else {
                            //Generate an appropriate error
                            listener.onFailure(manager.getError(response))
                        }
                    }
                })
        } catch (e: IOException) {
            listener.onFailure(e)
            e.printStackTrace()
        }
    }

    /**
     * Get Video stream information from its full URL
     * @param videoURL Video URL
     * @param referrer Video referrer URL
     * @param listener Callback from extraction
     */
    fun fetchVideoWithURL(
        videoURL: String,
        referrer: String?,
        listener: OnVimeoExtractionListener
    ) {
        //Check for valid URL length
        if (videoURL.isEmpty()) {
            listener.onFailure(IllegalArgumentException("Video URL cannot be empty"))
            return
        }
        val parser = VimeoParser(videoURL)
        //Determine if Vimeo URL is valid
        if (!parser.isVimeoURLValid) {
            listener.onFailure(IllegalArgumentException("Vimeo URL is not valid"))
            return
        }

        //Extract identifier from URL
        val identifier = parser.extractedIdentifier
        //Get Video stream information using its identifier
        fetchVideoWithIdentifier(identifier, referrer, listener)
    }

    companion object {
        /**
         * Get singleton instance of the extractor
         * @return singleton instance
         */
        private var instance: VimeoExtractor? = null
        fun getInstance(): VimeoExtractor? {
            if (instance == null) {
                instance = VimeoExtractor()
            }
            return instance
        }
    }
}
