package com.rdwatch.androidtv.di

// MediaUrlResolver now uses @Inject constructor - no manual provider needed
// This module is kept for potential future media-related dependencies

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    // MediaUrlResolver is now provided automatically via @Inject constructor
    // Additional media-related dependencies can be added here in the future
}
