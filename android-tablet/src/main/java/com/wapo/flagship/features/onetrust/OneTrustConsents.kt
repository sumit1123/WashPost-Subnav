package com.wapo.flagship.features.onetrust

/**
 *  OT custom identifiers for different categories
 *  [groupId] Custom Group Identifier associated with the specific category.
 *  Strictly Necessary is always active and cannot be disabled.
 *      Things like Dagger and Splunk are considered Strictly Necessary.
 *      Parts of SDKs are considered Strictly Necessary - for example, OneLink for deeplinking with AppsFlyer.
 *  Performance controls analytics - Firebase and Chartbeat.
 *  Targeting controls personalization - current implementation covers ads, AppsFlyer tracking, RTE, For You, IAA.
 *  Functionality has no current implementation in apps but must be present to sync consent with site.
 *      For You might have been categorized as Functionality but could not be separated from the rest of RTE.
 *  Social Media has no current implementation in apps but must be present to sync consent with site.
 */
enum class OneTrustConsents(
    val groupId: String,
) {
    STRICTLY_NECESSARY(groupId = "C0001"),
    PERFORMANCE(groupId = "C0002"),
    FUNCTIONALITY(groupId = "C0003"),
    TARGETING(groupId = "C0004"),
    SOCIAL_MEDIA(groupId = "C0005"),
}
