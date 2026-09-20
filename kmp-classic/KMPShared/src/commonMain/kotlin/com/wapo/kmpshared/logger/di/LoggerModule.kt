package com.wapo.kmpshared.logger.di

import com.wapo.kmpshared.core.di.Qualifiers
import com.wapo.kmpshared.logger.domain.service.LogConsoleWriter
import com.wapo.kmpshared.logger.domain.service.logConsoleWriter
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan(Qualifiers.LOGGER_PACKAGE)
class LoggerModule {
    @Single
    fun provideConsoleWriter(): LogConsoleWriter = logConsoleWriter()
}
