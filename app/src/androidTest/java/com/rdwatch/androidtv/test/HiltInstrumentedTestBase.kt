package com.rdwatch.androidtv.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for Hilt instrumented (UI) tests.
 * Provides common setup and utilities for dependency injection in Android tests.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
abstract class HiltInstrumentedTestBase {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    open fun setUp() {
        hiltRule.inject()
    }
}