package com.rdwatch.androidtv.test

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule

/**
 * Base class for Hilt unit tests.
 * Provides common setup and utilities for dependency injection testing.
 */
@HiltAndroidTest
abstract class HiltTestBase {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    open fun setUp() {
        hiltRule.inject()
    }
}