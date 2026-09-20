package com.washingtonpost.android.paywall.reminder.acquisition

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderCtaDestination
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderModel
import com.washingtonpost.android.config.domain.models.config.paywall.SubscriberDataModel
import com.washingtonpost.android.paywall.features.promocodes.viemodels.ViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
internal class AcquisitionReminderViewModelTest : ViewModelTest() {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharePrefs: SharedPreferences

    @Mock
    private lateinit var applicationContext: Context

    private val systemClockMock = Mockito.mockStatic(SystemClock::class.java)

    private lateinit var model: AcquisitionReminderModel
    private lateinit var storage: AcquisitionReminderStorage

    private val elapsedTimeLessThanFrequency = 20000L
    private val elapsedTimeGreaterThanFrequency = 40000L
    private val storedTime = 0L

    @Before
    override fun setUp() {
        super.setUp()
        model = AcquisitionReminderModel(
            SubscriberDataModel("newSubMessage", "newSubHeading"),
            SubscriberDataModel("terminatedSubMessage", "terminatedSubHeading"),
            "wp.classic.basic",
            AcquisitionReminderCtaDestination.PAYWALL,
            true,
            frequency = 30000
        )

        storage = AcquisitionReminderStorage.getInstance(context)

        `when`(context.applicationContext).then { applicationContext }
        `when`(
            applicationContext.getSharedPreferences(
                AcquisitionReminderStorage.PREFS_NAME,
                Context.MODE_PRIVATE
            )
        ).then { sharePrefs }

    }


    @Test
    fun areConditionsMet() {
        // showMessage = False - enough time has NOT elapsed for Non-Subscriber
        assert(
            !areConditionsMet(
                elapsedTimeLessThanFrequency,
                isPremiumUser = false,
                isTerminated = false
            )
        )
        // showMessage = True - elapsed time is greater than frequency for Non-Subscriber
        assert(
            areConditionsMet(
                elapsedTimeGreaterThanFrequency,
                isPremiumUser = false,
                isTerminated = false
            )
        )
        // showMessage = False - User is a subscriber
        assert(
            !areConditionsMet(
                elapsedTimeGreaterThanFrequency,
                isPremiumUser = true,
                isTerminated = false
            )
        )
        // showMessage = True - User is a terminated Subscriber
        assert(
            !areConditionsMet(
                elapsedTimeGreaterThanFrequency,
                isPremiumUser = true,
                isTerminated = true
            )
        )
    }

    private fun areConditionsMet(
        elapsedTime: Long,
        isPremiumUser: Boolean,
        isTerminated: Boolean
    ): Boolean {
        `when`(paywallService.isPremiumUser).then { isPremiumUser }
        `when`(paywallService.isSubscriptionTerminated).then { isTerminated }
        `when`(
            sharePrefs.getLong(
                AcquisitionReminderStorage.PREF_ACQ_REMINDER_SHOWN_TIME,
                -1
            )
        ).then { storedTime }

        systemClockMock.`when`<Any> { SystemClock.elapsedRealtime() }
            .thenReturn(elapsedTime)

        val showMessage = AcquisitionReminderViewModel.areConditionsMet(model, storage, false)

        println("storedTime = $storedTime | elapsedTime = $elapsedTime | frequency = ${model.frequency} | isPremiumUser = $isPremiumUser | isTerminated = $isTerminated")
        println("showMessage = $showMessage")
        return showMessage
    }


}