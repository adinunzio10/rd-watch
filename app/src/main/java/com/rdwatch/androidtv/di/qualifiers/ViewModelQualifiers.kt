package com.rdwatch.androidtv.di.qualifiers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MainViewModel

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DetailViewModel

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SearchViewModel

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PlayerViewModel

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsViewModel