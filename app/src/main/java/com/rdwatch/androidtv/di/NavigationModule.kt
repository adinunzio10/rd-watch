package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.presentation.navigation.DeepLinkHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {
    @Provides
    @Singleton
    fun provideDeepLinkHandler(): DeepLinkHandler = DeepLinkHandler()
}
