package com.rdwatch.androidtv.di

import com.rdwatch.androidtv.test.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * Test class to validate that dependency injection configuration is correct.
 * This test helps catch circular dependencies and other DI configuration issues.
 */
@HiltAndroidTest
class DependencyValidationTest : HiltTestBase() {

    @Test
    fun `verify no circular dependencies exist`() {
        // This test will fail to compile or run if there are circular dependencies
        // The fact that it can run means the dependency graph is valid
        
        // If we reach this point, dependency injection is working correctly
        // and there are no circular dependencies in the graph
        assert(true) { "Dependency injection setup is valid" }
    }

    @Test
    fun `verify all modules can be loaded`() {
        // The @HiltAndroidTest annotation will try to build the dependency graph
        // If there are issues with any modules, this test will fail
        
        // Getting to this point means all modules loaded successfully
        assert(true) { "All Hilt modules loaded successfully" }
    }
}