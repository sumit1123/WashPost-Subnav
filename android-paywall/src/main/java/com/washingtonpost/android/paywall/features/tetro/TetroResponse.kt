package com.washingtonpost.android.paywall.features.tetro

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * This is the model we use to parse Tetro API response for handling metering of native articles
 *
 * Sample Response:
 *
 * {
        "status": "SUCCESS",
        "data": {
            "meterCount": 0,
            "meterLimit": 2,
            "freeTrialConsumptionCount": -99,
            "meterState": 20,
            "meterCycleDays": 45,
            "weightedArticles": {
                "/sports/2021/06/22/sally-jenkins-supreme-court-ncaa-decision/": {
                "0": "5"
                },
                "/nation/2021/06/22/valerie-bacot-france-abuse-husband-stepfather/": {
                "0": "2"
                },
                ...
        }
    }
}
 */
data class TetroResponse(
    @SerializedName("status") val status: String,
    @SerializedName("action") val action: Int,
    @SerializedName("granted") val granted: Boolean,
    @SerializedName("grantReason") val grantedReason: String,
    @SerializedName("data") val data: TetroResponseForData
)

/**
 * This data holds metering state
 * - meterCount -> Current meter count of articles read
 * - meterLimit -> Max limit before use hits paywall
 * - weightedArticles -> map of articles with weights
 * - freeTrialConsumptionCount -> Number of free articles remaining. Ignores metering/article weights.
 *      Default value is -99. Currently only for Reddit free trial promo.
 */
data class TetroResponseForData(
    @SerializedName("meterCount") val meterCount: Int,
    @SerializedName("meterLimit") val meterLimit: Int,
    @SerializedName("freeTrialConsumptionCount") val freeTrialConsumptionCount: Int,
    @SerializedName("articleWeight") val articleWeight: Int,
    @SerializedName("requestId") val requestId: String,
    @SerializedName("wapoActmgmt") val wapoActmgmt: String,
    @Expose
    @SerializedName("weightedArticles") val weightedArticles: HashMap<String, Map<String, String>>,
    @SerializedName("meterState") val meterState: Int,
    @SerializedName("meterCycleDays") val meterCycleDays: Int,
    @Expose
    @SerializedName("articles") val articles: HashMap<String, Map<String, String>>,
    @SerializedName("actionCodes") val actionCodes: List<String>,
    @SerializedName("prompts") val prompts: List<Prompt>?
)

data class Prompt(
    @SerializedName("id") val id: String?,
    @SerializedName("itid") val itid: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("promo") val promo: Promo?,
    @SerializedName("trigger") val trigger: Trigger?,
    @SerializedName("menu") val menu: Boolean? = false,
    @SerializedName("appearance") val appearance: Appearance?
)

data class Promo(
    @SerializedName("logo") val logo: String?,
    @SerializedName("labels") val labels: List<String>?
)

data class Trigger(
    @SerializedName("type") val type: String?,
    @SerializedName("depth") val depth: Int?
)

/**
 * @param dismiss Dismiss button implementation (client vs. server)
 * @param dismissWalls Names of the walls to dismiss, defaults to ["current"] for server side dismiss
 * @param dismissLifespanSeconds Used to calculate wall-dismissal uxp
 * @param dismissExpirationSeconds Minimum time interval between dismissal and next appearance. Also used to calculate wall-dismissal exp
 * @param maxSnooze Maximum number of user dismissals after which MAP wall never appears
 */
data class Appearance(
    @SerializedName("dismiss") val dismiss: String?,
    @SerializedName("dismissWalls") val dismissWalls: List<String>?,
    @SerializedName("dismissLifespan") val dismissLifespanSeconds: Long?,
    @SerializedName("dismissExpirationSeconds") val dismissExpirationSeconds: Long?,
    @SerializedName("maxSnooze") val maxSnooze: Int? = Int.MAX_VALUE
)

sealed class TetroState {
    class Success(val data: TetroResponse) : TetroState()
    class Failure(val message: String) : TetroState()
    class Exception(val message: String) : TetroState()
}

enum class TriggerType(val value: String) {
    SCROLL("scroll")
}

enum class DismissType(val value: String) {
    CLIENT("client"),
    SERVER("server")
}
