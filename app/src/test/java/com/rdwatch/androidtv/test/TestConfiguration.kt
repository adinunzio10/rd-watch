package com.rdwatch.androidtv.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Test rule that sets up coroutine dispatchers for testing.
 * Use this rule in tests that use coroutines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

/**
 * Utility object for common test configurations and constants.
 */
object TestConfiguration {
    
    /**
     * Base URL for test API endpoints
     */
    const val TEST_BASE_URL = "http://localhost:8080/"
    
    /**
     * Test timeout for coroutines in milliseconds
     */
    const val TEST_TIMEOUT_MS = 5000L
    
    /**
     * Default test data size
     */
    const val DEFAULT_TEST_DATA_SIZE = 10
}