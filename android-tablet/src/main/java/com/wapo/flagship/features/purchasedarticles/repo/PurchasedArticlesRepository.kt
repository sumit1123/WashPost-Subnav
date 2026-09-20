package com.wapo.flagship.features.purchasedarticles.repo

import android.os.Build
import com.wapo.android.commons.constants.AUTHORIZATION
import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.CLIENT_APP_VERSION
import com.wapo.android.commons.constants.CLIENT_ID
import com.wapo.android.commons.constants.CLIENT_IP
import com.wapo.android.commons.constants.CLIENT_USER_AGENT
import com.wapo.android.commons.constants.DEVICE_ID
import com.wapo.android.commons.constants.DEVICE_NAME
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.constants.REQUEST_ID
import com.wapo.android.commons.util.ContentType
import com.wapo.flagship.features.purchasedarticles.model.MetadataPurchasedArticleModel
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.save.models.MyPostArticleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class PurchasedArticlesRepository @Inject constructor(
    private val purchasedArticleManager: PurchasedArticleManager
) {
    suspend fun downloadPurchasedArticles() {
        purchasedArticleManager.getPurchasedArticlesFromRemote(getHeaders())
    }

    fun getPurchasedArticles(): Flow<List<MyPostArticleItem>> =
        purchasedArticleManager.getPurchasedArticle().map { list ->
            list.map {
                it.toMyPostArticleItem(list)
            }
        }.flowOn(Dispatchers.IO)
}

fun MetadataPurchasedArticleModel.toMyPostArticleItem(list: List<MetadataPurchasedArticleModel>): MyPostArticleItem {
    return MyPostArticleItem(
        contentType = ContentType.ARTICLE,
        this.url,
        this.headline,
        null,
        null,
        this.label.basic.text,
        this.label.transparency.text,
        this.socialImageUrl,
        this.byLine,
        this.purchasedDate,
        null,
        null,
        list.indexOf(this).toString()
    )
}

private fun getHeaders(): HashMap<String, String> {
    return hashMapOf(
        Pair(
            AUTHORIZATION,
            "Bearer " + PaywallService.getAccessToken(),
        ),
        Pair(CLIENT_ID, PaywallService.getConnector().clientId),
        Pair(CLIENT_IP, PaywallService.getConnector().ipAddress),
        Pair(CLIENT_APP, PaywallService.getConnector().appName),
        Pair(REQUEST_ID, UUID.randomUUID().toString()),
        Pair(DEVICE_ID, PaywallService.getConnector().deviceId),
        Pair(CLIENT_USER_AGENT, PaywallService.getConnector().userAgent),
        Pair(CLIENT_APP_VERSION, PaywallService.getConnector().appVersion),
        Pair(OS_VERSION, Build.VERSION.SDK_INT.toString()),
        Pair(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL),
    )
}