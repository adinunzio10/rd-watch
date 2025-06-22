package com.rdwatch.androidtv.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {
    
    // ViewModels will be added here as we create them
    // Example:
    // @Binds
    // @ViewModelScoped
    // abstract fun bindMovieDetailViewModel(
    //     movieDetailViewModelImpl: MovieDetailViewModelImpl
    // ): MovieDetailViewModel
}