package com.wapo.flagship.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.wapo.flagship.auto.ask.AskErrorAudioProvider
import com.wapo.flagship.auto.ask.AskFeaturedQuestionsGateway
import com.wapo.flagship.auto.ask.AskSpeechRecognizer
import com.wapo.flagship.auto.ask.AskThePostGateway
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class WapoCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        CarHostValidatorFactory.create(applicationContext)

    override fun onCreateSession(): Session {
        val dependencies =
            EntryPointAccessors.fromApplication(
                applicationContext,
                AndroidAutoDependencies::class.java,
            )
        return AutoCarSession(
            askThePostGateway = dependencies.askThePostGateway(),
            featuredQuestionsGateway = dependencies.featuredQuestionsGateway(),
            speechRecognizer = dependencies.askSpeechRecognizer(),
            errorAudioProvider = dependencies.askErrorAudioProvider(),
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AndroidAutoDependencies {
    fun askThePostGateway(): AskThePostGateway

    fun featuredQuestionsGateway(): AskFeaturedQuestionsGateway

    fun askSpeechRecognizer(): AskSpeechRecognizer

    fun askErrorAudioProvider(): AskErrorAudioProvider
}

private class AutoCarSession(
    private val askThePostGateway: AskThePostGateway,
    private val featuredQuestionsGateway: AskFeaturedQuestionsGateway,
    private val speechRecognizer: AskSpeechRecognizer,
    private val errorAudioProvider: AskErrorAudioProvider,
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen =
        WapoCarHomeScreen(
            carContext = carContext,
            askThePostGateway = askThePostGateway,
            featuredQuestionsGateway = featuredQuestionsGateway,
            speechRecognizer = speechRecognizer,
            errorAudioProvider = errorAudioProvider,
        )
}
