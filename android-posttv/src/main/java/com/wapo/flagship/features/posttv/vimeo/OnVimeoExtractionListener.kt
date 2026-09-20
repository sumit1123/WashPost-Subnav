package com.wapo.flagship.features.posttv.vimeo


/**
 * Callback from Vimeo Stream extraction, providing video information on success
 * or error information on failure.
 */
interface OnVimeoExtractionListener {
    /**
     * Returns a [VimeoVideo] object relating to the extraction
     * @param video the Vimeo video
     */
    fun onSuccess(video: VimeoVideo?)

    /**
     * Returns when an error occurs during extractions
     * @param throwable the error object
     */
    fun onFailure(throwable: Throwable?)
}