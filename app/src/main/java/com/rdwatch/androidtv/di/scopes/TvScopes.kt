package com.rdwatch.androidtv.di.scopes

import javax.inject.Scope

/**
 * Custom scope for components that should live as long as a playback session
 * Useful for video player related dependencies
 */
@Scope
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackScoped

/**
 * Custom scope for components that should live as long as a browsing session
 * Useful for content browsing and discovery related dependencies
 */
@Scope
@Retention(AnnotationRetention.BINARY)
annotation class BrowseScoped

/**
 * Custom scope for components that should live as long as a user session
 * Useful for user preferences and authentication related dependencies
 */
@Scope
@Retention(AnnotationRetention.BINARY)
annotation class UserSessionScoped
