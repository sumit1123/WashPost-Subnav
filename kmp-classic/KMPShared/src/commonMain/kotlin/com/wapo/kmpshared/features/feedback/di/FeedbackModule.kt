// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.features.feedback.di

import com.wapo.kmpshared.core.di.Qualifiers
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan(Qualifiers.FEEDBACK_PACKAGE)
class FeedbackModule
