package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.test.fake.FakeMovieRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module that provides fake repository implementations.
 * Use this when you want to test with fake data instead of mocks.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class TestFakeRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(
        fakeMovieRepository: FakeMovieRepository
    ): MovieRepository
}