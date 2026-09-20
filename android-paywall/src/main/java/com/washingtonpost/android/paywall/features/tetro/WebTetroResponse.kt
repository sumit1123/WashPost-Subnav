package com.washingtonpost.android.paywall.features.tetro

import com.google.gson.annotations.SerializedName

/**
 * This model is specifically for Tetro responses passed back from Webviews to JS Interface function.
 * This is not to be used for handling Native API responses.
 *
 * Sample Response:
 * {
        "status": "final",
        "action": 3,
        "data": {
            "isSubscriber": false,
            "isPreview": false,
            "userAttributes": {
                "isTerminated": false
            },
            "ruleSetVersion": 1,
            "isSignedIn": false,
            "meterCount": "meter_paywall",
            "actionCodes": [
            "1600",
            "1699",
            "60",
            "65",
            "31"
            ],
            "articleWeight": 1,
            "wallMeterType": "",
            "wapoLoginId": "",
            "token": {
                "info": "2833246"
            },
            "freeTrialConsumptionCount": -99,
            "articleWeightMachine": 1,
            "requestId": "265a8314-eda2-400a-86b9-49f97bccf691",
            "action": 3,
            "articleWeightManual": 1,
            "wapoActmgmt": "",
            "meterState": 1,
            "refererType": "DARK"
        },
        "granted": false,
        "statusCode": 200,
        "listeningTo": "v2"
        }

 * Note: Not all response values are used and thus the model only contains what is needed.
 * - Currently handled actions from webview
 * - Action 3 - Paywall
 * - Action 6 - Softwall
 */
data class WebTetroResponse(
    @SerializedName("action") val action: Int,
    @SerializedName("data") val data: WebTetroResponseForData
)

/**
 * Action codes are used to determine gift state from Webview response.
 * - Currently handled actions
 * - w_1610 -> Valid not expired Gift
 * - w_1698 -> Expired Gift
 */
data class WebTetroResponseForData(
    @SerializedName("actionCodes") val actionCodes: List<String>,
    @SerializedName("prompts") val prompts: List<Prompt>? = null
)
