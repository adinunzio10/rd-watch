package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.test.fake.FakeFileBrowserRepository
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module for FileBrowser dependencies.
 * Replaces the real FileBrowserRepository with a fake implementation for testing.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [/* Add actual FileBrowserModule here */]
)
abstract class TestFileBrowserModule {

    @Binds
    @Singleton
    abstract fun bindFileBrowserRepository(
        fakeFileBrowserRepository: FakeFileBrowserRepository
    ): FileBrowserRepository
}