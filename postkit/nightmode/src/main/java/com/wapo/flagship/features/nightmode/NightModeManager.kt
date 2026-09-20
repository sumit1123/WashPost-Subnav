package com.wapo.flagship.features.nightmode

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicInteger

class NightModeManager @JvmOverloads constructor(
        context: Context,
        private val storage: NightModeStorage = DefaultNightModeStorage(context)
) {
    private val lightConditions = BehaviorSubject.create<LightConditions>()
    private val subscibersCounter = AtomicInteger(0)
    private val lightSensor = LightSensorManager(context)

    private val nightModeStatusSubj = PublishSubject.create<Boolean>()

    private val lightListener = object : LightSensorManager.EnvironmentChangedListener {
        override fun onDayDetected() {
            lightConditions.onNext(LightConditions.DAY)
        }

        override fun onNightDetected() {
            lightConditions.onNext(LightConditions.NIGHT)
        }
    }

    val immediateNightModeStatus: Boolean
        get() = storage.isNightModeEnabled()

    init {
        // handle upgrade path from deprecated preference 4.42 -> 4.43
        // prevents night mode from turning off on upgrade and should
        // be removed in a future release
        if (storage.readNightModeStatus()) {
            storage.setUserExplicitlySelectedAMode()
            storage.setNightMode(true)
        }
    }

    fun getNightModeStatus(): Observable<Boolean> {
        return Observable.just(storage.isNightModeEnabled())
                .concatWith(nightModeStatusSubj)
    }

    fun setNightModeStatus(status: Boolean) {
        if (storage.isNightModeEnabled() != status) {
            storage.setNightMode(status)
            nightModeStatusSubj.onNext(status)
        }
        val nightMode: Int = if (status) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun handleSystemNightMode(uiMode: Int) {
        setNightModeStatus(if (storage.hasUserExplicitlySelectedAMode()) {
            storage.isNightModeEnabled()
        } else {
            uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        })
    }

    /**
     * Sets if user made an explicit action of enabling/disabling night mode.
     */
    fun setUserExplicitlySelectedAMode() {
        storage.setUserExplicitlySelectedAMode()
    }

    /**
     * Resets if user made a selection of "system setting" in night mode dropdown.
     */
    fun resetUserExplicitlySelectedAMode() {
        storage.resetUserExplicitlySelectedAMode()
    }

    /**
     * Returns true if user explicitly enabled/disabled night mode, false otherwise.
     */
    fun hasUserExplicitlySelectedAMode(): Boolean =
            storage.hasUserExplicitlySelectedAMode()

    /**
     * Sets if user's legacy version (prior to 4.45) has already been respected and need not be considered
     * again in the future.
     */
    fun setLegacyVersionSettingRespected() {
        storage.setLegacyVersionSettingRespected()
    }

    /**
     * Returns true if the legacy version (prior to 4.45) setting has been respected in newer implementation.
     * False otherwise.
     */
    fun hasLegacyVersionSettingRespected(): Boolean =
            storage.hasLegacyVersionSettingRespected()

    /**
     * Returns true if night mode enabled, false otherwise.
     */
    fun isNightModeEnabled(): Boolean = storage.isNightModeEnabled()


    fun getLightConditions(): Observable<LightConditions> {
        return lightConditions.asObservable()
                .doOnSubscribe {
                    val count = subscibersCounter.incrementAndGet()
                    if (count == 1) {
                        subscribeToLightUpdates()
                    }
                }
                .doOnUnsubscribe {
                    val count = subscibersCounter.decrementAndGet()
                    if (count == 0) {
                        unsubscribeFromLightUpdates()
                    }
                }
                .distinctUntilChanged()
    }

    private fun subscribeToLightUpdates() {
        lightSensor.environmentChangedListener = lightListener
        lightSensor.enable()
    }

    private fun unsubscribeFromLightUpdates() {
        lightSensor.environmentChangedListener = null
        lightSensor.disable()
    }
}