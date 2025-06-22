package com.rdwatch.androidtv.di.scopes

import javax.inject.Qualifier
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class SessionScoped

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class FeatureScoped

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ViewModelScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RepositoryScope