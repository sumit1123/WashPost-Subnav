package com.wapo.flagship.di.app.modules

import com.wapo.flagship.features.shared.EmbedJSInterfaceImpl
import com.wapo.view.web_embeds.EmbedJSInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WebViewModule {

    @Binds
    abstract fun bindEmbedJSInterfaceImpl(
        impl: EmbedJSInterfaceImpl
    ): EmbedJSInterface

}