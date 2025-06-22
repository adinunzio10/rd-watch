package com.rdwatch.androidtv.di.qualifiers

import javax.inject.Qualifier

/**
 * Qualifier for the main application database
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDatabase

/**
 * Qualifier for cache database used for temporary storage
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDatabase

/**
 * Qualifier for user preferences database
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserDatabase

/**
 * Qualifier for downloaded content database
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadsDatabase