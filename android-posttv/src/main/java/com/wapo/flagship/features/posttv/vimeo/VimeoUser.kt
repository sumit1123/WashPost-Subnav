package com.wapo.flagship.features.posttv.vimeo

import org.json.JSONObject

/**
 * Created by edgeorge on 18/01/2017.
 */
class VimeoUser {
    /**
     * Get account type of user - e.g. plus, basic
     * @return Account type of user
     */
    // Account type for user
    var accountType: String? = null
        private set

    /**
     * Get full name of user
     * @return Name of user
     */
    // Name for user
    var name: String? = null
        private set

    /**
     * Profile image of user
     * @return Image url
     */
    // Image url for user
    var imageUrl: String? = null
        private set

    /**
     * Larger profile image of user
     * @return HQ image url
     */
    // HQ Image url
    var image2xUrl: String? = null
        private set

    /**
     * Profile URL of the user
     * @return url of profile
     */
    // Profile url of user
    var url: String? = null
        private set

    /**
     * Get the Vimeo assigned ID for the user
     * @return id for user
     */
    // User Id
    var id: Long = 0
        private set

    private constructor() {}
    internal constructor(userObject: JSONObject) {
        accountType = userObject.optString("account_type")
        name = userObject.optString("name")
        imageUrl = userObject.optString("img")
        image2xUrl = userObject.optString("img_2x")
        url = userObject.optString("url")
        id = userObject.optLong("id")
    }
}