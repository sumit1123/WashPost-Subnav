package com.wapo.flagship.features.posttv.vimeo

import org.json.JSONException
import org.json.JSONObject


/**
 * An information extracted Vimeo video containing details about
 * stream quality, stream links, duration and video title
 */
class VimeoVideo constructor(json: String?) {
    /**
     * Video title
     * @return the video title
     */
    //Video title
    var title: String? = null
        private set

    /**
     * Video duration in seconds
     * @return the video duration
     */
    //Video length in seconds
    var duration: Long = 0
        private set

    //Stream information with key being quality name (e.g. 1080p) and value stream url
    val streams = HashMap<String, String>()

    //Stream thumbnails with key being quality and value url of image
    private val thumbs: MutableMap<String, String>

    /**
     * Get information on the user that created / uploaded the video
     * @return VimeoUser object containing information on the user
     */
    //User that created video
    var videoUser: VimeoUser? = null
        private set

    private fun parseJson(json: String?) {
        try {
            //Turn JSON string to object
            val requestJson = JSONObject(json)

            //Access video information
            val videoInfo = requestJson.getJSONObject("video")
            duration = videoInfo.getLong("duration")
            title = videoInfo.getString("title")

            //Get user information
            val userInfo = videoInfo.getJSONObject("owner")
            videoUser = VimeoUser(userInfo)

            //Get thumbnail information
            val thumbsInfo = videoInfo.getJSONObject("thumbs")
            val iterator: Iterator<String>
            iterator = thumbsInfo.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                thumbs[key] = thumbsInfo.getString(key)
            }

            //Access video stream information
            val streamArray = requestJson.getJSONObject("request")
                .getJSONObject("files")
                .getJSONArray("progressive")

            //Get info for each stream available
            for (streamIndex in 0 until streamArray.length()) {
                val stream = streamArray.getJSONObject(streamIndex)
                val url = stream.getString("url")
                val quality = stream.getString("quality")
                //Store stream information
                streams[quality] = url
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * Check if given video has stream information
     * @return true if information is present, false otherwise
     */
    fun hasStreams(): Boolean {
        return streams.size > 0
    }

    /**
     * Check if video has HD stream available
     * @return true if 1080 or 4096p streams are available, false otherwise
     */
    val isHD: Boolean
        get() = streams.containsKey("1080p") || streams.containsKey("4096p")

    /**
     * Check if video has associated thumbnails
     * @return true if thumbnails are present; false otherwise
     */
    fun hasThumbs(): Boolean {
        return thumbs.size > 0
    }

    /**
     * Get thumbnail information in the form of a key-value map.
     * Keys are the quality information of the thumbnail (e.g. base, 640, 1280)
     * The default key returned from Vimeo's API is "base"
     * Values are the corresponding thumbnail image URL
     * @return Map of available thumbnails for video
     */
    fun getThumbs(): Map<String, String> {
        return thumbs
    }

    //Initialise VimeoVideo from JSON
    init {
        thumbs = HashMap()
        parseJson(json)
    }
}
