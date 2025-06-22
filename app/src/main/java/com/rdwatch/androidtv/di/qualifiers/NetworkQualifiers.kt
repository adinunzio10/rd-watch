package com.rdwatch.androidtv.di.qualifiers

import javax.inject.Qualifier

/**
 * Qualifier for the main API endpoint used for movie data
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainApi

/**
 * Qualifier for content delivery network (CDN) API endpoint for images and videos
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CdnApi

/**
 * Qualifier for analytics API endpoint for tracking user interactions
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnalyticsApi

/**
 * Qualifier for OkHttpClient with authentication interceptors
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/**
 * Qualifier for OkHttpClient without authentication (for public endpoints)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

/**
 * Qualifier for OkHttpClient with caching enabled for media content
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CachingClient

/**
 * Qualifier for Real-Debrid API endpoint
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealDebridApi