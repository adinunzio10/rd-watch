package com.rdwatch.androidtv.di.qualifiers

import javax.inject.Qualifier

/**
 * Qualifier for local-only repository implementation (offline mode)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalRepository

/**
 * Qualifier for remote-only repository implementation (online mode)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteRepository

/**
 * Qualifier for cache repository implementation for temporary data
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheRepository
