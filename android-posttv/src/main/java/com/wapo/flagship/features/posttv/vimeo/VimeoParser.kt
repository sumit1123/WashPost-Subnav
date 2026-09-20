package com.wapo.flagship.features.posttv.vimeo



/**
 * Parser for a given Vimeo Link
 */
class VimeoParser
/**
 * Initialise VideoParser with url
 * @param url Vimeo Video url
 */(
    /**
     * Get the URL stored by parser
     * @return the url
     */
    //Full URL of Vimeo video
    val url: String
) {

    /**
     * Check if a Vimeo URL has a valid identifier
     * @return true if identifier is valid, false otherwise
     */
    val isVimeoURLValid: Boolean
        get() {
            val videoID = extractedIdentifier
            return videoID.isNotEmpty() && VimeoUtils.isDigitsOnly(videoID)
        }

    /**
     * Get a Vimeo identifier from the url
     * @return Vimeo identifier if found and an empty string otherwise
     */
    val extractedIdentifier: String
        get() {
            val urlParts = url.split("/").toTypedArray()
            return if (urlParts.isEmpty()) {
                ""
            } else urlParts[urlParts.size - 1]
        }

}
