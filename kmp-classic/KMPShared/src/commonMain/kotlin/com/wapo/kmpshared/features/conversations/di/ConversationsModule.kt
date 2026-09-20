package com.wapo.kmpshared.features.conversations.di

import com.wapo.kmpshared.core.di.Qualifiers
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan(Qualifiers.CONVERSATIONS_PACKAGE)
class ConversationsModule
