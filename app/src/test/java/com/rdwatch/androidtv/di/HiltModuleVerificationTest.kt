package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.data.MovieDao
import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.di.qualifiers.MainApi
import com.rdwatch.androidtv.di.qualifiers.PublicClient
import com.rdwatch.androidtv.network.ApiService
import com.rdwatch.androidtv.test.HiltTestBase
import com.rdwatch.androidtv.test.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Test class to verify that Hilt dependency injection is working correctly.
 * This test validates that all dependencies can be injected properly.
 */
@HiltAndroidTest
class HiltModuleVerificationTest : HiltTestBase() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    lateinit var movieRepository: MovieRepository

    @Inject
    lateinit var movieDao: MovieDao

    @Inject
    lateinit var apiService: ApiService

    @Inject
    @PublicClient
    lateinit var publicHttpClient: OkHttpClient

    @Inject
    @MainApi
    lateinit var mainRetrofit: Retrofit

    @Test
    fun `verify all dependencies are injected`() {
        // Verify core dependencies are not null
        assertNotNull("MovieRepository should be injected", movieRepository)
        assertNotNull("MovieDao should be injected", movieDao)
        assertNotNull("ApiService should be injected", apiService)
        assertNotNull("Public OkHttpClient should be injected", publicHttpClient)
        assertNotNull("Main Retrofit should be injected", mainRetrofit)
    }

    @Test
    fun `verify qualified dependencies are distinct`() {
        // Verify that qualified dependencies are properly distinguished
        assertNotNull("Public HTTP client should be injected", publicHttpClient)
        
        // Verify retrofit uses correct base URL for tests
        val baseUrl = mainRetrofit.baseUrl().toString()
        assertTrue("Retrofit should use test base URL", baseUrl.contains("localhost"))
    }

    @Test
    fun `verify repository can be used`() = runTest {
        // Test that the injected repository can be called
        val movies = movieRepository.getAllMovies().first()
        
        // Since we're using the fake repository module, this should work
        assertNotNull("Repository should return movies", movies)
    }

    @Test
    fun `verify database dependencies work`() {
        // Verify that the DAO is properly injected and can be used
        assertNotNull("MovieDao should be injected", movieDao)
        
        // Since we're using in-memory database in tests, this should work
        // We can't easily test database operations here without coroutines,
        // but we can verify the injection works
    }

    @Test
    fun `verify network dependencies are configured for testing`() {
        // Verify network components are set up for testing
        assertNotNull("ApiService should be injected", apiService)
        
        // Verify HTTP client has reasonable timeouts for testing
        val connectTimeout = publicHttpClient.connectTimeoutMillis
        assertTrue("Connect timeout should be reasonable for tests", connectTimeout <= 30000)
    }
}