package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.test.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Test
import javax.inject.Inject

/**
 * Simple test to verify Hilt dependency injection is working.
 */
@HiltAndroidTest
class SimpleHiltTest : HiltTestBase() {

    @Inject
    lateinit var movieRepository: MovieRepository

    @Test
    fun `verify dependency injection works`() {
        // This test will fail if Hilt dependency injection is not working
        assertNotNull("MovieRepository should be injected", movieRepository)
    }

    @Test
    fun `verify test modules are loaded`() {
        // If we get here, it means the test modules loaded successfully
        // and there are no circular dependencies or other DI issues
        assertTrue("Test modules loaded successfully", true)
    }
}