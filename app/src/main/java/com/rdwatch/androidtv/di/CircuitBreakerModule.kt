package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.network.CircuitBreaker
import com.rdwatch.androidtv.network.CircuitBreakerConfig
import com.rdwatch.androidtv.network.CircuitBreakerFactory
import com.rdwatch.androidtv.network.CircuitBreakerMonitor
import com.rdwatch.androidtv.network.CircuitBreakerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module for circuit breaker dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object CircuitBreakerModule {
    @Provides
    @Singleton
    fun provideCircuitBreakerRegistry(): CircuitBreakerRegistry {
        return CircuitBreakerRegistry()
    }

    @Provides
    @Singleton
    fun provideCircuitBreakerMonitor(): CircuitBreakerMonitor {
        return CircuitBreakerMonitor()
    }

    @Provides
    @Singleton
    @Named("real-debrid")
    fun provideRealDebridCircuitBreaker(): CircuitBreaker {
        return CircuitBreakerFactory.createRealDebridCircuitBreaker()
    }

    @Provides
    @Singleton
    @Named("scraper-default")
    fun provideDefaultScraperCircuitBreaker(): CircuitBreaker {
        return CircuitBreakerFactory.createScraperCircuitBreaker("default")
    }

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTMDbCircuitBreaker(): CircuitBreaker {
        return CircuitBreaker(
            serviceName = "tmdb-api",
            failureThreshold = CircuitBreakerConfig.METADATA_DEFAULT.failureThreshold,
            recoveryTimeout = CircuitBreakerConfig.METADATA_DEFAULT.recoveryTimeout,
            fallbackFunction = {
                android.util.Log.d("CircuitBreaker", "Using fallback for TMDb API")
                "{}" // Empty JSON object as fallback for TMDb responses
            },
        )
    }

    // Configuration providers
    @Provides
    @Named("scraper-config")
    fun provideScraperCircuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.SCRAPER_DEFAULT
    }

    @Provides
    @Named("real-debrid-config")
    fun provideRealDebridCircuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.REAL_DEBRID_DEFAULT
    }

    @Provides
    @Named("tmdb-config")
    fun provideTMDbCircuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig.METADATA_DEFAULT
    }
}
