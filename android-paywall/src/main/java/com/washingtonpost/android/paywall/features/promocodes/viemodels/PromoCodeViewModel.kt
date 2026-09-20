package com.washingtonpost.android.paywall.features.promocodes.viemodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import com.wapo.android.commons.util.LiveEvent
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.models.PromoCode
import com.washingtonpost.android.paywall.models.PromoCodeJsonAdapter
import com.washingtonpost.android.paywall.models.PromoCodeRequestState
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * For retrieving the promo code either from the verify device subscription call OR shared prefs.
 */
class PromoCodeViewModel: ViewModel() {

    private val dateFormat = "yyyy-MM-dd HH:mm:ss"

    private val _promoCodeRequestState = LiveEvent<PromoCodeRequestState>()
    val promoCodeRequestState: LiveData<PromoCodeRequestState> = _promoCodeRequestState

    /**
     * Call this function for a primary attempt to retrieve promo code.
     * WARNING: Make sure that you're calling this function only once during the lifecycle of a fragment (if vm scoped to fragment's lifecycle)
     * or an activity (if vm scoped to activity's lifecycle) since adding the same source twice will cause the app crash.
     *
     * If you need to make another request for promo code for whatever reason (e.g. network issues),
     * please consider calling [tryAgainClicked] (If necessary change the function name [tryAgainClicked] to something generic)
     */
    fun startPromoCodeRetrieval() {
        _promoCodeRequestState.addSource(PaywallService.getInstance().promoCodeRequestLiveData) { result ->
            if (result is PromoCodeRequestState.Success) {
                PaywallPrefHelper.setPromoCode(
                    PromoCodeJsonAdapter(Moshi.Builder().build()).toJson(result.promocode)
                )
            }
            _promoCodeRequestState.value = result
        }
        checkForPromoCode()
    }

    /**
     * If primary attempt fails, use this function to make all of the subsequent attempts (if the lifecycle owner has not changed)
     */
    fun tryAgainClicked(){
        checkForPromoCode()
    }

    /**
     * Checks the promo code availability in the shared prefs before making the verify device subscription call.
     */
    private fun checkForPromoCode(){
        val serializedPromoCode: String? = PaywallPrefHelper.getPromoCode()
        val validatedPromoCode = validateStoredPromoCode(serializedPromoCode)
        if(validatedPromoCode != null){
            _promoCodeRequestState.value = PromoCodeRequestState.Success(validatedPromoCode)
        }else{
            PaywallService.getInstance().verifyDeviceSubscription(true, true, false)
        }
    }

    /**
     * Validates the promo code stored in shared prefs.
     * [promoCodeSerialized] - Serialized promo code data that was stored in shared prefs.
     */
    private fun validateStoredPromoCode(promoCodeSerialized: String?): PromoCode?{
        if(promoCodeSerialized != null) {
            val promoCode: PromoCode? =
                PromoCodeJsonAdapter(Moshi.Builder().build()).fromJson(promoCodeSerialized)
            if (promoCode != null && isWithinThePromoOfferTime(promoCode.startDate, promoCode.endDate)){
                return promoCode
            }
        }
        return null
    }

    /**
     * Checks if the promo code is not expired and/or the promo offer has started.
     * Note: The dates provided should be in [dateFormat] in order for this to evaluate the validity of the promo code.
     * For different date formats this function will always return false
     * [startDate] - Start date of the promotional offer.
     * [endDate] - End date of the promotional offer.
     */
    private fun isWithinThePromoOfferTime(startDate: String?, endDate: String?): Boolean{
        if(startDate == null || endDate == null)
            return false
        try{
            val format = SimpleDateFormat(dateFormat, Locale.US)
            val startDateFormatted = format.parse(startDate)
            val endDateFormatted = format.parse(endDate)
            val currentDate = Date()
            if(startDateFormatted == null || endDateFormatted == null)
                return false
            return currentDate.before(endDateFormatted) && currentDate.after(startDateFormatted)
        }catch (ex: Exception){
            return false
        }
    }

}